package com.wisetech.rras.calculationengine.repository;

import com.wisetech.rras.calculationengine.domain.SnapshotRun;
import com.wisetech.rras.calculationengine.enums.CalculationType;
import com.wisetech.rras.calculationengine.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SnapshotRunRepository extends JpaRepository<SnapshotRun, Long> {

    Optional<SnapshotRun> findBySnapshotDateAndCalculationType(
            LocalDate snapshotDate,
            CalculationType calculationType
    );

    List<SnapshotRun> findByStatus(RunStatus status);

    @Query("SELECT s FROM SnapshotRun s WHERE s.status = 'CALCULATED' " +
            "AND s.snapshotDate <= :date ORDER BY s.snapshotDate DESC")
    List<SnapshotRun> findCalculatedSnapshotsUpTo(LocalDate date);

    @Query("SELECT s FROM SnapshotRun s WHERE s.calculationType = :type " +
            "ORDER BY s.snapshotDate DESC")
    List<SnapshotRun> findByCalculationTypeOrderByDateDesc(CalculationType type);
}