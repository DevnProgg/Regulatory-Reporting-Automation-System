package com.wisetech.rras.model.snapshot;

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
@Getter
@Setter
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long snapshot_id;

    @NotNull(message = "Date should not be null")
    private Date date;

    @NotBlank(message = "Status should not be blank")
    private String status;

    private Date created_at;
}
