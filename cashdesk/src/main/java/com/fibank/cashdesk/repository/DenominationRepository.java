package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.model.Denomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DenominationRepository extends JpaRepository<Denomination, Long> {

}
