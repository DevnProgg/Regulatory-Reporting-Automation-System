package com.wisetech.rras.calculationengine.batch;


import com.wisetech.rras.calculationengine.enums.CalculationType;
import com.wisetech.rras.calculationengine.enums.RunStatus;
import com.wisetech.rras.calculationengine.messaging.CalculationEventPublisher;
import com.wisetech.rras.calculationengine.domain.SnapshotRun; // Assuming model package
import com.wisetech.rras.calculationengine.repository.SnapshotRunRepository;
import com.wisetech.rras.calculationengine.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RegulatoryCalculationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SnapshotRunRepository snapshotRunRepository;
    private final JdbcTemplate jdbcTemplate;

    // Services
    private final RWACalculationService rwaService;
    private final CARCalculationService carService;
    private final NPLCalculationService nplService;
    private final ECLCalculationService eclService;
    private final LCRCalculationService lcrService;
    private final CalculationEventPublisher eventPublisher;

    @Bean
    public Job regulatoryCalculationJob(Step createSnapshotStep,
                                        Step copyLoanDataStep,
                                        Step copyCapitalDataStep,
                                        Step copyLiquidityDataStep,
                                        Step validateSnapshotStep,
                                        Step calculateRWAStep,
                                        Step calculateNPLStep,
                                        Step calculateECLStep,
                                        Step calculateCARStep,
                                        Step calculateLCRStep,
                                        Step finalizeSnapshotStep) {
        return new JobBuilder("regulatoryCalculationJob", jobRepository)
                .start(createSnapshotStep)
                .next(copyLoanDataStep)
                .next(copyCapitalDataStep)
                .next(copyLiquidityDataStep)
                .next(validateSnapshotStep)
                .next(calculateRWAStep)
                .next(calculateNPLStep)
                .next(calculateECLStep)
                .next(calculateCARStep)
                .next(calculateLCRStep)
                .next(finalizeSnapshotStep)
                .build();
    }

    // Create Snapshot
    @Bean
    public Step createSnapshotStep() {
        return new StepBuilder("createSnapshot", jobRepository)
                .tasklet(createSnapshotTasklet(), transactionManager)
                .build();
    }

    private Tasklet createSnapshotTasklet() {
        return (contribution, chunkContext) -> {
            Map<String, Object> jobParams = chunkContext.getStepContext().getJobParameters();

            LocalDate snapshotDate = LocalDate.parse((String) jobParams.get("snapshotDate"));
            String typeStr = (String) jobParams.get("calculationType");
            CalculationType calcType = CalculationType.valueOf(typeStr);
            String initiatedBy = (String) jobParams.getOrDefault("initiatedBy", "SYSTEM");

            log.info("Creating snapshot for date: {}, type: {}", snapshotDate, calcType);

            SnapshotRun snapshot = SnapshotRun.builder()
                    .snapshotDate(snapshotDate)
                    .calculationType(calcType)
                    .status(RunStatus.DRAFT)
                    .initiatedBy(initiatedBy)
                    .createdAt(ZonedDateTime.now())
                    .build();

            snapshot = snapshotRunRepository.save(snapshot);

            // Pass Snapshot ID to future steps via Job Execution Context
            chunkContext.getStepContext().getStepExecution().getJobExecution()
                    .getExecutionContext().putLong("snapshotId", snapshot.getSnapshotId());

            eventPublisher.publishSnapshotCreated(snapshot.getSnapshotId(), snapshotDate);
            return RepeatStatus.FINISHED;
        };
    }

    //  Copy Loan Data (ELT)
    @Bean
    public Step copyLoanDataStep() {
        return new StepBuilder("copyLoanData", jobRepository)
                .tasklet(copyLoanDataTasklet(), transactionManager)
                .build();
    }

    private Tasklet copyLoanDataTasklet() {
        return (contribution, chunkContext) -> {
            Long snapshotId = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getLong("snapshotId");

            log.info("Copying loan data to snapshot {}", snapshotId);

            String sql = """
                INSERT INTO snapshots.loan_exposures_snapshot (
                    snapshot_id, loan_id, customer_id, customer_type, country,
                    country_risk_rating, internal_rating, pd_value, lgd_value,
                    is_financial_inst, is_public_sector, principal_amount,
                    outstanding_balance, collateral_value, collateral_type,
                    product_type, loan_purpose, ltv_ratio, days_past_due,
                    asset_class, stage, is_restructured, is_forborne,
                    maturity_date, remaining_term_months, currency
                )
                SELECT 
                    ?, loan_id, customer_id, customer_type, country,
                    country_risk_rating, internal_rating, pd_value, lgd_value,
                    is_financial_inst, is_public_sector, principal_amount,
                    outstanding_balance, collateral_value, collateral_type,
                    product_type, loan_purpose, ltv_ratio, days_past_due,
                    asset_class, stage, is_restructured, is_forborne,
                    maturity_date, remaining_term_months, currency
                FROM source_read.loan_exposures
                """;

            int rows = jdbcTemplate.update(sql, snapshotId);
            log.info("Copied {} loan records to snapshot {}", rows, snapshotId);
            return RepeatStatus.FINISHED;
        };
    }

    //  Copy Capital Data
    @Bean
    public Step copyCapitalDataStep() {
        return new StepBuilder("copyCapitalData", jobRepository)
                .tasklet(copyCapitalDataTasklet(), transactionManager)
                .build();
    }

    private Tasklet copyCapitalDataTasklet() {
        return (contribution, chunkContext) -> {
            Long snapshotId = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getLong("snapshotId");

            // Retrieve job param safely
            String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("snapshotDate");
            LocalDate snapshotDate = LocalDate.parse(dateStr);

            log.info("Copying capital data to snapshot {}", snapshotId);

            String sql = """
                INSERT INTO snapshots.capital_snapshot (
                    snapshot_id, component_type, component_name, amount, currency
                )
                SELECT 
                    ?, component_type, component_name, 
                    (amount + COALESCE(regulatory_adjustment, 0)), currency
                FROM cbs.capital_components
                WHERE as_of_date = ?
                """;

            int rows = jdbcTemplate.update(sql, snapshotId, snapshotDate);
            log.info("Copied {} capital records", rows);
            return RepeatStatus.FINISHED;
        };
    }

    // Copy Liquidity Data
    @Bean
    public Step copyLiquidityDataStep() {
        return new StepBuilder("copyLiquidityData", jobRepository)
                .tasklet(copyLiquidityDataTasklet(), transactionManager)
                .build();
    }

    private Tasklet copyLiquidityDataTasklet() {
        return (contribution, chunkContext) -> {
            Long snapshotId = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getLong("snapshotId");
            String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("snapshotDate");
            LocalDate snapshotDate = LocalDate.parse(dateStr);

            String sql = """
                INSERT INTO snapshots.liquidity_snapshot (
                    snapshot_id, asset_id, asset_type, hqla_value, hqla_level, currency
                )
                SELECT 
                    ?, asset_id, asset_type, hqla_value, hqla_level, currency
                FROM source_read.liquidity_positions
                WHERE as_of_date = ?
                """;

            int rows = jdbcTemplate.update(sql, snapshotId, snapshotDate);
            log.info("Copied {} liquidity records", rows);
            return RepeatStatus.FINISHED;
        };
    }

    // Validate Snapshot
    @Bean
    public Step validateSnapshotStep() {
        return new StepBuilder("validateSnapshot", jobRepository)
                .tasklet(validateSnapshotTasklet(), transactionManager)
                .build();
    }

    private Tasklet validateSnapshotTasklet() {
        return (contribution, chunkContext) -> {
            Long snapshotId = chunkContext.getStepContext().getStepExecution()
                    .getJobExecution().getExecutionContext().getLong("snapshotId");

            Integer loanCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM snapshots.loan_exposures_snapshot WHERE snapshot_id = ?",
                    Integer.class, snapshotId);

            if (loanCount == null || loanCount == 0) {
                throw new IllegalStateException("Validation Failed: No loan data found for snapshot " + snapshotId);
            }

            snapshotRunRepository.findById(snapshotId).ifPresent(snapshot -> {
                snapshot.setStatus(RunStatus.VALIDATED);
                snapshot.setValidatedAt(ZonedDateTime.now());
                snapshotRunRepository.save(snapshot);
            });

            eventPublisher.publishSnapshotValidated(snapshotId);
            return RepeatStatus.FINISHED;
        };
    }

    // Calculation Steps (Wrapper Pattern)

    private Step createCalculationStep(String stepName, String calcType, Runnable calcAction) {
        return new StepBuilder(stepName, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long snapshotId = chunkContext.getStepContext().getStepExecution()
                            .getJobExecution().getExecutionContext().getLong("snapshotId");

                    log.info("Starting {} Calculation for Snapshot {}", calcType, snapshotId);
                    calcAction.run();
                    eventPublisher.publishCalculationCompleted(snapshotId, calcType);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean(name = "calculateRWAStep")
    public Step calculateRWAStepImpl() {
        return new StepBuilder("calculateRWA", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");
                    rwaService.calculateRWA(id);
                    eventPublisher.publishCalculationCompleted(id, "RWA");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    @Bean
    public Step calculateNPLStep() {
        return new StepBuilder("calculateNPL", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");
                    nplService.calculateNPL(id);
                    eventPublisher.publishCalculationCompleted(id, "NPL");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    @Bean
    public Step calculateECLStep() {
        return new StepBuilder("calculateECL", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");
                    eclService.calculateECL(id);
                    eventPublisher.publishCalculationCompleted(id, "ECL");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    @Bean
    public Step calculateCARStep() {
        return new StepBuilder("calculateCAR", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");
                    String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("snapshotDate");
                    carService.calculateCAR(id, LocalDate.parse(dateStr));
                    eventPublisher.publishCalculationCompleted(id, "CAR");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    @Bean
    public Step calculateLCRStep() {
        return new StepBuilder("calculateLCR", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");
                    String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("snapshotDate");
                    lcrService.calculateLCR(id, LocalDate.parse(dateStr));
                    eventPublisher.publishCalculationCompleted(id, "LCR");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    // Finalize

    @Bean
    public Step finalizeSnapshotStep() {
        return new StepBuilder("finalizeSnapshot", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long id = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getLong("snapshotId");

                    snapshotRunRepository.findById(id).ifPresent(snapshot -> {
                        snapshot.setStatus(RunStatus.CALCULATED);
                        snapshot.setCalculatedAt(ZonedDateTime.now());
                        snapshotRunRepository.save(snapshot);
                    });

                    eventPublisher.publishSnapshotCompleted(id);
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }
}