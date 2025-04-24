package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCashierUid(Long cashierUid);

    @Query("""
            SELECT t
              FROM Transaction t
             WHERE t.cashier.uid = :cashierUid
               AND CAST(t.timestamp AS date) BETWEEN :from AND :to
            """)
    List<Transaction> findByCashierUidAndTimestampBetween(
            @Param("cashierUid") Long cashierUid,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

//    List<Transaction> findByCashierUidAndTimestampBetween(
//            Long cashierUid, LocalDate from, LocalDate to);
//
//    List<Transaction> findByCashierUidAndTimestampAfter(
//            Long cashierUid, LocalDate from);
//
//    List<Transaction> findByCashierUidAndTimestampBefore(
//            Long cashierUid, LocalDate to);
}


