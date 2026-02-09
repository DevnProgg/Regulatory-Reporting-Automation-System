-- Create the schema
CREATE SCHEMA IF NOT EXISTS cbs;

-- Create a custom ENUM for customer types
CREATE TYPE cbs.customer_category AS ENUM ('RETAIL', 'SME', 'CORP');

CREATE TABLE cbs.customers (
    customer_id   SERIAL PRIMARY KEY,
    customer_type cbs.customer_category NOT NULL,
    country       VARCHAR(100) NOT NULL,
    risk_rating   INTEGER CHECK (risk_rating >= 1 AND risk_rating <= 10),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create a custom ENUM for account types
CREATE TYPE cbs.account_category AS ENUM ('SAVINGS', 'CURRENT', 'LOAN');

CREATE TABLE cbs.accounts (
    account_id   SERIAL PRIMARY KEY,
    customer_id  INTEGER NOT NULL,
    account_type cbs.account_category NOT NULL,
    currency     CHAR(3) NOT NULL, -- e.g., 'USD', 'EUR', 'GBP'
    opened_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(20) DEFAULT 'ACTIVE',

    -- Relationship: Links account to a specific customer
    CONSTRAINT fk_customer
        FOREIGN KEY(customer_id)
        REFERENCES cbs.customers(customer_id)
        ON DELETE CASCADE
);

CREATE TABLE cbs.loans (
    loan_id          SERIAL PRIMARY KEY,
    account_id       INTEGER NOT NULL UNIQUE, -- Assuming 1 loan per loan account
    principal_amount DECIMAL(18, 2) NOT NULL,
    interest_rate    DECIMAL(5, 2) NOT NULL,  -- e.g., 12.50
    maturity_date    DATE NOT NULL,
    collateral_value DECIMAL(18, 2),
    product_type     VARCHAR(50),             -- e.g., 'MORTGAGE', 'AUTO', 'PERSONAL'

    -- Relationship: Links loan to a specific account
    CONSTRAINT fk_account
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
        ON DELETE RESTRICT
);

CREATE TABLE cbs.loans (
    loan_id          SERIAL PRIMARY KEY,
    account_id       INTEGER NOT NULL UNIQUE,
    principal_amount NUMERIC(15, 2) NOT NULL,
    interest_rate    NUMERIC(5, 4) NOT NULL,  -- e.g., 0.0525 for 5.25%
    maturity_date    DATE NOT NULL,
    collateral_value NUMERIC(15, 2),
    product_type     VARCHAR(50),             -- e.g., 'MORTGAGE', 'PERSONAL'

    -- Relationship: Links loan to a specific account
    CONSTRAINT fk_account
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
        ON DELETE RESTRICT
);

-- Create a custom ENUM for transaction direction
CREATE TYPE cbs.txn_direction AS ENUM ('DEBIT', 'CREDIT');

CREATE TABLE cbs.transactions (
    txn_id        BIGSERIAL PRIMARY KEY,
    account_id    INTEGER NOT NULL,
    txn_date      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    amount        NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    direction     cbs.txn_direction NOT NULL,
    balance_after NUMERIC(15, 2) NOT NULL,

    -- Relationship: Links transaction to a specific account
    CONSTRAINT fk_account_txn
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
);

-- Indexing for performance
CREATE INDEX idx_txn_account_date ON cbs.transactions(account_id, txn_date);

CREATE TABLE cbs.loan_performance (
    loan_id           INTEGER PRIMARY KEY,
    days_past_due     INTEGER DEFAULT 0 CHECK (days_past_due >= 0),
    last_payment_date DATE,

    -- Relationship: Links performance directly to the loan contract
    CONSTRAINT fk_loan_perf
        FOREIGN KEY(loan_id)
        REFERENCES cbs.loans(loan_id)
        ON DELETE CASCADE
);

-- Ensure the reporting schema exists
CREATE SCHEMA IF NOT EXISTS source_read;

-- Create the view
CREATE OR REPLACE VIEW source_read.loan_exposures AS
SELECT
    l.loan_id,
    a.account_id,
    c.customer_id,        -- Added for easier grouping
    c.customer_type,
    l.principal_amount,
    l.interest_rate,     -- Added to calculate yield/revenue
    l.collateral_value,
    -- Calculate Loan-to-Value (LTV) ratio
    CASE
        WHEN l.collateral_value > 0 THEN (l.principal_amount / l.collateral_value)
        ELSE NULL
    END AS ltv_ratio,
    lp.days_past_due,
    c.country
FROM cbs.loans l
INNER JOIN cbs.accounts a ON l.account_id = a.account_id
INNER JOIN cbs.customers c ON a.customer_id = c.customer_id
LEFT JOIN cbs.loan_performance lp ON lp.loan_id = l.loan_id;

-- Create the schema for snapshots
CREATE SCHEMA IF NOT EXISTS snapshots;

-- Create the status workflow ENUM
CREATE TYPE snapshots.run_status AS ENUM ('DRAFT', 'VALIDATED', 'CALCULATED', 'APPROVED');

CREATE TABLE snapshots.snapshot_runs (
    snapshot_id   SERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    status        snapshots.run_status DEFAULT 'DRAFT',
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraint: Prevent duplicate runs for the same date unless intended
    CONSTRAINT unique_snapshot_date UNIQUE (snapshot_date)
);

CREATE TABLE snapshots.loan_exposures_snapshot (
    snapshot_id      INTEGER NOT NULL,
    loan_id          INTEGER NOT NULL,
    customer_type    cbs.customer_category, -- Reusing your ENUM
    principal_amount NUMERIC(15, 2),
    collateral_value NUMERIC(15, 2),
    days_past_due    INTEGER,
    country          VARCHAR(100),

    -- Composite Primary Key: One entry per loan, per snapshot run
    PRIMARY KEY (snapshot_id, loan_id),

    -- Relationship: Links to the control table
    CONSTRAINT fk_snapshot_run
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

-- Indexing for reporting performance
CREATE INDEX idx_snapshot_country ON snapshots.loan_exposures_snapshot(country);
CREATE INDEX idx_snapshot_dpd ON snapshots.loan_exposures_snapshot(days_past_due);

-- Create the schema for final metrics
CREATE SCHEMA IF NOT EXISTS metrics;

CREATE TABLE metrics.regulatory_metrics (
    metric_id     SERIAL PRIMARY KEY,
    snapshot_id   INTEGER NOT NULL,
    metric_code   VARCHAR(50) NOT NULL, -- e.g., 'NPL_RATIO', 'TOTAL_EXPOSURE_USD'
    value         NUMERIC(20, 4) NOT NULL,
    unit          VARCHAR(20),          -- e.g., 'PERCENTAGE', 'CURRENCY', 'COUNT'
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Relationship: Every metric must belong to a specific snapshot run
    CONSTRAINT fk_metric_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

-- Index for quick lookup of specific KPIs over time
CREATE INDEX idx_metric_history ON metrics.regulatory_metrics(metric_code, snapshot_id);

CREATE TABLE metrics.metric_components (
    snapshot_id   INTEGER NOT NULL,
    loan_id       INTEGER NOT NULL,
    rwa_value     NUMERIC(15, 2) NOT NULL,
    risk_weight   NUMERIC(5, 4) NOT NULL, -- e.g., 0.7500 for 75% risk weight

    -- Composite Primary Key ensures one calculation per loan per snapshot
    PRIMARY KEY (snapshot_id, loan_id),

    -- Relationship: Links to the snapshot run
    CONSTRAINT fk_comp_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id),

    -- Relationship: Links to the specific loan record in that snapshot
    CONSTRAINT fk_comp_loan_snap
        FOREIGN KEY (snapshot_id, loan_id)
        REFERENCES snapshots.loan_exposures_snapshot(snapshot_id, loan_id)
);

