package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponseDTO;
import com.fibank.cashdesk.dto.CashOperationRequestDTO;

import java.time.LocalDate;

public interface CashDeskService {
    void performOperation(CashOperationRequestDTO request);

    CashBalanceResponseDTO getCashBalance(Long cashierId, LocalDate dateFrom, LocalDate dateTo);
}
