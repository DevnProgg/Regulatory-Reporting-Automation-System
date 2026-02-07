package com.wisetech.rras.model.source_read;

import com.wisetech.rras.model.cbs.Customer_Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter @Setter
public class Loan_Exposures {

    @NotNull(message = "Loan id cannot be null")
    private long loan_id;

    @NotNull(message = "account id cannot be null")
    private long account_id;

    @NotNull(message = "Customer id cannot be null")
    private long customer_id;

    @NotNull(message = "Customer type should not be null")
    private Customer_Category customer_type;

    @DecimalMin(value = "0.01", message = "Principal Amount should be greater than 0")
    private double principal_amount;

    @DecimalMin(value = "0.01", message = "Interest rate should be greater than 0")
    private double interest_rate;

    @DecimalMin(value = "0.01", message = "Collateral value should be greater than 0")
    private double collateral_value;

    @NotNull(message = "Date should not be null")
    private Date last_payment_date;

    @NotBlank(message = "Country should not be blank")
    private String country;
}
