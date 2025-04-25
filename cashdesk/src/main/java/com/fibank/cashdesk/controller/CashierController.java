package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Denomination;
import com.fibank.cashdesk.repository.CashierRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/cashier")
public class CashierController {
    private final CashierRepository repository;

    public CashierController(CashierRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)

    public ResponseEntity<Cashier> getCashier(@RequestHeader("FIB-X-AUTH") String apiKey, @PathVariable Long id) {
        return repository.findById(id)
                .map(cashier -> {
                    cashier.getDenominations().size();
                    cashier.getTransactions().size();

                    cashier.setDenominations(new HashSet<>(cashier.getDenominations()));
                    cashier.setTransactions(new HashSet<>());

                    return ResponseEntity.ok(cashier);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}