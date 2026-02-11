package com.wisetech.rras.calculationengine.domain;

import com.wisetech.rras.calculationengine.enums.CalculationType;
import com.wisetech.rras.calculationengine.enums.RunStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "snapshot_runs", schema = "snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private int snapshotId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false)
    private CalculationType calculationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private RunStatus status = RunStatus.DRAFT;

    @Column(name = "initiated_by")
    private String initiatedBy;

    @Column(name = "created_at")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "validated_at")
    private ZonedDateTime validatedAt;

    @Column(name = "calculated_at")
    private ZonedDateTime calculatedAt;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;
}