CREATE TABLE banks (
                       bank_id UUID PRIMARY KEY,
                       bank_code VARCHAR(20) UNIQUE NOT NULL,
                       bank_name TEXT NOT NULL,
                       license_number TEXT NOT NULL,
                       reporting_currency CHAR(3) DEFAULT 'LSL',
                       created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE customers (
                           customer_id UUID PRIMARY KEY,
                           customer_type VARCHAR(20) CHECK (customer_type IN ('INDIVIDUAL','CORPORATE')),
                           sector_code VARCHAR(20), -- Basel sector classification
                           country_code CHAR(2),
                           created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE accounts (
                          account_id UUID PRIMARY KEY,
                          bank_id UUID REFERENCES banks(bank_id),
                          customer_id UUID REFERENCES customers(customer_id),
                          account_type VARCHAR(30), -- LOAN, DEPOSIT, NOSTRO, DERIVATIVE
                          currency CHAR(3),
                          opened_at DATE,
                          maturity_date DATE,
                          status VARCHAR(20)
);
CREATE TABLE loan_exposures (
                                loan_id UUID PRIMARY KEY,
                                account_id UUID REFERENCES accounts(account_id),
                                outstanding_balance NUMERIC(18,2),
                                accrued_interest NUMERIC(18,2),
                                days_past_due INT,
                                collateral_value NUMERIC(18,2),
                                collateral_type VARCHAR(30),
                                probability_of_default NUMERIC(6,4),
                                loss_given_default NUMERIC(6,4),
                                exposure_at_default NUMERIC(18,2),
                                risk_weight NUMERIC(6,4),
                                npl_status BOOLEAN,
                                snapshot_date DATE NOT NULL
);
CREATE TABLE off_balance_sheet_exposures (
                                             obs_id UUID PRIMARY KEY,
                                             bank_id UUID REFERENCES banks(bank_id),
                                             exposure_type VARCHAR(50), -- GUARANTEE, LC, COMMITMENT
                                             notional_amount NUMERIC(18,2),
                                             credit_conversion_factor NUMERIC(6,4),
                                             counterparty_sector VARCHAR(30),
                                             snapshot_date DATE NOT NULL
);
CREATE TABLE capital_components (
                                    capital_id UUID PRIMARY KEY,
                                    bank_id UUID REFERENCES banks(bank_id),
                                    capital_type VARCHAR(20), -- CET1, AT1, T2
                                    amount NUMERIC(18,2),
                                    eligible BOOLEAN,
                                    snapshot_date DATE NOT NULL
);
CREATE TABLE liquidity_cashflows (
                                     cashflow_id UUID PRIMARY KEY,
                                     bank_id UUID REFERENCES banks(bank_id),
                                     flow_type VARCHAR(10) CHECK (flow_type IN ('INFLOW','OUTFLOW')),
                                     amount NUMERIC(18,2),
                                     currency CHAR(3),
                                     maturity_bucket VARCHAR(20), -- 0-7d, 8-30d, etc
                                     stress_factor NUMERIC(6,4),
                                     snapshot_date DATE NOT NULL
);
CREATE TABLE reporting_snapshots (
                                     snapshot_id UUID PRIMARY KEY,
                                     bank_id UUID REFERENCES banks(bank_id),
                                     reporting_period DATE,
                                     report_type VARCHAR(50), -- CAR, LCR, NPL
                                     generated_at TIMESTAMP DEFAULT NOW(),
                                     status VARCHAR(20)
);
CREATE TABLE reporting_snapshots (
                                     snapshot_id UUID PRIMARY KEY,
                                     bank_id UUID REFERENCES banks(bank_id),
                                     reporting_period DATE,
                                     report_type VARCHAR(50), -- CAR, LCR, NPL
                                     generated_at TIMESTAMP DEFAULT NOW(),
                                     status VARCHAR(20)
);
