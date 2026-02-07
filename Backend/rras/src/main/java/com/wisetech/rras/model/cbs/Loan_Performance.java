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
@Setter
@Getter
public class Loan_Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long loan_id;

    private int days_past_due;

    @NotNull(message = "Date should not be null")
    private Date last_payment_date;
}
