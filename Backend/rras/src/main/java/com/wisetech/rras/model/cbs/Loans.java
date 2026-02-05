package com.wisetech.rras.model.cbs;

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
public class Loans {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long loan_id;

    @NotNull(message = "Account id cannot be null")
    private @Setter @Getter long account_id;

    @DecimalMin(value = "0.01", message = "Principal Amount should be greater than 0")
    private @Setter @Getter double principal_amount;

    @DecimalMin(value = "0.01", message = "Interest rate should be greater than 0")
    private @Setter @Getter double interest_rate;

    @NotNull(message = "Date should not be null")
    private @Setter @Getter Date maturity_date;

    @DecimalMin(value = "0.01", message = "Collateral value should be greater than 0")
    private @Setter @Getter double collateral_value;

    @NotBlank(message = "Product Type should not be blank")
    private @Setter @Getter String product_type;
}
