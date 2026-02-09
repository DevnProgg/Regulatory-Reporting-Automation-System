package com.wisetech.rras.model.snapshot;

import com.wisetech.rras.model.auditing.AuditableEntity;
import com.wisetech.rras.model.cbs.Customer_Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@AllArgsConstructor
@Getter
@Setter
@Table(name = "loan_exposures_snapshot",
        schema = "snapshot",
        catalog = "RegulatoryReportingSystem")
@IdClass(LoanExposureSnapshotID.class)
public class LoanExposuresSnapshot extends AuditableEntity {

    @Id
    @Column(name = "snapshot_id")
    private long snapshotID;

    @Id
    @Column(name = "loan_id")
    private long loanID;

    @NotNull(message="Customer Category should not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_category", nullable = false)
    private Customer_Category customerCategory;

    @DecimalMin(value = "0.01", message = "Principal Amount should be greater than 0")
    @Column(name = "principal_amount")
    private double principalAmount;

    @DecimalMin(value = "0.01", message = "Collateral value should be greater than 0")
    @Column(name = "collateral_value")
    private double collateralValue;

    @Column(name = "days_past_due")
    private int daysPastDue;

    @NotBlank(message = "Country should not be blank")
    @Column(name = "country", nullable = false)
    private String country;
}
@Data
class LoanExposureSnapshotID implements Serializable{
    private long snapshotID;
    private long loanID;
}