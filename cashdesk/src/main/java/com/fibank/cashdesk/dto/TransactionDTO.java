package com.fibank.cashdesk.dto;

import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionDTO {
    private Long id;
    private Currency currency;
    private TransactionType type;
    private double amount;
    private LocalDateTime timestamp;

}