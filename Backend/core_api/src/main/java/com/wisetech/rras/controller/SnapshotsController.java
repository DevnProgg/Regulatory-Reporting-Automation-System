package com.wisetech.rras.controller;

import com.wisetech.rras.enums.CalculationType;
import com.wisetech.rras.model.metrics.RegulatoryMetric;
import com.wisetech.rras.model.snapshot.SnapshotRun;
import com.wisetech.rras.repository.RegulatoryMetricRepository;
import com.wisetech.rras.repository.SnapshotRepository;
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
@RequestMapping("/api/regulatory/")
@Slf4j
@RequiredArgsConstructor
public class SnapshotsController {

    private final SnapshotRepository snapshotRunRepository;
    private final RegulatoryMetricRepository metricRepository;

    /**
     * Get status of a specific snapshot run
     */
     @GetMapping("/snapshot/{snapshotId}")
     public ResponseEntity<SnapshotRun> getSnapshotStatus(@PathVariable int snapshotId) {
     return snapshotRunRepository.findById(snapshotId)
     .map(ResponseEntity::ok)
     .orElse(ResponseEntity.notFound().build());
     }

    /**
     * Get all snapshots for a specific date
     */
     @GetMapping("/snapshots")
     public ResponseEntity<List<SnapshotRun>> getSnapshots(
     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

     List<SnapshotRun> snapshots = snapshotRunRepository.findAll()
     .stream()
     .filter(s -> s.getSnapshotDate().equals(date))
     .toList();

     return ResponseEntity.ok(snapshots);
     }

    /**
     * Get all metrics for a snapshot
     */
     @GetMapping("/snapshot/{snapshotId}/metrics")
     public ResponseEntity<List<RegulatoryMetric>> getSnapshotMetrics(
     @PathVariable int snapshotId) {

     List<RegulatoryMetric> metrics = metricRepository.findBySnapshotId(snapshotId);
     return ResponseEntity.ok(metrics);
     }


    /**
     * Get a specific metric across time
     */
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

    /**
     * Get summary dashboard data for latest snapshot
     */
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


    @Data
    public static class CalculationRequest {
        private LocalDate snapshotDate;
        private CalculationType calculationType;
        private String initiatedBy;
    }
}
