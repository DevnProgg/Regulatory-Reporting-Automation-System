package com.wisetech.rras.calculationengine.repository;

import com.wisetech.rras.calculationengine.domain.RegulatoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegulatoryMetricRepository extends JpaRepository<RegulatoryMetric, Long> {

    List<RegulatoryMetric> findBySnapshotId(int snapshotId);

    Optional<RegulatoryMetric> findBySnapshotIdAndMetricCode(int snapshotId, String metricCode);

    @Query("SELECT r FROM RegulatoryMetric r WHERE r.metricCode = :metricCode " +
            "ORDER BY r.snapshotId DESC")
    List<RegulatoryMetric> findByMetricCodeOrderBySnapshotIdDesc(String metricCode);
}