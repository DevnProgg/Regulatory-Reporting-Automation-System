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

import java.util.Date;

@Entity
@AllArgsConstructor
public class Customers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Setter @Getter long customer_id;

    @NotNull(message="Customer Category should not be null")
    private @Getter @Setter Customer_Category customer_category;

    @NotBlank(message = "Country should not be blank")
    private @Getter @Setter String country;

    @NotNull(message = "Risk Rating should not be null")
    private @Setter @Getter int risk_rating;

    private @Getter @Setter Date created_at;
}
