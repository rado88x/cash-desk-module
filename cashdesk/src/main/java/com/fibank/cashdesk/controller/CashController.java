package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.CashBalanceRequestDTO;
import com.fibank.cashdesk.dto.CashBalanceResponseDTO;
import com.fibank.cashdesk.dto.CashOperationRequestDTO;
import com.fibank.cashdesk.service.CashDeskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CashController {

    private final CashDeskService cashDeskService;

    public CashController(CashDeskService cashDeskService) {
        this.cashDeskService = cashDeskService;
    }

    @PostMapping("/cash-operation")
    public ResponseEntity<CashBalanceResponseDTO> cashOperation(
            @RequestHeader("FIB-X-AUTH") String apiKey,
            @Valid @RequestBody CashOperationRequestDTO request
    ) {
        cashDeskService.performOperation(request);

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
}