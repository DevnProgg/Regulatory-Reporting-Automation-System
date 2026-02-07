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
@Setter
@Getter
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long txn_id;

    @NotNull(message = "Account id cannot be null")
    private long account_id;

    @NotNull(message = "Transaction Date cannot be null")
    private Date txn_date;

    @DecimalMin(value = "0.01", message = "Amount has to be greater than 0")
    private double amount;

    @NotNull(message = "Direction is required")
    private Txn_Direction direction;

    @NotNull(message = "Balance should not be null")
    private double balance_after;
}
