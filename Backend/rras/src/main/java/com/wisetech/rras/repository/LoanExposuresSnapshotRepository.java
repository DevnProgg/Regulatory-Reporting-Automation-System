package com.wisetech.rras.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanExposuresSnapshotRepository extends JpaRepository<LoanExposuresSnapshotRepository, Long> {
}
