package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponse;
import com.fibank.cashdesk.dto.CashOperationRequest;

import java.time.LocalDate;

public interface CashDeskService {
    void performOperation(CashOperationRequest request);

    CashBalanceResponse getCashBalance(Long cashierId, LocalDate dateFrom, LocalDate dateTo);
}
