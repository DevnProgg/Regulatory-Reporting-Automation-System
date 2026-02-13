package com.wisetech.rras.repository;

import com.wisetech.rras.model.metrics.RegulatoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegulatoryMetricRepository extends JpaRepository<RegulatoryMetric, Long> {
    List<RegulatoryMetric> findBySnapshotId(int snapshotId);

    @Query("SELECT r FROM RegulatoryMetric r WHERE r.metricCode = :metricCode " +
            "ORDER BY r.snapshotId DESC")
    List<RegulatoryMetric> findByMetricCodeOrderBySnapshotIdDesc(String metricCode);
}
