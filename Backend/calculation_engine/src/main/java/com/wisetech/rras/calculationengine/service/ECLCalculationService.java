package com.wisetech.rras.calculationengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import com.wisetech.rras.calculationengine.domain.LoanExposureSnapshot;
import com.wisetech.rras.calculationengine.domain.MetricComponentId;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.enums.AssetClassification;
import com.wisetech.rras.calculationengine.repository.CalculationAuditRepository;
import com.wisetech.rras.calculationengine.repository.LoanExposureSnapshotRepository;
import com.wisetech.rras.calculationengine.repository.MetricComponentRepository;
import com.wisetech.rras.calculationengine.repository.RegulatoryMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Expected Credit Loss (ECL) Calculation Service
 * Implements IFRS 9 ECL model with Lesotho Central Bank provisioning requirements
 *
 * ECL = PD × LGD × EAD
 *
 * IFRS 9 Staging:
 * - Stage 1: Performing loans (12-month ECL)
 * - Stage 2: Underperforming (Lifetime ECL)
 * - Stage 3: Credit-impaired (Lifetime ECL)
 *
 * Lesotho CBL Minimum Provisioning:
 * - Stage 1: 1% general provision
 * - Stage 2: 25% specific provision (minimum)
 * - Stage 3: 100% specific provision
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ECLCalculationService {

    private final LoanExposureSnapshotRepository loanRepository;
    private final MetricComponentRepository metricComponentRepository;
    private final RegulatoryMetricRepository regulatoryMetricRepository;
    private final CalculationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    @Value("${ecl.stage1.general-provision:1.0}")
    private BigDecimal stage1MinProvision;

    @Value("${ecl.stage2.specific-provision-min:25.0}")
    private BigDecimal stage2MinProvision;

    @Value("${ecl.stage3.loss-provision:100.0}")
    private BigDecimal stage3MinProvision;

    @Transactional
    public BigDecimal calculateECL(int snapshotId) {
        long startTime = System.currentTimeMillis();
        log.info("Starting ECL calculation for snapshot {}", snapshotId);

        List<LoanExposureSnapshot> loans = loanRepository.findBySnapshotId(snapshotId);

        BigDecimal totalECL = BigDecimal.ZERO;
        BigDecimal stage1ECL = BigDecimal.ZERO;
        BigDecimal stage2ECL = BigDecimal.ZERO;
        BigDecimal stage3ECL = BigDecimal.ZERO;

        int stage1Count = 0;
        int stage2Count = 0;
        int stage3Count = 0;

        for (LoanExposureSnapshot loan : loans) {
            // Determine IFRS 9 stage
            Integer stage = determineStage(loan);

            // Calculate ECL
            BigDecimal eclAmount = calculateLoanECL(loan, stage);
            totalECL = totalECL.add(eclAmount);

            // Update metric component with ECL
            updateMetricComponentWithECL(snapshotId, loan.getLoanId(), eclAmount, stage);

            // Aggregate by stage
            switch (stage) {
                case 1:
                    stage1ECL = stage1ECL.add(eclAmount);
                    stage1Count++;
                    break;
                case 2:
                    stage2ECL = stage2ECL.add(eclAmount);
                    stage2Count++;
                    break;
                case 3:
                    stage3ECL = stage3ECL.add(eclAmount);
                    stage3Count++;
                    break;
            }
        }

        // Save metrics
        saveMetric(snapshotId, "TOTAL_ECL", totalECL, "CURRENCY");
        saveMetric(snapshotId, "STAGE1_ECL", stage1ECL, "CURRENCY");
        saveMetric(snapshotId, "STAGE2_ECL", stage2ECL, "CURRENCY");
        saveMetric(snapshotId, "STAGE3_ECL", stage3ECL, "CURRENCY");

        saveMetric(snapshotId, "STAGE1_COUNT", BigDecimal.valueOf(stage1Count), "COUNT");
        saveMetric(snapshotId, "STAGE2_COUNT", BigDecimal.valueOf(stage2Count), "COUNT");
        saveMetric(snapshotId, "STAGE3_COUNT", BigDecimal.valueOf(stage3Count), "COUNT");

        // Calculate NPL coverage ratio
        BigDecimal nplAmount = regulatoryMetricRepository
                .findBySnapshotIdAndMetricCode(snapshotId, "NPL_AMOUNT")
                .map(RegulatoryMetric::getValue)
                .orElse(BigDecimal.ZERO);

        BigDecimal coverageRatio = calculateCoverageRatio(totalECL, nplAmount);
        saveMetric(snapshotId, "NPL_COVERAGE_RATIO", coverageRatio, "PERCENTAGE");

        // Audit
        long executionTime = System.currentTimeMillis() - startTime;
        auditCalculation(snapshotId, "ECL_CALCULATION", loans.size(),
                totalECL, coverageRatio, executionTime);

        log.info("Completed ECL calculation for snapshot {}: Total ECL = {}, Coverage = {}%",
                snapshotId, totalECL, coverageRatio);

        return totalECL;
    }

    /**
     * Determine IFRS 9 staging based on loan performance
     */
    private Integer determineStage(LoanExposureSnapshot loan) {
        // Stage 3: Credit-impaired (90+ DPD or NPL classification)
        if (loan.getDaysPastDue() != null && loan.getDaysPastDue() >= 90) {
            return 3;
        }

        if (loan.getAssetClass() != null && loan.getAssetClass().getValue().equals(AssetClassification.DOUBTFUL.toString())) {
            return 3;
        }

        // Stage 2: Significant increase in credit risk (30+ DPD)
        if (loan.getDaysPastDue() != null && loan.getDaysPastDue() >= 30) {
            return 2;
        }

        // Restructured or forborne loans move to Stage 2
        if ((loan.getIsRestructured() != null && loan.getIsRestructured()) ||
                (loan.getIsForborne() != null && loan.getIsForborne())) {
            return 2;
        }

        // Stage 1: Performing loans
        return 1;
    }

    /**
     * Calculate ECL for individual loan
     */
    private BigDecimal calculateLoanECL(LoanExposureSnapshot loan, Integer stage) {
        BigDecimal exposureAmount = loan.getOutstandingBalance();

        // Get PD and LGD
        BigDecimal pd = loan.getPdValue() != null ?
                loan.getPdValue() : getDefaultPD(stage);
        BigDecimal lgd = loan.getLgdValue() != null ?
                loan.getLgdValue() : getDefaultLGD(loan);

        // ECL = EAD × PD × LGD
        BigDecimal modelECL = exposureAmount
                .multiply(pd)
                .multiply(lgd);

        // Apply Lesotho CBL minimum provisioning
        BigDecimal minProvision = getMinimumProvision(exposureAmount, stage);

        // Take the higher of model ECL or regulatory minimum
        return modelECL.max(minProvision);
    }

    /**
     * Get default PD based on stage
     */
    private BigDecimal getDefaultPD(Integer stage) {
        return switch (stage) {
            case 1 -> BigDecimal.valueOf(0.01);  // 1%
            case 2 -> BigDecimal.valueOf(0.15);  // 15%
            case 3 -> BigDecimal.valueOf(1.00);  // 100%
            default -> BigDecimal.valueOf(0.01);
        };
    }

    /**
     * Get default LGD based on collateral
     */
    private BigDecimal getDefaultLGD(LoanExposureSnapshot loan) {
        BigDecimal collateralValue = loan.getCollateralValue();
        BigDecimal exposure = loan.getOutstandingBalance();

        if (collateralValue != null && collateralValue.compareTo(BigDecimal.ZERO) > 0) {
            // LGD = 1 - (Collateral / Exposure)
            BigDecimal recoveryRate = collateralValue
                    .divide(exposure, 4, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE);
            return BigDecimal.ONE.subtract(recoveryRate);
        }

        // Default LGD for unsecured loans
        return BigDecimal.valueOf(0.45); // 45%
    }

    /**
     * Calculate minimum regulatory provision (Lesotho CBL)
     */
    private BigDecimal getMinimumProvision(BigDecimal exposure, Integer stage) {
        BigDecimal rate = switch (stage) {
            case 1 -> stage1MinProvision;   // 1%
            case 2 -> stage2MinProvision;   // 25%
            case 3 -> stage3MinProvision;   // 100%
            default -> stage1MinProvision;
        };

        return exposure.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void updateMetricComponentWithECL(int snapshotId, Long loanId,
                                              BigDecimal eclAmount, Integer stage) {
        MetricComponentId id = new MetricComponentId(snapshotId, loanId);
        metricComponentRepository.findById(id).ifPresent(component -> {
            component.setEclAmount(eclAmount);
            component.setEclStage(stage);
            component.setProvisionAmount(eclAmount);
            metricComponentRepository.save(component);
        });
    }

    private BigDecimal calculateCoverageRatio(BigDecimal totalECL, BigDecimal nplAmount) {
        if (nplAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalECL.multiply(BigDecimal.valueOf(100))
                .divide(nplAmount, 2, RoundingMode.HALF_UP);
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

    private void auditCalculation(int snapshotId, String step, int loanCount,
                                  BigDecimal totalECL, BigDecimal coverageRatio,
                                  long executionTime) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("loan_count", loanCount);

            Map<String, Object> outputData = new HashMap<>();
            outputData.put("total_ecl", totalECL);
            outputData.put("npl_coverage_ratio", coverageRatio);

            CalculationAudit audit = CalculationAudit.builder()
                    .snapshotId(snapshotId)
                    .calculationStep(step)
                    .inputData(objectMapper.writeValueAsString(inputData))
                    .outputData(objectMapper.writeValueAsString(outputData))
                    .calculationRule("IFRS 9 ECL with Lesotho CBL minimum provisioning")
                    .executionTimeMs((int) executionTime)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error creating audit record", e);
        }
    }
}