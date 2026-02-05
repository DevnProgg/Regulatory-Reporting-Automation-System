package com.wisetech.rras.model.snapshot;

import com.wisetech.rras.model.cbs.Customer_Category;
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

@Entity
@AllArgsConstructor
public class Loan_Exposures_Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long snapshot_id;

    @NotNull(message = "Loan id cannot be null")
    private @Setter @Getter long loan_id;

    @NotNull(message="Customer Category should not be null")
    private @Getter @Setter Customer_Category customer_category;

    @DecimalMin(value = "0.01", message = "Principal Amount should be greater than 0")
    private @Setter @Getter double principal_amount;

    @DecimalMin(value = "0.01", message = "Collateral value should be greater than 0")
    private @Setter @Getter double collateral_value;

    private @Setter @Getter int days_past_due;

    @NotBlank(message = "Country should not be blank")
    private @Getter @Setter String country;
}
