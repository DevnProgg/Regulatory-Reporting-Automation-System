package com.wisetech.rras.model.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricComponentId implements Serializable {
    private int snapshotId;
    private Long loanId;
}