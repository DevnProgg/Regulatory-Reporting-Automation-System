package com.wisetech.rras.calculationengine.service;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.repository.CalculationAuditRepository;
import com.wisetech.rras.calculationengine.repository.MetricComponentRepository;
import com.wisetech.rras.calculationengine.repository.RegulatoryMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Capital Adequacy Ratio (CAR) Calculation Service
 * Implements Basel III capital framework with Lesotho Central Bank requirements
 *
 * CAR = (Total Capital / Risk Weighted Assets) × 100
 *
 * Lesotho CBL Requirements:
 * - Minimum CAR: 15% (vs Basel III 10.5%)
 * - Minimum Tier 1: 10% (vs Basel III 8.5%)
 * - Minimum CET1: 8% (vs Basel III 7%)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CARCalculationService {

    private final MetricComponentRepository metricComponentRepository;
    private final RegulatoryMetricRepository regulatoryMetricRepository;
    private final CalculationAuditRepository auditRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${regulatory.lesotho.min-car:15.0}")
    private BigDecimal minCAR;

    @Value("${regulatory.lesotho.min-car-tier1:10.0}")
    private BigDecimal minTier1Ratio;

    @Value("${regulatory.lesotho.min-car-cet1:8.0}")
    private BigDecimal minCET1Ratio;

    @Value("${regulatory.lesotho.conservation-buffer:2.5}")
    private BigDecimal conservationBuffer;

    /**
     * Calculate Capital Adequacy Ratio
     */
    @Transactional
    public Map<String, BigDecimal> calculateCAR(Long snapshotId, LocalDate snapshotDate) {
        long startTime = System.currentTimeMillis();
        log.info("Starting CAR calculation for snapshot {}", snapshotId);

        // Get total RWA
        BigDecimal totalRWA = metricComponentRepository.getTotalRWA(snapshotId);

        if (totalRWA == null || totalRWA.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("RWA must be calculated before CAR");
        }

        // Get capital components from snapshot
        Map<String, BigDecimal> capitalComponents = getCapitalComponents(snapshotDate);

        BigDecimal cet1Capital = capitalComponents.get("CET1");
        BigDecimal tier1Capital = capitalComponents.get("TIER1");
        BigDecimal totalCapital = capitalComponents.get("TOTAL");

        // Calculate ratios
        BigDecimal cet1Ratio = calculateRatio(cet1Capital, totalRWA);
        BigDecimal tier1Ratio = calculateRatio(tier1Capital, totalRWA);
        BigDecimal totalCARRatio = calculateRatio(totalCapital, totalRWA);

        // Save metrics
        saveMetric(snapshotId, "CET1_CAPITAL", cet1Capital, "CURRENCY");
        saveMetric(snapshotId, "TIER1_CAPITAL", tier1Capital, "CURRENCY");
        saveMetric(snapshotId, "TOTAL_CAPITAL", totalCapital, "CURRENCY");
        saveMetric(snapshotId, "CET1_RATIO", cet1Ratio, "PERCENTAGE");
        saveMetric(snapshotId, "TIER1_RATIO", tier1Ratio, "PERCENTAGE");
        saveMetric(snapshotId, "CAR", totalCARRatio, "PERCENTAGE");

        // Calculate buffers and surplus
        BigDecimal cet1Surplus = cet1Ratio.subtract(minCET1Ratio);
        BigDecimal tier1Surplus = tier1Ratio.subtract(minTier1Ratio);
        BigDecimal carSurplus = totalCARRatio.subtract(minCAR);

        saveMetric(snapshotId, "CET1_SURPLUS", cet1Surplus, "PERCENTAGE");
        saveMetric(snapshotId, "TIER1_SURPLUS", tier1Surplus, "PERCENTAGE");
        saveMetric(snapshotId, "CAR_SURPLUS", carSurplus, "PERCENTAGE");

        // Check compliance
        boolean isCompliant = cet1Ratio.compareTo(minCET1Ratio) >= 0 &&
                tier1Ratio.compareTo(minTier1Ratio) >= 0 &&
                totalCARRatio.compareTo(minCAR) >= 0;

        saveMetric(snapshotId, "CAR_COMPLIANT",
                isCompliant ? BigDecimal.ONE : BigDecimal.ZERO, "BOOLEAN");

        // Audit
        long executionTime = System.currentTimeMillis() - startTime;
        auditCalculation(snapshotId, "CAR_CALCULATION", capitalComponents,
                totalRWA, totalCARRatio, isCompliant, executionTime);

        log.info("Completed CAR calculation for snapshot {}: CAR = {}%, Compliant = {}",
                snapshotId, totalCARRatio, isCompliant);

        Map<String, BigDecimal> results = new HashMap<>();
        results.put("CET1_RATIO", cet1Ratio);
        results.put("TIER1_RATIO", tier1Ratio);
        results.put("CAR", totalCARRatio);
        results.put("COMPLIANT", isCompliant ? BigDecimal.ONE : BigDecimal.ZERO);

        return results;
    }

    /**
     * Get capital components from the capital snapshot
     */
    private Map<String, BigDecimal> getCapitalComponents(LocalDate snapshotDate) {
        String sql = """
            SELECT 
                SUM(CASE WHEN component_type = 'CET1' THEN amount ELSE 0 END) as cet1,
                SUM(CASE WHEN component_type = 'AT1' THEN amount ELSE 0 END) as at1,
                SUM(CASE WHEN component_type = 'T2' THEN amount ELSE 0 END) as t2
            FROM cbs.capital_components
            WHERE as_of_date = ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            BigDecimal cet1 = rs.getBigDecimal("cet1");
            BigDecimal at1 = rs.getBigDecimal("at1");
            BigDecimal t2 = rs.getBigDecimal("t2");

            Map<String, BigDecimal> components = new HashMap<>();
            components.put("CET1", cet1 != null ? cet1 : BigDecimal.ZERO);
            components.put("AT1", at1 != null ? at1 : BigDecimal.ZERO);
            components.put("TIER1", components.get("CET1").add(components.get("AT1")));
            components.put("TIER2", t2 != null ? t2 : BigDecimal.ZERO);
            components.put("TOTAL", components.get("TIER1").add(components.get("TIER2")));

            return components;
        }, snapshotDate);
    }

    private BigDecimal calculateRatio(BigDecimal capital, BigDecimal rwa) {
        if (rwa.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return capital.multiply(BigDecimal.valueOf(100))
                .divide(rwa, 2, RoundingMode.HALF_UP);
    }

    private void saveMetric(Long snapshotId, String metricCode, BigDecimal value, String unit) {
        RegulatoryMetric metric = RegulatoryMetric.builder()
                .snapshotId(snapshotId)
                .metricCode(metricCode)
                .value(value)
                .unit(unit)
                .build();

        regulatoryMetricRepository.save(metric);
    }

    private void auditCalculation(Long snapshotId, String step,
                                  Map<String, BigDecimal> capital,
                                  BigDecimal rwa, BigDecimal car,
                                  boolean compliant, long executionTime) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("capital_components", capital);
            inputData.put("total_rwa", rwa);

            Map<String, Object> outputData = new HashMap<>();
            outputData.put("car", car);
            outputData.put("compliant", compliant);
            outputData.put("min_car_required", minCAR);

            CalculationAudit audit = CalculationAudit.builder()
                    .snapshotId(snapshotId)
                    .calculationStep(step)
                    .inputData(objectMapper.writeValueAsString(inputData))
                    .outputData(objectMapper.writeValueAsString(outputData))
                    .calculationRule("Basel III CAR with Lesotho CBL minimum ratios")
                    .executionTimeMs((int) executionTime)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error creating audit record", e);
        }
    }
}