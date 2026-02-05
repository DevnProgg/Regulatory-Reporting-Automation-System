package com.wisetech.rras.model.cbs;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Entity
@AllArgsConstructor
public class Accounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long account_id;

    @NotNull(message = "customer id cannot be null")
    private @Setter @Getter long customer_id;

    @NotNull(message = "Account category is required")
    private @Setter @Getter Account_Category account_type;

    @NotBlank(message = "Currency must not be blank")
    private @Setter @Getter String currency;

    private @Getter @Setter Date opened_at;

    @NotBlank(message = "Status must not be blank")
    private @Setter @Getter String Status;


}

