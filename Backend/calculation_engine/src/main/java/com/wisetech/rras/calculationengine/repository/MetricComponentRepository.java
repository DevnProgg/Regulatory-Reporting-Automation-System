package com.wisetech.rras.calculationengine.repository;

import com.wisetech.rras.calculationengine.domain.MetricComponent;
import com.wisetech.rras.calculationengine.domain.MetricComponentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MetricComponentRepository extends
        JpaRepository<MetricComponent, MetricComponentId> {

    List<MetricComponent> findBySnapshotId(Long snapshotId);

    @Query("SELECT SUM(m.rwaValue) FROM MetricComponent m WHERE m.snapshotId = :snapshotId")
    BigDecimal getTotalRWA(Long snapshotId);

    @Query("SELECT SUM(m.eclAmount) FROM MetricComponent m WHERE m.snapshotId = :snapshotId")
    BigDecimal getTotalECL(Long snapshotId);
}