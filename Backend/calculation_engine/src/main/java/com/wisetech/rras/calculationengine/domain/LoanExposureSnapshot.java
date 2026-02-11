package com.wisetech.rras.calculationengine.domain;

import com.wisetech.rras.calculationengine.enums.AssetClassification;
import com.wisetech.rras.calculationengine.enums.CustomerCategory;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_exposures_snapshot", schema = "snapshots")
@IdClass(LoanExposureSnapshotId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanExposureSnapshot {

    @Id
    @Column(name = "snapshot_id")
    private int snapshotId;

    @Id
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "customer_id")
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private CustomerCategory customerType;

    @Column(name = "country")
    private String country;

    @Column(name = "country_risk_rating")
    private Integer countryRiskRating;

    @Column(name = "internal_rating")
    private String internalRating;

    @Column(name = "pd_value")
    private BigDecimal pdValue;

    @Column(name = "lgd_value")
    private BigDecimal lgdValue;

    @Column(name = "is_financial_inst")
    private Boolean isFinancialInstitution;

    @Column(name = "is_public_sector")
    private Boolean isPublicSector;

    @Column(name = "principal_amount")
    private BigDecimal principalAmount;

    @Column(name = "outstanding_balance")
    private BigDecimal outstandingBalance;

    @Column(name = "collateral_value")
    private BigDecimal collateralValue;

    @Column(name = "collateral_type")
    private String collateralType;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "loan_purpose")
    private String loanPurpose;

    @Column(name = "ltv_ratio")
    private BigDecimal ltvRatio;

    @Column(name = "days_past_due")
    private Integer daysPastDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class")
    private AssetClassification assetClass;

    @Column(name = "stage")
    private Integer stage;

    @Column(name = "is_restructured")
    private Boolean isRestructured;

    @Column(name = "is_forborne")
    private Boolean isForborne;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "remaining_term_months")
    private Integer remainingTermMonths;

    @Column(name = "currency")
    private String currency;
}