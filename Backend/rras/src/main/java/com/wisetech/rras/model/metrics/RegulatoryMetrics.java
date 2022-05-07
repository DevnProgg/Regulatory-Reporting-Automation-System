package com.wisetech.rras.model.metrics;

import com.wisetech.rras.model.auditing.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
@Getter
@Setter
@Table(name="regulatory_metrics",
        schema = "metrics",
        catalog = "RegulatoryReportingSystem")
public class RegulatoryMetrics extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private long metricID;

    @NotNull(message = "Snapshot id cannot be null")
    @Column(name = "snapshot_id")
    private long snapshotID;

    @NotBlank(message = "Code should not be blank")
    @Column(name = "metric_code", nullable = false)
    private String metricCode;

    @DecimalMin(value = "0.01", message = "Value should be greater than 0")
    @Column(name = "value")
    private double value;

    @NotBlank(message = "Unit should not be blank")
    @Column(name = "unit", nullable = false)
    private String unit;

    @NotNull(message = "Date should not be null")
    @Column(name = "calculated_at")
    private Date calculatedAt;
}
