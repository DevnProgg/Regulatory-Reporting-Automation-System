package com.wisetech.rras.model.metrics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@AllArgsConstructor
@Getter
@Setter
public class Metric_Components {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long snapshot_id;

    @NotNull(message = "Loan id cannot be null")
    private long loan_id;

    @DecimalMin(value = "0.01", message = "RWA Should not be less then 0")
    private double rwa_value;

    @DecimalMin(value = "0.01", message = "Risk Weight should not be less than 0")
    private double risk_weight;
}
