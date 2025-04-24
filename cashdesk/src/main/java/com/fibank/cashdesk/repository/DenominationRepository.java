package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.model.Denomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenominationRepository extends JpaRepository<Denomination, Long> {

    List<Denomination> findByCashierUidAndCurrency(Long cashierUid, Currency currency);


}
