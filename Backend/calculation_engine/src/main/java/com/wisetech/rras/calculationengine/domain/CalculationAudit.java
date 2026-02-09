package com.wisetech.rras.calculationengine.domain;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.ZonedDateTime;

@Entity
@Table(name = "calculation_audit", schema = "metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "calculation_step", nullable = false, length = 100)
    private String calculationStep;

    @Column(name = "input_data", columnDefinition = "jsonb")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "jsonb")
    private String outputData;

    @Column(name = "calculation_rule", columnDefinition = "TEXT")
    private String calculationRule;

    @Column(name = "executed_at")
    @Builder.Default
    private ZonedDateTime executedAt = ZonedDateTime.now();

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;
}