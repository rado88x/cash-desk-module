package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.CashBalanceRequestDTO;
import com.fibank.cashdesk.dto.CashBalanceResponseDTO;
import com.fibank.cashdesk.dto.CashOperationRequestDTO;
import com.fibank.cashdesk.dto.DenominationDTO;
import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.exception.CashierNotFoundException;
import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.CashierRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.CashDeskService;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CashController {
    private static final Logger log = LoggerFactory.getLogger(CashController.class);
    private final CashDeskService cashDeskService;
    private final TransactionRepository transactionRepo;
    private final CashierRepository cashierRepo;


    public CashController(CashDeskService cashDeskService, TransactionRepository transactionRepo, CashierRepository cashierRepo) {
        this.cashDeskService = cashDeskService;
        this.transactionRepo = transactionRepo;
        this.cashierRepo = cashierRepo;
    }

    @PostMapping("/cash-operation")
    public ResponseEntity<CashBalanceResponseDTO> cashOperation(
            @RequestHeader("FIB-X-AUTH") String apiKey,
            @Valid @RequestBody CashOperationRequestDTO request
    ) throws Exception {
        try {
            cashDeskService.performOperation(request);
        } catch (InsufficientFundsException | CashierNotFoundException e) {
            log.error("Transaction failed.");
            throw new IllegalArgumentException("Transaction failed. " + e.getMessage());
        }

        CashBalanceResponseDTO updated = cashDeskService.getCashBalance(
                request.getCashierId(),
                null,
                null
        );

        // 3. return 200 + body
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/cash-balance")
    public ResponseEntity<CashBalanceResponseDTO> cashBalance(
            @RequestHeader("FIB-X-AUTH") String apiKey,
            @Valid @RequestBody CashBalanceRequestDTO request
    ) {
        CashBalanceResponseDTO resp = cashDeskService.getCashBalance(
                request.getCashierId(),
                request.getDateFrom(),
                request.getDateTo()
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/transactions/export")
    public ResponseEntity<Resource> exportTransactions(@PathVariable("id") Long cashierId) {
        List<Transaction> txns = transactionRepo.findByCashierUid(cashierId);
        if (txns.isEmpty()) {
            log.warn("No transactions found for cashier {}", cashierId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String filename = "transactions_" + cashierId + "_" + System.currentTimeMillis() + ".txt";
        Path exportsDir = Paths.get("exports");
        Path file = exportsDir.resolve(filename);
        log.info("Path name: {}", exportsDir.toString());

        try {
            Files.createDirectories(exportsDir);
            log.info("Directory was created.");

            // write each transaction on its own line
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                for (Transaction t : txns) {
                    writer.write(formatTransaction(t));
                    writer.newLine();

                }
                log.info("Transaction file for cashier {} were created.", cashierId);
            }

            Resource resource = new PathResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to export transactions for cashier {}: {}", cashierId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/balances/export")
    public ResponseEntity<Resource> exportBalancesAndDenoms() {
        List<Cashier> cashiers = cashierRepo.findAll();
        if (cashiers.isEmpty()) {
            log.warn("No cashiers found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String filename = "cash_balances_denominations_" + System.currentTimeMillis() + ".txt";
        Path exportsDir = Paths.get("exports");
        Path file = exportsDir.resolve(filename);

        try {
            Files.createDirectories(exportsDir);
            log.info("Directory name: {}", exportsDir.toAbsolutePath());

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                for (Cashier cashier : cashiers) {
                    CashBalanceResponseDTO dto = cashDeskService.getCashBalance(
                            cashier.getUid(), null, null);

                    writer.write("Cashier ID: " + cashier.getUid());
                    writer.newLine();
                    writer.write("Balance: " + dto.getBalances());
                    writer.newLine();
                    writer.write("Denominations:");
                    writer.newLine();
                    for (Map.Entry<Currency, List<DenominationDTO>> entry : dto.getDenominations().entrySet()) {
                        writer.write("  " + entry.getKey() + " x " + entry.getValue());
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }

            Resource resource = new PathResource(file.toAbsolutePath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(Files.size(file))
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to export balances/denoms: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private String formatTransaction(Transaction t) {
        return String.format("Transaction[id=%d, amount=%s, date=%s]",
                t.getId(),
                t.getAmount(),
                t.getTimestamp());

    }
}