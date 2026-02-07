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
@Getter
@Setter
public class Regulatory_Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long metric_id;

    @NotNull(message = "Snapshot id cannot be null")
    private long snapshot_id;

    @NotBlank(message = "Code should not be blank")
    private String metric_code;

    @DecimalMin(value = "0.01", message = "Value should be greater than 0")
    private double value;

    @NotBlank(message = "Unit should not be blank")
    private String unit;

    @NotNull(message = "Date should not be null")
    private Date calculated_at;
}
