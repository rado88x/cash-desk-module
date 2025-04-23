package com.fibank.cashdesk.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fibank.cashdesk.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "denominations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Denomination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_uid", nullable = false)
    @JsonBackReference           // pairs with @JsonManagedReference above
    private Cashier cashier;

    @Enumerated(EnumType.STRING)
    private Currency currency;
    private Integer denomination;
    private Integer count;

    public Denomination(Cashier cashier, Currency currency, Integer denomination, Integer count) {
        this.cashier = cashier;
        this.currency = currency;
        this.denomination = denomination;
        this.count = count;
    }
}
