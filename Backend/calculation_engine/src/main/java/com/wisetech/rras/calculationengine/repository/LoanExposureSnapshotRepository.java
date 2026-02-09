package com.wisetech.rras.calculationengine.repository;

import com.wisetech.rras.calculationengine.domain.LoanExposureSnapshot;
import com.wisetech.rras.calculationengine.domain.LoanExposureSnapshotId;
import com.wisetech.rras.calculationengine.enums.AssetClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanExposureSnapshotRepository extends
        JpaRepository<LoanExposureSnapshot, LoanExposureSnapshotId> {

    List<LoanExposureSnapshot> findBySnapshotId(Long snapshotId);

    @Query("SELECT l FROM LoanExposureSnapshot l WHERE l.snapshotId = :snapshotId " +
            "AND l.assetClass IN :classifications")
    List<LoanExposureSnapshot> findBySnapshotIdAndAssetClassIn(
            Long snapshotId,
            List<AssetClassification> classifications
    );

    @Query("SELECT SUM(l.outstandingBalance) FROM LoanExposureSnapshot l " +
            "WHERE l.snapshotId = :snapshotId")
    BigDecimal getTotalExposure(Long snapshotId);

    @Query("SELECT SUM(l.outstandingBalance) FROM LoanExposureSnapshot l " +
            "WHERE l.snapshotId = :snapshotId AND l.assetClass IN ('SUBSTANDARD', 'DOUBTFUL', 'LOSS')")
    BigDecimal getNonPerformingLoanAmount(Long snapshotId);
}