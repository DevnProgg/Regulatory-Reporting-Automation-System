package com.wisetech.rras.calculationengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.repository.CalculationAuditRepository;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Liquidity Coverage Ratio (LCR) Calculation Service
 *
 * LCR = (High Quality Liquid Assets / Total Net Cash Outflows over 30 days) × 100
 *
 * Lesotho CBL Requirement: Minimum LCR = 100%
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LCRCalculationService {

    private final RegulatoryMetricRepository regulatoryMetricRepository;
    private final CalculationAuditRepository auditRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${regulatory.lesotho.min-lcr:100.0}")
    private BigDecimal minLCR;

    @Transactional
    public Map<String, BigDecimal> calculateLCR(Long snapshotId, LocalDate snapshotDate) {
        long startTime = System.currentTimeMillis();
        log.info("Starting LCR calculation for snapshot {}", snapshotId);

        // Calculate HQLA (High Quality Liquid Assets)
        BigDecimal totalHQLA = calculateHQLA(snapshotDate);

        // Calculate Net Cash Outflows
        Map<String, BigDecimal> cashFlows = calculateNetCashOutflows(snapshotDate);
        BigDecimal totalOutflows = cashFlows.get("TOTAL_OUTFLOWS");
        BigDecimal totalInflows = cashFlows.get("TOTAL_INFLOWS");
        BigDecimal netCashOutflows = totalOutflows.subtract(totalInflows);

        // Cap inflows at 75% of outflows (Basel III rule)
        BigDecimal cappedInflows = totalOutflows.multiply(BigDecimal.valueOf(0.75));
        if (totalInflows.compareTo(cappedInflows) > 0) {
            netCashOutflows = totalOutflows.subtract(cappedInflows);
        }

        // Calculate LCR
        BigDecimal lcr = calculateRatio(totalHQLA, netCashOutflows);

        // Save metrics
        saveMetric(snapshotId, "HQLA_TOTAL", totalHQLA, "CURRENCY");
        saveMetric(snapshotId, "CASH_OUTFLOWS", totalOutflows, "CURRENCY");
        saveMetric(snapshotId, "CASH_INFLOWS", totalInflows, "CURRENCY");
        saveMetric(snapshotId, "NET_CASH_OUTFLOWS", netCashOutflows, "CURRENCY");
        saveMetric(snapshotId, "LCR", lcr, "PERCENTAGE");

        boolean isCompliant = lcr.compareTo(minLCR) >= 0;
        saveMetric(snapshotId, "LCR_COMPLIANT",
                isCompliant ? BigDecimal.ONE : BigDecimal.ZERO, "BOOLEAN");

        // Audit
        long executionTime = System.currentTimeMillis() - startTime;
        auditCalculation(snapshotId, "LCR_CALCULATION", totalHQLA,
                netCashOutflows, lcr, isCompliant, executionTime);

        log.info("Completed LCR calculation for snapshot {}: LCR = {}%, Compliant = {}",
                snapshotId, lcr, isCompliant);

        Map<String, BigDecimal> results = new HashMap<>();
        results.put("LCR", lcr);
        results.put("HQLA", totalHQLA);
        results.put("NET_OUTFLOWS", netCashOutflows);
        results.put("COMPLIANT", isCompliant ? BigDecimal.ONE : BigDecimal.ZERO);

        return results;
    }

    /**
     * Calculate High Quality Liquid Assets
     */
    private BigDecimal calculateHQLA(LocalDate snapshotDate) {
        String sql = """
            SELECT SUM(hqla_value) as total_hqla
            FROM cbs.liquidity_assets
            WHERE as_of_date = ? AND is_unencumbered = true
            """;

        BigDecimal hqla = jdbcTemplate.queryForObject(sql, BigDecimal.class, snapshotDate);
        return hqla != null ? hqla : BigDecimal.ZERO;
    }

    /**
     * Calculate Net Cash Outflows over 30-day stressed period
     */
    private Map<String, BigDecimal> calculateNetCashOutflows(LocalDate snapshotDate) {
        LocalDate endDate = snapshotDate.plusDays(30);

        // Calculate outflows with run-off rates
        String outflowSql = """
            SELECT SUM(contractual_amount * COALESCE(run_off_rate, 1.0)) as total_outflows
            FROM cbs.cash_flows
            WHERE as_of_date = ? 
            AND flow_type = 'OUTFLOW'
            AND expected_date BETWEEN ? AND ?
            """;

        BigDecimal outflows = jdbcTemplate.queryForObject(outflowSql, BigDecimal.class,
                snapshotDate, snapshotDate, endDate);

        // Calculate inflows with inflow rates
        String inflowSql = """
            SELECT SUM(contractual_amount * COALESCE(inflow_rate, 0.5)) as total_inflows
            FROM cbs.cash_flows
            WHERE as_of_date = ? 
            AND flow_type = 'INFLOW'
            AND expected_date BETWEEN ? AND ?
            """;

        BigDecimal inflows = jdbcTemplate.queryForObject(inflowSql, BigDecimal.class,
                snapshotDate, snapshotDate, endDate);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("TOTAL_OUTFLOWS", outflows != null ? outflows : BigDecimal.ZERO);
        result.put("TOTAL_INFLOWS", inflows != null ? inflows : BigDecimal.ZERO);

        return result;
    }

    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
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

    private void auditCalculation(Long snapshotId, String step, BigDecimal hqla,
                                  BigDecimal netOutflows, BigDecimal lcr,
                                  boolean compliant, long executionTime) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("hqla", hqla);
            inputData.put("net_outflows", netOutflows);

            Map<String, Object> outputData = new HashMap<>();
            outputData.put("lcr", lcr);
            outputData.put("compliant", compliant);
            outputData.put("min_lcr_required", minLCR);

            CalculationAudit audit = CalculationAudit.builder()
                    .snapshotId(snapshotId)
                    .calculationStep(step)
                    .inputData(objectMapper.writeValueAsString(inputData))
                    .outputData(objectMapper.writeValueAsString(outputData))
                    .calculationRule("Basel III LCR with 30-day stress scenario")
                    .executionTimeMs((int) executionTime)
                    .build();

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error creating audit record", e);
        }
    }
}