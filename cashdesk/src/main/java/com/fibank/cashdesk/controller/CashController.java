package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.CashBalanceRequest;
import com.fibank.cashdesk.dto.CashBalanceResponse;
import com.fibank.cashdesk.dto.CashOperationRequest;
import com.fibank.cashdesk.service.CashDeskService;
import com.fibank.cashdesk.service.CashDeskServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class CashController {

    private final CashDeskService cashDeskService;

    public CashController(CashDeskService cashDeskService) {
        this.cashDeskService = cashDeskService;
    }

    @PostMapping("/cash-operation")
    public ResponseEntity<CashBalanceResponse> cashOperation(
            @RequestHeader("FIB-X-AUTH") String apiKey,
            @Valid @RequestBody CashOperationRequest request
    ) {
        // 1. perform the deposit/withdraw
        cashDeskService.performOperation(request);

        // 2. fetch the fresh balances
        CashBalanceResponse updated = cashDeskService.getCashBalance(
                request.getCashierId(),
                null,
                null
        );

        // 3. return 200 + body
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/cash-balance")
    public ResponseEntity<CashBalanceResponse> cashBalance(
            @RequestHeader("FIB-X-AUTH") String apiKey,
            @Valid @RequestBody CashBalanceRequest request
    ) {
        CashBalanceResponse resp = cashDeskService.getCashBalance(
                request.getCashierId(),
                request.getDateFrom(),
                request.getDateTo()
        );
        return ResponseEntity.ok(resp);
    }
}