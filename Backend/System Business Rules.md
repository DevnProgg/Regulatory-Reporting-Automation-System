# Business Rules for Regulatory Reporting Automation

These are non-negotiable banking rules regulators expect.

## 2.1 Basel III Capital Adequacy Rules

### Capital Classification

- **CET1**: Paid-up capital, retained earnings, disclosed reserves
- **AT1**: Perpetual instruments
- **Tier 2**: Subordinated debt

### Eligibility Rules

**Exclude:**
- Deferred tax assets (beyond threshold)
- Intangible assets

**Requirements:**
- Capital must be fully paid and loss-absorbing

### Capital Ratio Rules

**Minimum CAR:**
- CET1 ≥ 4.5%
- Tier 1 ≥ 6%
- Total Capital ≥ 8%

**Additional Buffers:**
- Capital Conservation Buffer (2.5%)
- Possible CBL-specific buffers

## 2.2 Risk-Weighted Assets (RWA) Rules

**On-balance exposures weighted by:**
- Counterparty type
- Credit quality
- Collateral

**Off-balance exposures:**
- Converted using CCF (Credit Conversion Factor)

**Defaulted exposures:**
- Risk weight = 100%–150%

## 2.3 Non-Performing Loan (NPL) Classification Rules

Basel-aligned + African central bank practice:

| Days Past Due | Classification |
|---------------|----------------|
| < 30          | Performing     |
| 30–89         | Watchlist      |
| ≥ 90          | NPL            |

### Additional NPL Triggers

- Bankruptcy
- Restructured loan with distress
- Interest capitalization due to inability to pay

## 2.4 Liquidity Rules (Basel III – LCR)

### Stress Factors

- **Retail deposits**: 5–10% outflow
- **Corporate deposits**: 20–40% outflow

### High-Quality Liquid Assets (HQLA)

- **Level 1**: Cash, Central Bank reserves
- **Level 2**: Government bonds (with haircut)

## 2.5 Reporting Governance Rules (CBL-Style)

**Reports must be:**
- Snapshot-based
- Reproducible
- Fully auditable

**Data Management:**
- Historical data must never be overwritten
- All calculations traceable to source data

## 3. Reporting Formulas & Required Input Data

### 3.1 Capital Adequacy Ratio (CAR)

**Formula:**

```
CAR = Total Eligible Capital / Total Risk-Weighted Assets
```

**Inputs:**
- `capital_components.amount`
- `loan_exposures.risk_weight`
- `off_balance_sheet_exposures.credit_conversion_factor`

### 3.2 Risk-Weighted Assets (RWA)

**On-Balance:**

```
RWA = Exposure Amount × Risk Weight
RWA = outstanding_balance × risk_weight
```

**Off-Balance:**

```
RWA = Notional × CCF × Risk Weight
```

### 3.3 CET1 Ratio

**Formula:**

```
CET1 Ratio = CET1 Capital / Total RWA
```

### 3.4 Liquidity Coverage Ratio (LCR)

**Formula:**

```
LCR = High Quality Liquid Assets / Net Cash Outflows (30 days)
```

**Net Cash Outflows:**

```
Outflows – min(Inflows, 75% of Outflows)
```

**Inputs:**
- `liquidity_cashflows.amount`
- `liquidity_cashflows.stress_factor`

### 3.5 NPL Ratio

**Formula:**

```
NPL Ratio = Total NPL Outstanding / Total Loan Portfolio
```

**Inputs:**
- `loan_exposures.npl_status`
- `loan_exposures.outstanding_balance`

### 3.6 Expected Credit Loss (Optional – IFRS 9 Ready)

**Formula:**

```
ECL = PD × LGD × EAD
```

**Inputs:**
- `probability_of_default` (PD)
- `loss_given_default` (LGD)
- `exposure_at_default` (EAD)

## 4. How This Scales in a Real Bank

What you're building is exactly how Tier-1 banks do it:

- Snapshot-driven
- Calculation engines isolated
- Immutable reporting periods
- Regulator-aligned ratios