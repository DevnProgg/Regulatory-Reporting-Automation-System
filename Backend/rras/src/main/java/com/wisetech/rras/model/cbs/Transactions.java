package com.wisetech.rras.model.cbs;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long txn_id;

    @NotNull(message = "Account id cannot be null")
    private @Setter @Getter  long account_id;

    @NotNull(message = "Transaction Date cannot be null")
    private @Setter @Getter Date txn_date;

    @DecimalMin(value = "0.01", message = "Amount has to be greater than 0")
    private @Setter @Getter double amount;

    @NotNull(message = "Direction is required")
    private @Setter @Getter Txn_Direction direction;

    @NotNull(message = "Balance should not be null")
    private @Setter @Getter double balance_after;
}
