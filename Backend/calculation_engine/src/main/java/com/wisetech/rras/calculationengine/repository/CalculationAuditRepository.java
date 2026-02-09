package com.wisetech.rras.calculationengine.repository;
import com.wisetech.rras.calculationengine.domain.CalculationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalculationAuditRepository extends JpaRepository<CalculationAudit, Long> {

    List<CalculationAudit> findBySnapshotIdOrderByExecutedAtAsc(Long snapshotId);

    List<CalculationAudit> findBySnapshotIdAndCalculationStep(
            Long snapshotId,
            String calculationStep
    );
}