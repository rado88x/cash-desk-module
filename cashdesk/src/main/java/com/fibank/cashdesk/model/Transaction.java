package com.fibank.cashdesk.model;

import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_uid")
    private Cashier cashier;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private Double amount;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private LocalDateTime timestamp;


    public Transaction(Cashier cashier, Currency currency, Double amount, TransactionType transactionType, LocalDateTime timestamp) {
        this.cashier = cashier;
        this.currency = currency;
        this.amount = amount;
        this.transactionType = transactionType;
        this.timestamp = timestamp;
    }
}
