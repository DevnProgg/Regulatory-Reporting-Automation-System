package com.wisetech.rras.calculationengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import com.wisetech.rras.calculationengine.domain.LoanExposureSnapshot;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.enums.AssetClassification;
import com.wisetech.rras.calculationengine.repository.CalculationAuditRepository;
import com.wisetech.rras.calculationengine.repository.LoanExposureSnapshotRepository;
import com.wisetech.rras.calculationengine.repository.RegulatoryMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-Performing Loan (NPL) Calculation Service
 *
 * NPL Ratio = (Non-Performing Loans / Total Loans) × 100
 *
 * Lesotho CBL Classification:
 * - Standard: 0-30 DPD
 * - Watch: 31-60 DPD
 * - Substandard: 61-90 DPD (NPL starts here)
 * - Doubtful: 91-180 DPD
 * - Loss: 180+ DPD
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NPLCalculationService {

    private final LoanExposureSnapshotRepository loanRepository;
    private final RegulatoryMetricRepository regulatoryMetricRepository;
    private final CalculationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, BigDecimal> calculateNPL(int snapshotId) {
        long startTime = System.currentTimeMillis();
        log.info("Starting NPL calculation for snapshot {}", snapshotId);

        List<LoanExposureSnapshot> allLoans = loanRepository.findBySnapshotId(snapshotId);

        BigDecimal totalLoans = BigDecimal.ZERO;
        BigDecimal nplLoans = BigDecimal.ZERO;
        BigDecimal substandardLoans = BigDecimal.ZERO;
        BigDecimal doubtfulLoans = BigDecimal.ZERO;
        BigDecimal lossLoans = BigDecimal.ZERO;

        int totalCount = 0;
        int nplCount = 0;

        for (LoanExposureSnapshot loan : allLoans) {
            BigDecimal balance = loan.getOutstandingBalance();
            totalLoans = totalLoans.add(balance);
            totalCount++;

            AssetClassification classification = loan.getAssetClass();

            if (loan.getDaysPastDue() >= 90) {
                nplLoans = nplLoans.add(balance);
                nplCount++;

                switch (classification) {
                    case SUBSTANDARD:
                        substandardLoans = substandardLoans.add(balance);
                        break;
                    case DOUBTFUL:
                        doubtfulLoans = doubtfulLoans.add(balance);
                        break;
                    case LOSS:
                        lossLoans = lossLoans.add(balance);
                        break;
                }
            }
        }

        // Calculate ratios
        BigDecimal nplRatio = calculatePercentage(nplLoans, totalLoans);
        BigDecimal substandardRatio = calculatePercentage(substandardLoans, totalLoans);
        BigDecimal doubtfulRatio = calculatePercentage(doubtfulLoans, totalLoans);
        BigDecimal lossRatio = calculatePercentage(lossLoans, totalLoans);

        // Save metrics
        saveMetric(snapshotId, "TOTAL_LOANS", totalLoans, "CURRENCY");
        saveMetric(snapshotId, "NPL_AMOUNT", nplLoans, "CURRENCY");
        saveMetric(snapshotId, "NPL_RATIO", nplRatio, "PERCENTAGE");
        saveMetric(snapshotId, "NPL_COUNT", BigDecimal.valueOf(nplCount), "COUNT");

        saveMetric(snapshotId, "SUBSTANDARD_AMOUNT", substandardLoans, "CURRENCY");
        saveMetric(snapshotId, "SUBSTANDARD_RATIO", substandardRatio, "PERCENTAGE");

        saveMetric(snapshotId, "DOUBTFUL_AMOUNT", doubtfulLoans, "CURRENCY");
        saveMetric(snapshotId, "DOUBTFUL_RATIO", doubtfulRatio, "PERCENTAGE");

        saveMetric(snapshotId, "LOSS_AMOUNT", lossLoans, "CURRENCY");
        saveMetric(snapshotId, "LOSS_RATIO", lossRatio, "PERCENTAGE");

        // Coverage ratio (will be calculated after ECL)
        saveMetric(snapshotId, "LOAN_COUNT", BigDecimal.valueOf(totalCount), "COUNT");

        // Audit
        long executionTime = System.currentTimeMillis() - startTime;
        auditCalculation(snapshotId, "NPL_CALCULATION", totalCount, nplCount,
                nplLoans, totalLoans, nplRatio, executionTime);

        log.info("Completed NPL calculation for snapshot {}: NPL Ratio = {}%",
                snapshotId, nplRatio);

        Map<String, BigDecimal> results = new HashMap<>();
        results.put("NPL_RATIO", nplRatio);
        results.put("NPL_AMOUNT", nplLoans);
        results.put("TOTAL_LOANS", totalLoans);

        return results;
    }

    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void saveMetric(int snapshotId, String metricCode, BigDecimal value, String unit) {
        RegulatoryMetric metric = RegulatoryMetric.builder()
                .snapshotId(snapshotId)
                .metricCode(metricCode)
                .value(value)
                .unit(unit)
                .build();

        regulatoryMetricRepository.save(metric);
    }

    private void auditCalculation(int snapshotId, String step, int totalCount,
                                  int nplCount, BigDecimal nplAmount,
                                  BigDecimal totalAmount, BigDecimal nplRatio,
                                  long executionTime) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("total_loan_count", totalCount);
            inputData.put("npl_loan_count", nplCount);

            Map<String, Object> outputData = new HashMap<>();
            outputData.put("npl_amount", nplAmount);
            outputData.put("total_amount", totalAmount);
            outputData.put("npl_ratio", nplRatio);

            CalculationAudit audit = CalculationAudit.builder()
                    .snapshotId(snapshotId)
                    .calculationStep(step)
                    .inputData(objectMapper.writeValueAsString(inputData))
                    .outputData(objectMapper.writeValueAsString(outputData))
                    .calculationRule("Lesotho CBL NPL Classification: NPL = Substandard + Doubtful + Loss")
                    .executionTimeMs((int) executionTime)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error creating audit record", e);
        }
    }
}