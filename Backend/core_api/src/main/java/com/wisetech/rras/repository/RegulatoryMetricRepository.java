package com.wisetech.rras.repository;

import com.wisetech.rras.model.metrics.RegulatoryMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegulatoryMetricRepository extends JpaRepository<RegulatoryMetrics, Long> {
}
