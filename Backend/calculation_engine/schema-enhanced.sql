-- Enhanced Schema for Basel III Regulatory Calculations
-- Includes Lesotho Central Bank overrides

-- ============================================================================
-- CORE BANKING SYSTEM (CBS) SCHEMA - ENHANCED
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS cbs;

-- Customer categories
CREATE TYPE cbs.customer_category AS ENUM ('RETAIL', 'SME', 'CORP', 'SOVEREIGN', 'BANK');

-- Account categories
CREATE TYPE cbs.account_category AS ENUM ('SAVINGS', 'CURRENT', 'LOAN', 'INVESTMENT');

-- Transaction direction
CREATE TYPE cbs.txn_direction AS ENUM ('DEBIT', 'CREDIT');

-- Asset classification for NPL (as per Lesotho CBL)
CREATE TYPE cbs.asset_classification AS ENUM (
    'STANDARD',      -- 0-30 days past due
    'WATCH',         -- 31-60 days past due
    'SUBSTANDARD',   -- 61-90 days past due
    'DOUBTFUL',      -- 91-180 days past due
    'LOSS'           -- 180+ days past due
);

-- ============================================================================
-- CUSTOMERS TABLE - Enhanced with Basel III fields
-- ============================================================================
CREATE TABLE cbs.customers (
    customer_id         SERIAL PRIMARY KEY,
    customer_type       cbs.customer_category NOT NULL,
    country             VARCHAR(100) NOT NULL,
    country_risk_rating INTEGER CHECK (country_risk_rating >= 1 AND country_risk_rating <= 10),
    internal_rating     VARCHAR(10),  -- Internal credit rating (e.g., 'AAA', 'BB+')
    external_rating     VARCHAR(10),  -- External credit rating (S&P, Moody's, etc.)
    pd_value            NUMERIC(5, 4), -- Probability of Default (0.0000 to 1.0000)
    lgd_value           NUMERIC(5, 4), -- Loss Given Default (0.0000 to 1.0000)
    is_financial_inst   BOOLEAN DEFAULT FALSE,
    is_public_sector    BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- ACCOUNTS TABLE - Enhanced
-- ============================================================================
CREATE TABLE cbs.accounts (
    account_id          SERIAL PRIMARY KEY,
    customer_id         INTEGER NOT NULL,
    account_type        cbs.account_category NOT NULL,
    currency            CHAR(3) NOT NULL,
    balance             NUMERIC(15, 2) DEFAULT 0,
    available_balance   NUMERIC(15, 2) DEFAULT 0,
    opened_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    is_off_balance_sheet BOOLEAN DEFAULT FALSE, -- For contingent liabilities
    
    CONSTRAINT fk_customer
        FOREIGN KEY(customer_id)
        REFERENCES cbs.customers(customer_id)
        ON DELETE CASCADE
);

-- ============================================================================
-- LOANS TABLE - Enhanced for RWA and ECL calculations
-- ============================================================================
CREATE TABLE cbs.loans (
    loan_id             SERIAL PRIMARY KEY,
    account_id          INTEGER NOT NULL UNIQUE,
    principal_amount    NUMERIC(15, 2) NOT NULL,
    outstanding_balance NUMERIC(15, 2) NOT NULL,
    interest_rate       NUMERIC(5, 4) NOT NULL,
    origination_date    DATE NOT NULL,
    maturity_date       DATE NOT NULL,
    collateral_value    NUMERIC(15, 2),
    collateral_type     VARCHAR(50),  -- 'PROPERTY', 'VEHICLE', 'CASH', 'SECURITIES', etc.
    product_type        VARCHAR(50),  -- 'MORTGAGE', 'AUTO', 'PERSONAL', 'CORPORATE'
    loan_purpose        VARCHAR(100), -- 'RESIDENTIAL', 'COMMERCIAL', 'WORKING_CAPITAL'
    is_restructured     BOOLEAN DEFAULT FALSE,
    restructure_date    DATE,
    asset_class         cbs.asset_classification DEFAULT 'STANDARD',
    stage               INTEGER CHECK (stage IN (1, 2, 3)), -- IFRS 9 staging
    original_term_months INTEGER,
    remaining_term_months INTEGER,
    
    CONSTRAINT fk_account
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
        ON DELETE RESTRICT
);

-- ============================================================================
-- LOAN PERFORMANCE - Enhanced
-- ============================================================================
CREATE TABLE cbs.loan_performance (
    loan_id             INTEGER PRIMARY KEY,
    days_past_due       INTEGER DEFAULT 0 CHECK (days_past_due >= 0),
    last_payment_date   DATE,
    last_payment_amount NUMERIC(15, 2),
    times_past_due_30   INTEGER DEFAULT 0, -- Count of 30+ DPD in last 12 months
    times_past_due_60   INTEGER DEFAULT 0,
    times_past_due_90   INTEGER DEFAULT 0,
    is_forborne         BOOLEAN DEFAULT FALSE,
    forbearance_date    DATE,
    
    CONSTRAINT fk_loan_perf
        FOREIGN KEY(loan_id)
        REFERENCES cbs.loans(loan_id)
        ON DELETE CASCADE
);

-- ============================================================================
-- TRANSACTIONS TABLE
-- ============================================================================
CREATE TABLE cbs.transactions (
    txn_id          BIGSERIAL PRIMARY KEY,
    account_id      INTEGER NOT NULL,
    txn_date        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    direction       cbs.txn_direction NOT NULL,
    balance_after   NUMERIC(15, 2) NOT NULL,
    txn_type        VARCHAR(50),
    
    CONSTRAINT fk_account_txn
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
);

CREATE INDEX idx_txn_account_date ON cbs.transactions(account_id, txn_date);

-- ============================================================================
-- CAPITAL TABLE - For CAR calculations
-- ============================================================================
CREATE TABLE cbs.capital_components (
    capital_id          SERIAL PRIMARY KEY,
    as_of_date          DATE NOT NULL,
    component_type      VARCHAR(50) NOT NULL, -- 'CET1', 'AT1', 'T2'
    component_name      VARCHAR(100) NOT NULL,
    amount              NUMERIC(15, 2) NOT NULL,
    currency            CHAR(3) NOT NULL,
    regulatory_adjustment NUMERIC(15, 2) DEFAULT 0,
    description         TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_capital_component UNIQUE (as_of_date, component_type, component_name)
);

-- ============================================================================
-- LIQUIDITY ASSETS - For LCR calculations
-- ============================================================================
CREATE TABLE cbs.liquidity_assets (
    asset_id            SERIAL PRIMARY KEY,
    asset_type          VARCHAR(50) NOT NULL, -- 'CASH', 'CENTRAL_BANK_RESERVES', 'GOVT_SECURITIES'
    currency            CHAR(3) NOT NULL,
    market_value        NUMERIC(15, 2) NOT NULL,
    haircut_percentage  NUMERIC(5, 4) DEFAULT 0, -- Haircut applied to asset value
    maturity_date       DATE,
    is_unencumbered     BOOLEAN DEFAULT TRUE,
    hqla_level          INTEGER CHECK (hqla_level IN (1, 2)), -- Level 1 or Level 2 HQLA
    as_of_date          DATE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- CASH FLOWS - For LCR calculations
-- ============================================================================
CREATE TABLE cbs.cash_flows (
    flow_id             SERIAL PRIMARY KEY,
    flow_type           VARCHAR(50) NOT NULL, -- 'INFLOW', 'OUTFLOW'
    flow_category       VARCHAR(100) NOT NULL, -- 'RETAIL_DEPOSIT_STABLE', 'WHOLESALE_UNSECURED', etc.
    currency            CHAR(3) NOT NULL,
    contractual_amount  NUMERIC(15, 2) NOT NULL,
    expected_date       DATE NOT NULL,
    run_off_rate        NUMERIC(5, 4), -- For outflows
    inflow_rate         NUMERIC(5, 4), -- For inflows
    account_id          INTEGER,
    as_of_date          DATE NOT NULL,
    
    CONSTRAINT fk_cashflow_account
        FOREIGN KEY(account_id)
        REFERENCES cbs.accounts(account_id)
);

CREATE INDEX idx_cashflows_date ON cbs.cash_flows(as_of_date, expected_date);

-- ============================================================================
-- SOURCE_READ SCHEMA - Read-only views
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS source_read;

CREATE OR REPLACE VIEW source_read.loan_exposures AS
SELECT
    l.loan_id,
    a.account_id,
    c.customer_id,
    c.customer_type,
    c.country,
    c.country_risk_rating,
    c.internal_rating,
    c.external_rating,
    c.pd_value,
    c.lgd_value,
    c.is_financial_inst,
    c.is_public_sector,
    l.principal_amount,
    l.outstanding_balance,
    l.interest_rate,
    l.collateral_value,
    l.collateral_type,
    l.product_type,
    l.loan_purpose,
    l.is_restructured,
    l.asset_class,
    l.stage,
    l.origination_date,
    l.maturity_date,
    l.original_term_months,
    l.remaining_term_months,
    CASE
        WHEN l.collateral_value > 0 THEN (l.outstanding_balance / l.collateral_value)
        ELSE NULL
    END AS ltv_ratio,
    lp.days_past_due,
    lp.times_past_due_30,
    lp.times_past_due_60,
    lp.times_past_due_90,
    lp.is_forborne,
    a.currency
FROM cbs.loans l
INNER JOIN cbs.accounts a ON l.account_id = a.account_id
INNER JOIN cbs.customers c ON a.customer_id = c.customer_id
LEFT JOIN cbs.loan_performance lp ON lp.loan_id = l.loan_id;

CREATE OR REPLACE VIEW source_read.capital_base AS
SELECT
    as_of_date,
    component_type,
    component_name,
    amount,
    currency,
    regulatory_adjustment,
    (amount + COALESCE(regulatory_adjustment, 0)) AS adjusted_amount
FROM cbs.capital_components
ORDER BY as_of_date DESC, component_type;

CREATE OR REPLACE VIEW source_read.liquidity_positions AS
SELECT
    asset_id,
    asset_type,
    currency,
    market_value,
    haircut_percentage,
    (market_value * (1 - COALESCE(haircut_percentage, 0))) AS hqla_value,
    maturity_date,
    is_unencumbered,
    hqla_level,
    as_of_date
FROM cbs.liquidity_assets
WHERE is_unencumbered = TRUE;

-- ============================================================================
-- SNAPSHOTS SCHEMA - Time-frozen copies
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS snapshots;

CREATE TYPE snapshots.run_status AS ENUM ('DRAFT', 'VALIDATED', 'CALCULATED', 'APPROVED', 'FAILED');
CREATE TYPE snapshots.calculation_type AS ENUM ('BI_WEEKLY', 'MONTHLY', 'ANNUAL');

CREATE TABLE snapshots.snapshot_runs (
    snapshot_id         SERIAL PRIMARY KEY,
    snapshot_date       DATE NOT NULL,
    calculation_type    snapshots.calculation_type NOT NULL,
    status              snapshots.run_status DEFAULT 'DRAFT',
    initiated_by        VARCHAR(100),
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    validated_at        TIMESTAMP WITH TIME ZONE,
    calculated_at       TIMESTAMP WITH TIME ZONE,
    approved_at         TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT unique_snapshot_date_type UNIQUE (snapshot_date, calculation_type)
);

CREATE TABLE snapshots.loan_exposures_snapshot (
    snapshot_id         INTEGER NOT NULL,
    loan_id             INTEGER NOT NULL,
    customer_id         INTEGER NOT NULL,
    customer_type       cbs.customer_category,
    country             VARCHAR(100),
    country_risk_rating INTEGER,
    internal_rating     VARCHAR(10),
    pd_value            NUMERIC(5, 4),
    lgd_value           NUMERIC(5, 4),
    is_financial_inst   BOOLEAN,
    is_public_sector    BOOLEAN,
    principal_amount    NUMERIC(15, 2),
    outstanding_balance NUMERIC(15, 2),
    collateral_value    NUMERIC(15, 2),
    collateral_type     VARCHAR(50),
    product_type        VARCHAR(50),
    loan_purpose        VARCHAR(100),
    ltv_ratio           NUMERIC(5, 4),
    days_past_due       INTEGER,
    asset_class         cbs.asset_classification,
    stage               INTEGER,
    is_restructured     BOOLEAN,
    is_forborne         BOOLEAN,
    maturity_date       DATE,
    remaining_term_months INTEGER,
    currency            CHAR(3),
    
    PRIMARY KEY (snapshot_id, loan_id),
    
    CONSTRAINT fk_snapshot_run
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

CREATE TABLE snapshots.capital_snapshot (
    snapshot_id         INTEGER NOT NULL,
    component_type      VARCHAR(50) NOT NULL,
    component_name      VARCHAR(100) NOT NULL,
    amount              NUMERIC(15, 2) NOT NULL,
    currency            CHAR(3) NOT NULL,
    
    PRIMARY KEY (snapshot_id, component_type, component_name),
    
    CONSTRAINT fk_capital_snapshot_run
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

CREATE TABLE snapshots.liquidity_snapshot (
    snapshot_id         INTEGER NOT NULL,
    asset_id            INTEGER NOT NULL,
    asset_type          VARCHAR(50) NOT NULL,
    hqla_value          NUMERIC(15, 2) NOT NULL,
    hqla_level          INTEGER,
    currency            CHAR(3) NOT NULL,
    
    PRIMARY KEY (snapshot_id, asset_id),
    
    CONSTRAINT fk_liquidity_snapshot_run
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_snapshot_country ON snapshots.loan_exposures_snapshot(country);
CREATE INDEX idx_snapshot_dpd ON snapshots.loan_exposures_snapshot(days_past_due);
CREATE INDEX idx_snapshot_classification ON snapshots.loan_exposures_snapshot(asset_class);

-- ============================================================================
-- METRICS SCHEMA - Final calculations
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS metrics;

CREATE TABLE metrics.regulatory_metrics (
    metric_id           SERIAL PRIMARY KEY,
    snapshot_id         INTEGER NOT NULL,
    metric_code         VARCHAR(50) NOT NULL,
    value               NUMERIC(20, 4) NOT NULL,
    unit                VARCHAR(20),
    metadata            JSONB, -- Store calculation details for auditability
    calculated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_metric_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

CREATE TABLE metrics.metric_components (
    snapshot_id         INTEGER NOT NULL,
    loan_id             INTEGER NOT NULL,
    exposure_amount     NUMERIC(15, 2) NOT NULL,
    risk_weight         NUMERIC(5, 4) NOT NULL,
    rwa_value           NUMERIC(15, 2) NOT NULL,
    ecl_amount          NUMERIC(15, 2),
    ecl_stage           INTEGER,
    provision_amount    NUMERIC(15, 2),
    
    PRIMARY KEY (snapshot_id, loan_id),
    
    CONSTRAINT fk_comp_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id),
    
    CONSTRAINT fk_comp_loan_snap
        FOREIGN KEY (snapshot_id, loan_id)
        REFERENCES snapshots.loan_exposures_snapshot(snapshot_id, loan_id)
);

CREATE INDEX idx_metric_history ON metrics.regulatory_metrics(metric_code, snapshot_id);

-- ============================================================================
-- AUDIT TRAIL - For traceability
-- ============================================================================
CREATE TABLE metrics.calculation_audit (
    audit_id            BIGSERIAL PRIMARY KEY,
    snapshot_id         INTEGER NOT NULL,
    calculation_step    VARCHAR(100) NOT NULL,
    input_data          JSONB,
    output_data         JSONB,
    calculation_rule    TEXT,
    executed_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms   INTEGER,
    
    CONSTRAINT fk_audit_snapshot
        FOREIGN KEY (snapshot_id)
        REFERENCES snapshots.snapshot_runs(snapshot_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_audit_snapshot ON metrics.calculation_audit(snapshot_id, calculation_step);
