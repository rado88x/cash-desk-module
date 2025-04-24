package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponse;
import com.fibank.cashdesk.dto.CashOperationRequest;
import com.fibank.cashdesk.dto.DenominationDTO;
import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Denomination;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.CashierRepository;
import com.fibank.cashdesk.repository.DenominationRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CashDeskServiceImpl implements CashDeskService {
    private final CashierRepository cashierRepo;
    private final TransactionRepository txRepo;
    private final DenominationRepository denomRepo;

    public CashDeskServiceImpl(
            CashierRepository cashierRepo,
            TransactionRepository txRepo,
            DenominationRepository denomRepo
    ) {
        this.cashierRepo = cashierRepo;
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
    }

    @Override
    @Transactional
    public void performOperation(CashOperationRequest req) {
        double amount = req.getAmount();

        double denomTotal = req.getDenominations().stream()
                .mapToDouble(d -> d.getDenomination() * d.getCount())
                .sum();
        if (Double.compare(denomTotal, amount) != 0) {
            throw new IllegalArgumentException(
                    String.format("Sum of denominations (%.2f) does not match requested amount (%.2f)",
                            denomTotal, amount));
        }

        Cashier cashier = cashierRepo.findById(req.getCashierId())
                .orElseThrow(() -> new EntityNotFoundException("Cashier not found: " + req.getCashierId()));

        Currency curr = req.getCurrency();
        TransactionType type = req.getType();

        if (type == TransactionType.WITHDRAW) {
            double bal = (curr == Currency.BGN ? cashier.getBgnBalance() : cashier.getEurBalance());
            if (bal < amount) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            if (curr == Currency.BGN) cashier.setBgnBalance(bal - amount);
            else cashier.setEurBalance(cashier.getEurBalance() - amount);
        } else {
            if (curr == Currency.BGN) cashier.setBgnBalance(cashier.getBgnBalance() + amount);
            else cashier.setEurBalance(cashier.getEurBalance() + amount);
        }
        cashierRepo.save(cashier);

        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCurrency(curr);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        txRepo.save(tx);

        List<Denomination> existing = denomRepo
                .findByCashierUidAndCurrency(cashier.getUid(), curr);
        Map<Integer, Denomination> faceMap = existing.stream()
                .collect(Collectors.toMap(Denomination::getDenomination, Function.identity()));

        for (DenominationDTO d : req.getDenominations()) {
            int face = d.getDenomination();
            int cnt = d.getCount();
            Denomination denom = faceMap.get(face);

            if (type == TransactionType.DEPOSIT) {
                if (denom != null) {
                    denom.setCount(denom.getCount() + cnt);
                } else {
                    denom = new Denomination();
                    denom.setCurrency(curr);
                    denom.setDenomination(face);
                    denom.setCount(cnt);
                    denom.setCashier(cashier);
                    existing.add(denom);
                }
            } else { // WITHDRAW
                if (denom == null || denom.getCount() < cnt) {
                    throw new IllegalArgumentException(
                            "Insufficient notes of " + face + " " + curr);
                }
                denom.setCount(denom.getCount() - cnt);
            }
        }

        denomRepo.saveAll(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public CashBalanceResponse getCashBalance(Long cashierId, LocalDate from, LocalDate to) {
        List<Cashier> cashiers;
        if (cashierId != null) {
            Cashier c = cashierRepo.findById(cashierId)
                    .orElseThrow(() -> new EntityNotFoundException("Cashier not found: " + cashierId));
            cashiers = Collections.singletonList(c);
        } else {
            cashiers = cashierRepo.findAll();
        }

        Cashier c = cashiers.get(0);

        List<Transaction> txs = txRepo.findByCashierUidAndTimestampBetween(
                c.getUid(),
                from != null ? from : LocalDate.MIN,
                to != null ? to : LocalDate.MAX
        );

        CashBalanceResponse resp = new CashBalanceResponse();
        resp.setCashierId(c.getUid());
        resp.setCashierName(c.getName());

        Map<Currency, Double> balances = new EnumMap<>(Currency.class);
        balances.put(Currency.BGN, c.getBgnBalance());
        balances.put(Currency.EUR, c.getEurBalance());
        resp.setBalances(balances);

        Map<Currency, List<DenominationDTO>> denomMap = c.getDenominations().stream()
                .map(d -> new DenominationDTO(d.getCurrency(), d.getDenomination(), d.getCount()))
                .collect(Collectors.groupingBy(
                        DenominationDTO::getCurrency,
                        () -> new EnumMap<>(Currency.class),
                        Collectors.toList()
                ));
        resp.setDenominations(denomMap);

        return resp;
    }
}
