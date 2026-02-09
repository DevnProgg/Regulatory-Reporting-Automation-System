package com.wisetech.rras.calculationengine.batch;

import com.wisetech.rras.calculationengine.enums.CalculationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class ScheduledCalculationLauncher {

    private final JobLauncher jobLauncher;
    private final Job regulatoryCalculationJob;

    @Value("${scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(cron = "${scheduling.cron.biweekly:0 0 2 1,15 * ?}")
    public void runBiWeeklyCalculation() {
        run(LocalDate.now(), CalculationType.BI_WEEKLY);
    }

    @Scheduled(cron = "${scheduling.cron.monthly:0 0 1 1 * ?}")
    public void runMonthlyCalculation() {
        // Run for the previous month's end date usually, but sticking to your logic:
        run(LocalDate.now().minusDays(1), CalculationType.MONTHLY);
    }

    @Scheduled(cron = "${scheduling.cron.annual:0 0 0 1 1 ?}")
    public void runAnnualCalculation() {
        run(LocalDate.now().minusDays(1), CalculationType.ANNUAL);
    }

    private void run(LocalDate snapshotDate, CalculationType type) {
        if (!schedulingEnabled) {
            log.info("Scheduling is disabled. Skipping {} run.", type);
            return;
        }

        try {
            // Using UUID for run.id ensures every scheduled run is treated as a unique JobInstance
            // If you want to prevent re-running the same date/type combination, remove "run.id"
            JobParameters params = new JobParametersBuilder()
                    .addString("snapshotDate", snapshotDate.toString())
                    .addString("calculationType", type.name())
                    .addString("initiatedBy", "SCHEDULER")
                    .addString("jobId", UUID.randomUUID().toString())
                    .toJobParameters();

            log.info("Launching {} calculation for {}", type, snapshotDate);
            jobLauncher.run(regulatoryCalculationJob, params);

        } catch (Exception e) {
            log.error("Failed to launch {} calculation job", type, e);
        }
    }
}