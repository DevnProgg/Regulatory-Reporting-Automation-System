package com.wisetech.rras.calculationengine.controller;

import com.wisetech.rras.calculationengine.batch.ScheduledCalculationLauncher;
import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import com.wisetech.rras.calculationengine.domain.SnapshotRun;
import com.wisetech.rras.calculationengine.enums.CalculationType;
import com.wisetech.rras.calculationengine.repository.RegulatoryMetricRepository;
import com.wisetech.rras.calculationengine.repository.SnapshotRunRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/regulatory-engine/")
@Slf4j
@RequiredArgsConstructor
public class RegulatoryCalculationController {

    private final ScheduledCalculationLauncher jobLauncher;
    private final SnapshotRunRepository snapshotRunRepository;
    private final RegulatoryMetricRepository metricRepository;

    /**
     * Trigger a regulatory calculation job manually
    */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> triggerCalculation(
            @RequestBody CalculationRequest request) {

        log.info("Received manual calculation request: {}", request);

        try {
            jobLauncher.launchCalculationJob(
                    request.getSnapshotDate(),
                    request.getCalculationType() != null ? request.getCalculationType() : CalculationType.BI_WEEKLY,
                    request.getInitiatedBy() != null ? request.getInitiatedBy() : "API"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Calculation job launched successfully");
            response.put("snapshotDate", request.getSnapshotDate());
            response.put("calculationType", request.getCalculationType());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to launch calculation job", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Failed to launch calculation job: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get status of a specific snapshot run

    @GetMapping("/snapshot/{snapshotId}")
    public ResponseEntity<SnapshotRun> getSnapshotStatus(@PathVariable int snapshotId) {
        return snapshotRunRepository.findById(snapshotId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }*/

    /**
     * Get all snapshots for a specific date

    @GetMapping("/snapshots")
    public ResponseEntity<List<SnapshotRun>> getSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<SnapshotRun> snapshots = snapshotRunRepository.findAll()
                .stream()
                .filter(s -> s.getSnapshotDate().equals(date))
                .toList();

        return ResponseEntity.ok(snapshots);
    }*/

    /**
     * Get all metrics for a snapshot

    @GetMapping("/snapshot/{snapshotId}/metrics")
    public ResponseEntity<List<RegulatoryMetric>> getSnapshotMetrics(
            @PathVariable int snapshotId) {

        List<RegulatoryMetric> metrics = metricRepository.findBySnapshotId(snapshotId);
        return ResponseEntity.ok(metrics);
    }
     */

    /**
     * Get a specific metric across time

    @GetMapping("/metrics/{metricCode}/history")
    public ResponseEntity<List<RegulatoryMetric>> getMetricHistory(
            @PathVariable String metricCode,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        List<RegulatoryMetric> metrics = metricRepository
                .findByMetricCodeOrderBySnapshotIdDesc(metricCode)
                .stream()
                .limit(limit)
                .toList();

        return ResponseEntity.ok(metrics);
    }
     */
    /**
     * Get summary dashboard data for latest snapshot

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        List<SnapshotRun> snapshots = snapshotRunRepository.findAll();

        if (snapshots.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No snapshots available"));
        }

        SnapshotRun latest = snapshots.stream()
                .max((a, b) -> a.getSnapshotDate().compareTo(b.getSnapshotDate()))
                .orElseThrow();

        List<RegulatoryMetric> metrics = metricRepository.findBySnapshotId(latest.getSnapshotId());

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("snapshotId", latest.getSnapshotId());
        dashboard.put("snapshotDate", latest.getSnapshotDate());
        dashboard.put("status", latest.getStatus());

        Map<String, Object> metricMap = new HashMap<>();
        for (RegulatoryMetric metric : metrics) {
            metricMap.put(metric.getMetricCode(), Map.of(
                    "value", metric.getValue(),
                    "unit", metric.getUnit()
            ));
        }
        dashboard.put("metrics", metricMap);

        return ResponseEntity.ok(dashboard);
    }
     */

    @Data
    public static class CalculationRequest {
        private LocalDate snapshotDate;
        private CalculationType calculationType;
        private String initiatedBy;
    }
}