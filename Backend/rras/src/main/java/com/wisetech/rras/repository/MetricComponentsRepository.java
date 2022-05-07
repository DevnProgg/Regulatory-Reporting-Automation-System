package com.wisetech.rras.repository;

import com.wisetech.rras.model.metrics.MetricComponents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricComponentsRepository  extends JpaRepository<MetricComponents, Long> {
}
