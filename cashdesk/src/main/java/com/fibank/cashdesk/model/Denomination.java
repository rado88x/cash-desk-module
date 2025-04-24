package com.fibank.cashdesk.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fibank.cashdesk.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "denominations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Denomination {

    // to keep id of transaction makes great sense
    // if we want to follow money source especially for fake denominations
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Denomination)) return false;
        Denomination other = (Denomination) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
