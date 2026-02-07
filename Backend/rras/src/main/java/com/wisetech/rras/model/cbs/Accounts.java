package com.wisetech.rras.model.cbs;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Entity
@AllArgsConstructor
@Getter
@Setter
public class Accounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long account_id;

    @NotNull(message = "customer id cannot be null")
    private long customer_id;

    @NotNull(message = "Account category is required")
    @Enumerated(EnumType.STRING)
    private Account_Category account_type;

    @NotBlank(message = "Currency must not be blank")
    private String currency;

    private Date opened_at;

    @NotBlank(message = "Status must not be blank")
    private String Status;


}

