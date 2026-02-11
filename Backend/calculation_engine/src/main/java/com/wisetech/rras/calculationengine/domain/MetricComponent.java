package com.wisetech.rras.calculationengine.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "metric_components", schema = "metrics")
@IdClass(MetricComponentId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricComponent {

    @Id
    @Column(name = "snapshot_id")
    private int snapshotId;

    @Id
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "exposure_amount", nullable = false)
    private BigDecimal exposureAmount;

    @Column(name = "risk_weight", nullable = false)
    private BigDecimal riskWeight;

    @Column(name = "rwa_value", nullable = false)
    private BigDecimal rwaValue;

    @Column(name = "ecl_amount")
    private BigDecimal eclAmount;

    @Column(name = "ecl_stage")
    private Integer eclStage;

    @Column(name = "provision_amount")
    private BigDecimal provisionAmount;
}