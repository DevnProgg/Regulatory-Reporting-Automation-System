package com.wisetech.rras.model.snapshot;

import com.wisetech.rras.model.auditing.AuditableEntity;
import jakarta.persistence.*;
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
@Table(name = "snapshot", schema = "snapshot", catalog = "RegulatoryReportingSystem")
public class Snapshot extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private long snapshotID;

    @NotNull(message = "Date should not be null")
    @Column(name = "date", nullable = false)
    private Date date;

    @NotBlank(message = "Status should not be blank")
    @Column(name = "status", nullable = false)
    private String status;

}
