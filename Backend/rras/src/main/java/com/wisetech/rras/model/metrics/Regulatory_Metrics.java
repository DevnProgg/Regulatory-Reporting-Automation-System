package com.wisetech.rras.model.metrics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
public class Regulatory_Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Getter @Setter long metric_id;

    @NotNull(message = "Snapshot id cannot be null")
    private @Getter @Setter long snapshot_id;

    @NotBlank(message = "Code should not be blank")
    private @Getter @Setter String metric_code;

    @DecimalMin(value = "0.01", message = "Value should be greater than 0")
    private @Getter @Setter double value;

    @NotBlank(message = "Unit should not be blank")
    private @Getter @Setter String unit;

    @NotNull(message = "Date should not be null")
    private @Getter @Setter Date calculated_at;
}
