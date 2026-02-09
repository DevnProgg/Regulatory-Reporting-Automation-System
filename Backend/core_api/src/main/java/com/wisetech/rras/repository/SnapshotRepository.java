package com.wisetech.rras.repository;

import com.wisetech.rras.model.snapshot.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
}
