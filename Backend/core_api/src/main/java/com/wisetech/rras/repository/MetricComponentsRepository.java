package com.wisetech.rras.repository;

import com.wisetech.rras.model.metrics.MetricComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricComponentsRepository  extends JpaRepository<MetricComponent, Long> {
    List<MetricComponent> findBySnapshotId(int snapshotId);
}
