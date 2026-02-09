package com.wisetech.rras.calculationengine.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanExposureSnapshotId implements Serializable {
    private Long snapshotId;
    private Long loanId;
}