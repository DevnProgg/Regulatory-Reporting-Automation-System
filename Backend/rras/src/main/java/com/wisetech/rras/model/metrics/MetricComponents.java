package com.wisetech.rras.model.metrics;

import com.wisetech.rras.model.auditing.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@AllArgsConstructor
@Getter
@Setter
@IdClass(MetricComponentsID.class)
@Table(name = "metric_components",
        schema = "metrics",
        catalog = "RegulatoryReportingSystem")
public class MetricComponents extends AuditableEntity {

    @Id
    @Column(name = "snapshot_id")
    private long snapshotID;

    @Id
    @Column(name = "loan_id")
    private long loanID;

    @DecimalMin(value = "0.01", message = "RWA Should not be less then 0")
    @Column(name = "rwa_value", nullable = false)
    private double rwaValue;

    @DecimalMin(value = "0.01", message = "Risk Weight should not be less than 0")
    @Column(name = "risk_weight", nullable = false)
    private double riskWeight;
}

@Data
class MetricComponentsID implements Serializable{
    private long snapshotID;
    private long loanID;
}