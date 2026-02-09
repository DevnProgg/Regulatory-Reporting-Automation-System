package com.wisetech.rras.calculationengine.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import com.wisetech.rras.calculationengine.domain.LoanExposureSnapshot;
import com.wisetech.rras.calculationengine.domain.MetricComponent;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.enums.CustomerCategory;
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
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Risk Weighted Assets (RWA) Calculation Service
 * Implements Basel III standardized approach with Lesotho Central Bank overrides
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RWACalculationService {

    private final LoanExposureSnapshotRepository loanRepository;
    private final MetricComponentRepository metricComponentRepository;
    private final RegulatoryMetricRepository regulatoryMetricRepository;
    private final CalculationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    // Basel III Standard Risk Weights
    @Value("${basel.standard.retail-mortgage-rw:35.0}")
    private BigDecimal retailMortgageRW;

    @Value("${basel.standard.retail-other-rw:75.0}")
    private BigDecimal retailOtherRW;

    @Value("${basel.standard.corporate-rw:100.0}")
    private BigDecimal corporateRW;

    @Value("${basel.standard.sovereign-rw:0.0}")
    private BigDecimal sovereignRW;

    @Value("${basel.standard.bank-rw:20.0}")
    private BigDecimal bankRW;

    /**
     * Calculate RWA for all loans in a snapshot
     * This is the main entry point called by the batch job
     */
    @Transactional
    public BigDecimal calculateRWA(Long snapshotId) {
        long startTime = System.currentTimeMillis();
        log.info("Starting RWA calculation for snapshot {}", snapshotId);

        List<LoanExposureSnapshot> loans = loanRepository.findBySnapshotId(snapshotId);
        BigDecimal totalRWA = BigDecimal.ZERO;

        for (LoanExposureSnapshot loan : loans) {
            MetricComponent component = calculateLoanRWA(snapshotId, loan);
            metricComponentRepository.save(component);
            totalRWA = totalRWA.add(component.getRwaValue());
        }

        // Save aggregate metric
        RegulatoryMetric rwaMetric = RegulatoryMetric.builder()
                .snapshotId(snapshotId)
                .metricCode("TOTAL_RWA")
                .value(totalRWA)
                .unit("CURRENCY")
                .metadata(createMetadata("Total Risk Weighted Assets", loans.size()))
                .build();

        regulatoryMetricRepository.save(rwaMetric);

        // Audit trail
        long executionTime = System.currentTimeMillis() - startTime;
        auditCalculation(snapshotId, "RWA_CALCULATION", loans.size(), totalRWA, executionTime);

        log.info("Completed RWA calculation for snapshot {}: Total RWA = {}",
                snapshotId, totalRWA);

        return totalRWA;
    }

    /**
     * Calculate RWA for a single loan
     * Applies appropriate risk weight based on loan characteristics
     */
    private MetricComponent calculateLoanRWA(Long snapshotId, LoanExposureSnapshot loan) {
        BigDecimal exposureAmount = loan.getOutstandingBalance();
        BigDecimal riskWeight = determineRiskWeight(loan);

        // RWA = Exposure × Risk Weight
        BigDecimal rwaValue = exposureAmount
                .multiply(riskWeight)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return MetricComponent.builder()
                .snapshotId(snapshotId)
                .loanId(loan.getLoanId())
                .exposureAmount(exposureAmount)
                .riskWeight(riskWeight.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .rwaValue(rwaValue)
                .build();
    }

    /**
     * Determine risk weight based on Basel III + Lesotho CBL rules
     */
    private BigDecimal determineRiskWeight(LoanExposureSnapshot loan) {
        CustomerCategory customerType = loan.getCustomerType();
        String productType = loan.getProductType();
        String loanPurpose = loan.getLoanPurpose();
        BigDecimal ltvRatio = loan.getLtvRatio();

        // Lesotho CBL Override: Sovereign exposures to Lesotho government = 0%
        if (customerType == CustomerCategory.SOVEREIGN && "Lesotho".equalsIgnoreCase(loan.getCountry())) {
            return sovereignRW;
        }

        // Bank exposures
        if (customerType == CustomerCategory.BANK ||
                (loan.getIsFinancialInstitution() != null && loan.getIsFinancialInstitution())) {
            return bankRW;
        }

        // Corporate exposures
        if (customerType == CustomerCategory.CORP) {
            // Lesotho CBL: Reduced risk weight for loans to public sector entities
            if (loan.getIsPublicSector() != null && loan.getIsPublicSector()) {
                return BigDecimal.valueOf(50.0); // 50% risk weight
            }
            return corporateRW;
        }

        // Retail exposures - Residential mortgages
        if (customerType == CustomerCategory.RETAIL &&
                "MORTGAGE".equalsIgnoreCase(productType) &&
                "RESIDENTIAL".equalsIgnoreCase(loanPurpose)) {

            // Basel III: LTV <= 80% gets lower risk weight
            if (ltvRatio != null && ltvRatio.compareTo(BigDecimal.valueOf(0.80)) <= 0) {
                return retailMortgageRW; // 35%
            } else {
                // Lesotho CBL: Higher risk weight for high LTV mortgages
                return BigDecimal.valueOf(50.0); // 50% instead of 35%
            }
        }

        // SME exposures treated as retail if exposure < LSL 5M
        if (customerType == CustomerCategory.SME) {
            BigDecimal smeThreshold = BigDecimal.valueOf(5_000_000); // LSL 5M
            if (loan.getOutstandingBalance().compareTo(smeThreshold) < 0) {
                return retailOtherRW; // 75%
            } else {
                return corporateRW; // 100%
            }
        }

        // Other retail exposures
        if (customerType == CustomerCategory.RETAIL) {
            return retailOtherRW; // 75%
        }

        // Default to 100% for any unclassified exposure
        return corporateRW;
    }

    private String createMetadata(String description, int loanCount) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("description", description);
            metadata.put("loan_count", loanCount);
            metadata.put("calculation_date", ZonedDateTime.now().toString());
            metadata.put("methodology", "Basel III Standardized Approach - Lesotho CBL");
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("Error creating metadata", e);
            return "{}";
        }
    }

    private void auditCalculation(Long snapshotId, String step, int inputCount,
                                  BigDecimal output, long executionTime) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("loan_count", inputCount);

            Map<String, Object> outputData = new HashMap<>();
            outputData.put("total_rwa", output);

            CalculationAudit audit = CalculationAudit.builder()
                    .snapshotId(snapshotId)
                    .calculationStep(step)
                    .inputData(objectMapper.writeValueAsString(inputData))
                    .outputData(objectMapper.writeValueAsString(outputData))
                    .calculationRule("Basel III Standardized Approach with Lesotho CBL overrides")
                    .executionTimeMs((int) executionTime)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error creating audit record", e);
        }
    }
}