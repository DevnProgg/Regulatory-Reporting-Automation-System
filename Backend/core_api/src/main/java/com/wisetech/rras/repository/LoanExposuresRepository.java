package com.wisetech.rras.repository;

import com.wisetech.rras.model.source_read.Loan_Exposures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanExposuresRepository extends JpaRepository<Loan_Exposures, Long> {
}
