package com.wisetech.rras.calculationengine.domain;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "regulatory_metrics", schema = "metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulatoryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long metricId;

    @Column(name = "snapshot_id", nullable = false)
    private int snapshotId;

    @Column(name = "metric_code", nullable = false, length = 50)
    private String metricCode;

    @Column(name = "value", nullable = false)
    private BigDecimal value;

    @Column(name = "unit", length = 20)
    private String unit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "calculated_at")
    @Builder.Default
    private ZonedDateTime calculatedAt = ZonedDateTime.now();
}