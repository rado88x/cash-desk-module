package com.fibank.cashdesk.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cashiers")
@Data
@Getter
@Setter
@NoArgsConstructor
public class Cashier {
    @Id
    private Long uid;
    private String name;
    private Double bgnBalance;
    private Double eurBalance;
    @OneToMany(mappedBy = "cashier",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Transaction> transactions = new HashSet<>();
    @OneToMany(mappedBy = "cashier",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Denomination> denominations = new HashSet<>();

    public Cashier(Long uid, String name, Double bgnBalance, Double eurBalance, Set<Transaction> transactions) {
        this.uid = uid;
        this.name = name;
        this.bgnBalance = bgnBalance;
        this.eurBalance = eurBalance;
        this.transactions = transactions;
    }
}
