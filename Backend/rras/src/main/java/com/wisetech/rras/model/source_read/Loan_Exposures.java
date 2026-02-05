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
public class Loan_Exposures {

    @NotNull(message = "Loan id cannot be null")
    private @Setter @Getter long loan_id;

    @NotNull(message = "account id cannot be null")
    private @Setter @Getter long account_id;

    @NotNull(message = "Customer id cannot be null")
    private @Setter @Getter long customer_id;

    @NotNull(message = "Customer type should not be null")
    private @Setter @Getter Customer_Category customer_type;

    @DecimalMin(value = "0.01", message = "Principal Amount should be greater than 0")
    private @Setter @Getter double principal_amount;

    @DecimalMin(value = "0.01", message = "Interest rate should be greater than 0")
    private @Setter @Getter double interest_rate;

    @DecimalMin(value = "0.01", message = "Collateral value should be greater than 0")
    private @Setter @Getter double collateral_value;

    @NotNull(message = "Date should not be null")
    private @Setter @Getter Date last_payment_date;

    @NotBlank(message = "Country should not be blank")
    private @Getter @Setter String country;
}
