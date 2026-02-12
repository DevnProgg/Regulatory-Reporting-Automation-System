# Regulatory Calculation Engine Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Basel III Calculations](#basel-iii-calculations)
4. [Lesotho Central Bank Overrides](#lesotho-central-bank-overrides)
5. [Data Flow](#data-flow)
6. [Setup and Installation](#setup-and-installation)
7. [Usage Guide](#usage-guide)
8. [API Reference](#api-reference)
9. [Scheduling](#scheduling)
10. [Traceability and Auditability](#traceability-and-auditability)
11. [Idempotency](#idempotency)
12. [Troubleshooting](#troubleshooting)

---

## Overview

The Regulatory Calculation Engine is a Spring Boot-based system designed to automate regulatory reporting calculations for banking institutions in Lesotho. It implements Basel III framework with Lesotho Central Bank (CBL) specific overrides.

### Key Features

- **Automated Calculations**: RWA, CAR, LCR, NPL, and ECL
- **Scheduled Execution**: Bi-weekly, monthly, and annual runs
- **Event-Driven Architecture**: RabbitMQ messaging for decoupled processing
- **Full Auditability**: Complete audit trail for all calculations
- **Idempotent Operations**: Safe to re-run without side effects
- **RESTful API**: Manual triggering and monitoring

### Calculated Metrics

1. **RWA (Risk Weighted Assets)** - Total risk-weighted credit exposures
2. **CAR (Capital Adequacy Ratio)** - Capital to RWA ratio
3. **LCR (Liquidity Coverage Ratio)** - Liquidity buffer adequacy
4. **NPL (Non-Performing Loans)** - Asset quality metrics
5. **ECL (Expected Credit Loss)** - IFRS 9 provisioning

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                         REST API Layer                          │
│              (Manual Job Triggering & Monitoring)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Batch Jobs                          │
│         (Orchestrates calculation workflow)                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Calculation Services                         │
│    RWA | CAR | LCR | NPL | ECL Calculation Logic               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      RabbitMQ Messaging                         │
│           (Event publishing and consumption)                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                          │
│      CBS → source_read → snapshots → metrics                   │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 4.0.2
- **Batch Processing**: Spring Batch
- **Messaging**: RabbitMQ (Spring AMQP)
- **Database**: PostgreSQL 14+
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Java Version**: 25

---

## Basel III Calculations

### 1. Risk Weighted Assets (RWA)

**Formula**: `RWA = Exposure × Risk Weight`

**Implementation**: `RWACalculationService.java`

**Risk Weights (Basel III Standardized Approach)**:
- Sovereign (Lesotho Government): 0%
- Banks: 20%
- Residential Mortgages (LTV ≤ 80%): 35%
- Residential Mortgages (LTV > 80%): 50% (Lesotho override)
- Retail Loans: 75%
- Corporate Loans: 100%
- SME Loans (< LSL 5M): 75%
- SME Loans (≥ LSL 5M): 100%

**Key Logic**:
```java
private BigDecimal determineRiskWeight(LoanExposureSnapshot loan) {
    // Sovereign exposures to Lesotho = 0%
    if (customerType == SOVEREIGN && country == "Lesotho") {
        return 0%;
    }
    
    // Residential mortgages
    if (productType == "MORTGAGE" && loanPurpose == "RESIDENTIAL") {
        if (ltvRatio <= 80%) return 35%;
        else return 50%;  // Lesotho CBL override
    }
    
    // Default logic...
}
```

### 2. Capital Adequacy Ratio (CAR)

**Formula**: `CAR = (Total Capital / RWA) × 100`

**Implementation**: `CARCalculationService.java`

**Capital Structure**:
- **CET1 (Common Equity Tier 1)**: Share capital, retained earnings, reserves
- **AT1 (Additional Tier 1)**: Perpetual non-cumulative preference shares
- **Tier 2**: Subordinated debt, general provisions

**Lesotho CBL Minimum Requirements**:
- **Total CAR**: 15% (vs Basel III 10.5%)
- **Tier 1 Ratio**: 10% (vs Basel III 8.5%)
- **CET1 Ratio**: 8% (vs Basel III 7%)
- **Conservation Buffer**: 2.5%

**Calculation Steps**:
1. Retrieve capital components from `capital_snapshot`
2. Calculate CET1 = Share Capital + Retained Earnings
3. Calculate Tier 1 = CET1 + AT1
4. Calculate Total Capital = Tier 1 + Tier 2
5. Get Total RWA from previous calculation
6. Compute ratios and check compliance

### 3. Liquidity Coverage Ratio (LCR)

**Formula**: `LCR = (HQLA / Net Cash Outflows over 30 days) × 100`

**Implementation**: `LCRCalculationService.java`

**HQLA (High Quality Liquid Assets)**:
- Level 1: Cash, central bank reserves, government securities (0% haircut)
- Level 2A: Corporate bonds, covered bonds (15% haircut)
- Level 2B: Lower-rated corporate bonds, equities (50% haircut)

**Net Cash Outflows**:
- Total Outflows × Run-off rates
- Minus: Total Inflows × Inflow rates (capped at 75% of outflows)

**Lesotho CBL Requirement**: Minimum LCR = 100%

### 4. Non-Performing Loans (NPL)

**Formula**: `NPL Ratio = (Non-Performing Loans / Total Loans) × 100`

**Implementation**: `NPLCalculationService.java`

**Lesotho CBL Classification** (based on Days Past Due):
- **Standard**: 0-30 DPD
- **Watch**: 31-60 DPD
- **Substandard**: 61-90 DPD (NPL begins)
- **Doubtful**: 91-180 DPD
- **Loss**: 180+ DPD

**NPL Components**: Substandard + Doubtful + Loss

**Additional Metrics Calculated**:
- NPL Count
- NPL Amount by classification
- NPL Coverage Ratio (after ECL calculation)

### 5. Expected Credit Loss (ECL)

**Formula**: `ECL = EAD × PD × LGD`

**Implementation**: `ECLCalculationService.java`

**Components**:
- **EAD (Exposure at Default)**: Outstanding loan balance
- **PD (Probability of Default)**: Based on historical data or rating
- **LGD (Loss Given Default)**: 1 - Recovery Rate

**IFRS 9 Staging**:
- **Stage 1**: Performing loans (12-month ECL)
  - PD: Model-based or 1% default
  - Triggers: Current loans, 0-29 DPD
  
- **Stage 2**: Significant increase in credit risk (Lifetime ECL)
  - PD: Model-based or 15% default
  - Triggers: 30+ DPD, restructured, forborne loans
  
- **Stage 3**: Credit-impaired (Lifetime ECL)
  - PD: 100%
  - Triggers: 90+ DPD, NPL classification

**Lesotho CBL Minimum Provisioning**:
- Stage 1: 1% general provision
- Stage 2: 25% specific provision (minimum)
- Stage 3: 100% specific provision

**LGD Calculation**:
```java
if (collateralValue > 0) {
    LGD = 1 - (Collateral Value / Exposure)
} else {
    LGD = 45%  // Default for unsecured
}
```

**Final ECL**: `max(Model ECL, Regulatory Minimum)`

---

## Lesotho Central Bank Overrides

### Capital Requirements (More Stringent than Basel III)

| Metric | Basel III | Lesotho CBL | Override |
|--------|-----------|-------------|----------|
| Minimum CAR | 10.5% | **15%** | +4.5% |
| Minimum Tier 1 | 8.5% | **10%** | +1.5% |
| Minimum CET1 | 7% | **8%** | +1% |
| Conservation Buffer | 2.5% | 2.5% | Same |

### Risk Weight Adjustments

1. **High LTV Mortgages**: 50% instead of 35% (for LTV > 80%)
2. **Public Sector Loans**: 50% instead of 100%
3. **SME Threshold**: LSL 5,000,000 for retail treatment

### NPL Classification

Lesotho CBL uses 5-tier classification instead of Basel's 3-tier:
- More granular monitoring (Watch category at 31-60 DPD)
- Earlier NPL classification (90 DPD vs 180 DPD in some jurisdictions)

### ECL Provisioning

Minimum provisioning floors above IFRS 9 model:
- Stage 1: 1% minimum
- Stage 2: 25% minimum (significantly higher than typical Stage 2)
- Stage 3: 100% (full provisioning)

---

## Data Flow

### Schema-to-Schema Workflow

```
1. CBS (Core Banking System) - Operational Data
   ↓
2. source_read (Read-Only Views) - Real-time view of CBS
   ↓
3. snapshots (Time-Frozen Copy) - Point-in-time snapshot
   ↓
4. metrics (Calculated Results) - Regulatory metrics
```

### Detailed Flow

#### Step 1: CBS Schema
- Contains live operational data
- Tables: `customers`, `accounts`, `loans`, `loan_performance`, `transactions`
- Additional tables: `capital_components`, `liquidity_assets`, `cash_flows`

#### Step 2: source_read Views
- Read-only materialized views
- View: `loan_exposures` (joins loans + accounts + customers + performance)
- View: `capital_base` (aggregates capital components)
- View: `liquidity_positions` (HQLA calculation)

#### Step 3: snapshots Schema
- Time-frozen copy of source_read at calculation time
- Tables:
  - `snapshot_runs`: Control table for each calculation run
  - `loan_exposures_snapshot`: Point-in-time loan data
  - `capital_snapshot`: Point-in-time capital data
  - `liquidity_snapshot`: Point-in-time liquidity data

#### Step 4: metrics Schema
- Calculation results
- Tables:
  - `regulatory_metrics`: Aggregate metrics (CAR, LCR, NPL ratio, etc.)
  - `metric_components`: Loan-level RWA and ECL
  - `calculation_audit`: Full audit trail

### Spring Batch Job Flow

```
1. createSnapshotStep
   ↓
2. copyLoanDataStep
   ↓
3. copyCapitalDataStep
   ↓
4. copyLiquidityDataStep
   ↓
5. validateSnapshotStep
   ↓
6. calculateRWAStep
   ↓
7. calculateNPLStep
   ↓
8. calculateECLStep
   ↓
9. calculateCARStep
   ↓
10. calculateLCRStep
    ↓
11. finalizeSnapshotStep
```

---

## Setup and Installation

### Prerequisites

1. **Java 25**
   ```bash
   java -version
   ```

2. **PostgreSQL 14+**
   ```bash
   psql --version
   ```

3. **RabbitMQ 3.11+**
   ```bash
   rabbitmq-server --version
   ```

4. **Maven 3.8+**
   ```bash
   mvn --version
   ```

### Database Setup

1. **Create Database**:
   ```sql
   CREATE DATABASE BankingSystemDB;
   ```



3. **Verify Schemas**:
   ```sql
   \dn  -- List schemas
   -- Should see: cbs, source_read, snapshots, metrics
   ```

### RabbitMQ Setup

1. **Start RabbitMQ**:
   ```bash
   rabbitmq-server
   ```

2. **Enable Management Plugin**:
   ```bash
   rabbitmq-plugins enable rabbitmq_management
   ```

3. **Access Management UI**:
   - URL: http://localhost:15672
   - Default credentials: guest/guest

### Application Setup

1. **Clone/Extract Project**:
   ```bash
   cd regulatory-calc-engine
   ```

2. **Configure Database** (`application.yaml`):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/BankingSystemDB
       username: your_username
       password: your_password
   ```

3. **Configure RabbitMQ** (`application.yaml`):
   ```yaml
   spring:
     rabbitmq:
       host: localhost
       port: 5672
       username: guest
       password: guest
   ```

4. **Build Application**:
   ```bash
   mvn clean install
   ```

5. **Run Application**:
   ```bash
   mvn spring-boot:run
   ```

6. **Verify Startup**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## Usage Guide

### Automated Scheduling

The engine runs automatically on these schedules:

1. **Bi-Weekly**: 1st and 15th of each month at 2:00 AM
2. **Monthly**: 1st of each month at 1:00 AM
3. **Annual**: January 1st at midnight

To disable scheduling:
```yaml
scheduling:
  enabled: false
```

### Manual Execution via API

#### Trigger Calculation

```bash
POST /api/regulatory-engine/calculate
Content-Type: application/json

{
  "snapshotDate": "2024-12-31",
  "calculationType": "MONTHLY",
  "initiatedBy": "john.doe@bank.com"
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "message": "Calculation job launched successfully",
  "snapshotDate": "2024-12-31",
  "calculationType": "MONTHLY"
}
```







---

## API Reference

### Base URL
```
http://localhost:8080/api/regulatory-engine
```

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/calculate` | Trigger calculation job |

### Calculation Types

- `BI_WEEKLY`: Bi-weekly regulatory reporting
- `MONTHLY`: Month-end regulatory reporting
- `ANNUAL`: Year-end regulatory reporting

### Metric Codes

**Aggregate Metrics**:
- `TOTAL_RWA`: Total Risk Weighted Assets
- `CAR`: Capital Adequacy Ratio
- `CET1_RATIO`: Common Equity Tier 1 Ratio
- `TIER1_RATIO`: Tier 1 Capital Ratio
- `LCR`: Liquidity Coverage Ratio
- `NPL_RATIO`: Non-Performing Loan Ratio
- `TOTAL_ECL`: Total Expected Credit Loss

**Component Metrics**:
- `CET1_CAPITAL`, `TIER1_CAPITAL`, `TOTAL_CAPITAL`
- `NPL_AMOUNT`, `TOTAL_LOANS`
- `STAGE1_ECL`, `STAGE2_ECL`, `STAGE3_ECL`
- `HQLA_TOTAL`, `NET_CASH_OUTFLOWS`

### Snapshot Statuses

- `DRAFT`: Snapshot created, data not yet copied
- `VALIDATED`: Data copied and validated
- `CALCULATED`: All calculations completed
- `APPROVED`: Results approved for reporting
- `FAILED`: Job failed (check logs)

---

## Scheduling

### Cron Expressions

Configure in `application.yaml`:

```yaml
scheduling:
  cron:
    biweekly: "0 0 2 1,15 * ?"
    monthly: "0 0 1 1 * ?"
    annual: "0 0 0 1 1 ?"
```

### Custom Schedules

To add custom schedule, modify `ScheduledCalculationLauncher.java`:

```java
@Scheduled(cron = "0 0 10 * * MON")  // Every Monday 10 AM
public void runWeeklyCalculation() {
    launchCalculationJob(LocalDate.now(), CalculationType.BI_WEEKLY, "WEEKLY_SCHEDULER");
}
```

---

## Traceability and Auditability

### Audit Trail

Every calculation is fully traceable via the `calculation_audit` table:

```sql
SELECT *
FROM metrics.calculation_audit
WHERE snapshot_id = 123
ORDER BY executed_at;
```

**Audit Record Contains**:
- Calculation step name
- Input data (JSON)
- Output data (JSON)
- Calculation rule/formula used
- Execution timestamp
- Execution time (milliseconds)

### Example Audit Query

```sql
-- Get audit trail for RWA calculation
SELECT 
    calculation_step,
    input_data::json->>'loan_count' as loans_processed,
    output_data::json->>'total_rwa' as total_rwa,
    execution_time_ms,
    executed_at
FROM metrics.calculation_audit
WHERE snapshot_id = 123
  AND calculation_step = 'RWA_CALCULATION';
```

### Metadata

All regulatory metrics include metadata:

```sql
SELECT 
    metric_code,
    value,
    metadata::json->>'description' as description,
    metadata::json->>'loan_count' as loans_included
FROM metrics.regulatory_metrics
WHERE snapshot_id = 123;
```

### Explainability

For any metric, you can trace back to:
1. **Snapshot ID**: Which snapshot it came from
2. **Input Data**: Loans/capital/liquidity included
3. **Calculation Rule**: Formula and parameters used
4. **Execution Time**: When it was calculated
5. **Loan-level Details**: Individual RWA and ECL per loan

**Example - Explain CAR**:
```sql
-- 1. Get CAR value
SELECT value FROM metrics.regulatory_metrics 
WHERE snapshot_id = 123 AND metric_code = 'CAR';

-- 2. Get calculation inputs
SELECT input_data 
FROM metrics.calculation_audit
WHERE snapshot_id = 123 AND calculation_step = 'CAR_CALCULATION';

-- 3. Get individual RWA components
SELECT loan_id, exposure_amount, risk_weight, rwa_value
FROM metrics.metric_components
WHERE snapshot_id = 123;
```

---

## Idempotency

### Safe Re-execution

All calculations are idempotent - running the same calculation multiple times produces the same result.

**Mechanisms**:

1. **Unique Constraint**: `(snapshot_date, calculation_type)` prevents duplicate snapshots
2. **Truncate and Reload**: Snapshots are completely replaced, not appended
3. **Transaction Boundaries**: Each step is atomic
4. **Deterministic Calculations**: Same inputs always produce same outputs

### Re-running Failed Jobs

If a job fails:

```bash
# Check failed snapshots
SELECT * FROM snapshots.snapshot_runs 
WHERE status = 'FAILED';

# Delete failed snapshot
DELETE FROM snapshots.snapshot_runs WHERE snapshot_id = 123;

# Re-trigger
POST /api/regulatory/calculate
{
  "snapshotDate": "2024-12-31",
  "calculationType": "MONTHLY"
}
```

### Reprocessing Historical Data

To recalculate historical data with updated logic:

1. Delete old snapshot:
   ```sql
   DELETE FROM snapshots.snapshot_runs 
   WHERE snapshot_date = '2024-11-30' 
     AND calculation_type = 'MONTHLY';
   ```

2. Re-trigger via API with same parameters
3. New snapshot will have latest calculation logic

---

## Troubleshooting

### Common Issues

#### 1. Job Not Starting

**Symptom**: Scheduled jobs don't run

**Solutions**:
- Check `scheduling.enabled: true` in `application.yaml`
- Verify cron expressions
- Check application logs for scheduler initialization

```bash
grep "Scheduled" logs/regulatory-calc-engine.log
```

#### 2. Database Connection Errors

**Symptom**: `PSQLException` or connection timeout

**Solutions**:
- Verify PostgreSQL is running: `pg_isready`
- Check connection string in `application.yaml`
- Verify schemas exist: `\dn` in psql

#### 3. RabbitMQ Connection Failed

**Symptom**: `AmqpConnectException`

**Solutions**:
- Verify RabbitMQ is running: `rabbitmqctl status`
- Check connection parameters in `application.yaml`
- Ensure RabbitMQ is accepting connections on port 5672

#### 4. No Loan Data in Snapshot

**Symptom**: Job fails at validation step

**Solutions**:
- Verify CBS data exists for snapshot date
- Check source_read views have data
- Manually test query:
  ```sql
  SELECT COUNT(*) FROM source_read.loan_exposures;
  ```

#### 5. Calculations Don't Match Expected

**Symptom**: Metrics seem incorrect

**Solutions**:
- Review audit trail for specific snapshot
- Check input data quality
- Verify risk weight parameters in `application.yaml`
- Review calculation rules in service classes

### Logging

#### Enable Debug Logging

```yaml
logging:
  level:
    com.regulatory: DEBUG
    org.springframework.batch: DEBUG
```

#### View Logs

```bash
tail -f logs/regulatory-calc-engine.log
```

#### Important Log Patterns

```
# Job started
"Starting regulatory calculation job"

# Snapshot created
"Created snapshot for date:"

# Calculation completed
"Completed [RWA|CAR|LCR|NPL|ECL] calculation"

# Job finished
"Job completed successfully"
```

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Batch jobs
curl http://localhost:8080/actuator/batch

# Database connectivity
curl http://localhost:8080/actuator/health/db
```

---

## Performance Optimization

### Database Indexes

Ensure these indexes exist (already in schema):

```sql
-- Snapshot queries
CREATE INDEX idx_snapshot_country ON snapshots.loan_exposures_snapshot(country);
CREATE INDEX idx_snapshot_dpd ON snapshots.loan_exposures_snapshot(days_past_due);

-- Metric history
CREATE INDEX idx_metric_history ON metrics.regulatory_metrics(metric_code, snapshot_id);

-- Audit queries
CREATE INDEX idx_audit_snapshot ON metrics.calculation_audit(snapshot_id, calculation_step);
```

### Batch Processing

For large portfolios (>100K loans), consider:

1. **Chunk Processing**: Modify batch configuration to process in chunks
2. **Parallel Processing**: Enable partition processing
3. **Database Tuning**: Increase `work_mem` and `shared_buffers` in PostgreSQL

### RabbitMQ Tuning

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 20
    cache:
      connection:
        size: 25
```

---

## Monitoring

### Metrics Exposed

Via Actuator:
- Job execution times
- Success/failure rates
- Database connection pool stats
- RabbitMQ queue depths

### Prometheus Integration

Add to `application.yaml`:
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
```

Access: `http://localhost:8080/actuator/prometheus`

### Alerting

Set up alerts for:
- CAR below 15%
- LCR below 100%
- NPL ratio above threshold (e.g., 5%)
- Job failures

---

## Security Considerations

### API Security

In production, add Spring Security:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Database Security

- Use separate read-only user for source_read views
- Restrict write access to metrics schema
- Enable SSL for database connections

### Audit Compliance

- All calculations are logged
- User actions are tracked (via `initiated_by`)
- Immutable audit trail in `calculation_audit`

---

## Contact and Support

For issues or questions:
- Review audit trail in database
- Check application logs
- Review this documentation
- Contact: regulatory-systems@yourbank.com

---

## Appendix A: SQL Queries

### Get Latest Metrics

```sql
SELECT 
    sr.snapshot_date,
    sr.status,
    rm.metric_code,
    rm.value,
    rm.unit
FROM snapshots.snapshot_runs sr
JOIN metrics.regulatory_metrics rm ON sr.snapshot_id = rm.snapshot_id
WHERE sr.snapshot_date = (SELECT MAX(snapshot_date) FROM snapshots.snapshot_runs)
ORDER BY rm.metric_code;
```

### Compliance Check

```sql
SELECT 
    snapshot_date,
    MAX(CASE WHEN metric_code = 'CAR' THEN value END) as car,
    MAX(CASE WHEN metric_code = 'LCR' THEN value END) as lcr,
    MAX(CASE WHEN metric_code = 'CET1_RATIO' THEN value END) as cet1,
    CASE 
        WHEN MAX(CASE WHEN metric_code = 'CAR' THEN value END) >= 15
         AND MAX(CASE WHEN metric_code = 'LCR' THEN value END) >= 100
         AND MAX(CASE WHEN metric_code = 'CET1_RATIO' THEN value END) >= 8
        THEN 'COMPLIANT'
        ELSE 'NON-COMPLIANT'
    END as compliance_status
FROM snapshots.snapshot_runs sr
JOIN metrics.regulatory_metrics rm ON sr.snapshot_id = rm.snapshot_id
WHERE sr.status = 'CALCULATED'
GROUP BY sr.snapshot_id, sr.snapshot_date
ORDER BY sr.snapshot_date DESC
LIMIT 12;  -- Last 12 months
```

### NPL Trend Analysis

```sql
SELECT 
    sr.snapshot_date,
    rm.value as npl_ratio
FROM snapshots.snapshot_runs sr
JOIN metrics.regulatory_metrics rm ON sr.snapshot_id = rm.snapshot_id
WHERE rm.metric_code = 'NPL_RATIO'
  AND sr.calculation_type = 'MONTHLY'
ORDER BY sr.snapshot_date DESC
LIMIT 12;
```

---

## Appendix B: RabbitMQ Queues

### Queue Configuration

| Queue Name | Purpose | TTL | DLQ |
|------------|---------|-----|-----|
| `calculation.rwa.queue` | RWA calculation events | 1 hour | Yes |
| `calculation.car.queue` | CAR calculation events | 1 hour | Yes |
| `calculation.lcr.queue` | LCR calculation events | 1 hour | Yes |
| `calculation.npl.queue` | NPL calculation events | 1 hour | Yes |
| `calculation.ecl.queue` | ECL calculation events | 1 hour | Yes |
| `calculation.snapshot.queue` | Snapshot lifecycle events | 1 hour | Yes |
| `calculation.notification.queue` | Notifications | 24 hours | Yes |

### Routing Keys

- `snapshot.created`
- `snapshot.validated`
- `snapshot.completed`
- `calculation.rwa`
- `calculation.car`
- `calculation.lcr`
- `calculation.npl`
- `calculation.ecl`
- `notification.snapshot.completed`
- `notification.calculation.failed`

---

*End of Documentation*
