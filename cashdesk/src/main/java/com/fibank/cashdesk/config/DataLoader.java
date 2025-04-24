package com.fibank.cashdesk.config;

import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Denomination;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.CashierRepository;
import com.fibank.cashdesk.repository.DenominationRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Configuration
public class DataLoader {


    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    @Order(1)
    public CommandLineRunner cashiersInitializer(CashierRepository cashierRepository) {
        return args -> {
            if (cashierRepository.count() == 0) {
                List<Cashier> cashiers = List.of(
                        new Cashier(1001L, "MARTINA", 1000.0, 2000.0, new HashSet<>()),
                        new Cashier(1002L, "PETER", 1000.0, 2000.0, new HashSet<>()),
                        new Cashier(1003L, "LINDA", 1000.0, 2000.0, new HashSet<>())
                );
                cashierRepository.saveAll(cashiers);
                log.info("Cashiers were initialized...");
            }
        };
    }

    @Bean
    @Order(2)
    public CommandLineRunner transactionInitializer(TransactionRepository transactionRepository,
                                                    CashierRepository cashierRepository) {
        return args -> {
            List<Cashier> cashiers = cashierRepository.findAll();
            for (Cashier cashier : cashiers) {
                transactionRepository.saveAll(List.of(
                        new Transaction(cashier, Currency.BGN, 100.0, TransactionType.WITHDRAW, LocalDateTime.of(2003,10,5,11,25)),
                        new Transaction(cashier, Currency.BGN, 600.0, TransactionType.DEPOSIT, LocalDateTime.of(2005,8,5,11,25)),
                        new Transaction(cashier, Currency.EUR, 500.0, TransactionType.WITHDRAW, LocalDateTime.of(2017,2,1,1,25)),
                        new Transaction(cashier, Currency.EUR, 200.0, TransactionType.DEPOSIT, LocalDateTime.of(2019,7,6,1,25))
                ));
            }
            log.info("Transactions were initialized...");
        };
    }

    @Bean
    @Order(3)
    public CommandLineRunner denominationInitializer(DenominationRepository denomRepo,
                                                     CashierRepository cashierRepo) {
        return args -> {
            List<Cashier> cashiers = cashierRepo.findAll();
            for (Cashier cashier : cashiers) {
                // BGN: 10 x 50, 50 x 10
                List<Denomination> bgdDenoms = List.of(
                        new Denomination(cashier, Currency.BGN, 50, 10),
                        new Denomination(cashier, Currency.BGN, 10, 50)
                );
                // EUR: 10 x 100, 50 x 20
                List<Denomination> eurDenoms = List.of(
                        new Denomination(cashier, Currency.EUR, 100, 10),
                        new Denomination(cashier, Currency.EUR, 20, 50)
                );
                denomRepo.saveAll(bgdDenoms);
                denomRepo.saveAll(eurDenoms);
            }
            log.info("Denominations were initialized...");
        };
    }
}
