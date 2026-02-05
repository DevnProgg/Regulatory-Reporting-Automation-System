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
public class Metric_Components {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Getter @Setter long snapshot_id;

    @NotNull(message = "Loan id cannot be null")
    private @Getter @Setter long loan_id;

    @DecimalMin(value = "0.01", message = "RWA Should not be less then 0")
    private @Getter @Setter double rwa_value;

    @DecimalMin(value = "0.01", message = "Risk Weight should not be less than 0")
    private @Getter @Setter double risk_weight;
}
