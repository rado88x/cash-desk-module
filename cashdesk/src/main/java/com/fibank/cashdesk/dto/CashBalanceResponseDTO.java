package com.fibank.cashdesk.dto;

import com.fibank.cashdesk.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class CashBalanceResponseDTO {

    private Long cashierId;
    private String cashierName;
    private Map<Currency, Double> balances;
    private Map<Currency, List<DenominationDTO>> denominations;
    private List<TransactionDTO> transactions;

}