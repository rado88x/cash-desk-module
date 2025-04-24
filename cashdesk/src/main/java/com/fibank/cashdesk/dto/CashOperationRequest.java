package com.fibank.cashdesk.dto;

import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CashOperationRequest {

    @NotNull
    private Long cashierId;

    @NotNull
    private Currency currency;

    @NotNull
    private TransactionType type;


    private double amount;

    @NotEmpty(message = "At least one denomination must be specified")
    private List<DenominationDTO> denominations;
}
