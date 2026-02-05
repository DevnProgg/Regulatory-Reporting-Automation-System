package com.wisetech.rras.model.cbs;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
public class Loan_Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long loan_id;

    private @Setter @Getter int days_past_due;

    @NotNull(message = "Date should not be null")
    private @Setter @Getter Date last_payment_date;
}
