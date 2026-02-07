package com.wisetech.rras.model.cbs;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@AllArgsConstructor
@Setter
@Getter
public class Customers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long customer_id;

    @NotNull(message="Customer Category should not be null")
    @Enumerated(EnumType.STRING)
    private Customer_Category customer_category;

    @NotBlank(message = "Country should not be blank")
    private String country;

    @NotNull(message = "Risk Rating should not be null")
    private int risk_rating;

    private Date created_at;
}
