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


}