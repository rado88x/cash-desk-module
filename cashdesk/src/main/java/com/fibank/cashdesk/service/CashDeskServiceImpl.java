package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponse;
import com.fibank.cashdesk.dto.CashOperationRequest;
import com.fibank.cashdesk.dto.DenominationDTO;
import com.fibank.cashdesk.dto.TransactionDTO;
import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.CashierRepository;
import com.fibank.cashdesk.repository.DenominationRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        // 0) Validate denominations sum to the requested amount
        double denomTotal = req.getDenominations().stream()
                .mapToDouble(d -> d.getDenomination() * d.getCount())
                .sum();
        if (Double.compare(denomTotal, req.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Sum of denominations %.2f does not match requested amount %.2f",
                            denomTotal, req.getAmount()
                    )
            );
        }

        // 1) Load cashier
        Cashier cashier = cashierRepo.findById(req.getCashierId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cashier not found: " + req.getCashierId()));

        double amount = req.getAmount();
        Currency curr = req.getCurrency();
        TransactionType type = req.getType();

        // 2) Update numeric balance
        if (type == TransactionType.WITHDRAW) {
            double bal = (curr == Currency.BGN ? cashier.getBgnBalance()
                    : cashier.getEurBalance());
            if (bal < amount) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            if (curr == Currency.BGN) {
                cashier.setBgnBalance(bal - amount);
            } else {
                cashier.setEurBalance(bal - amount);
            }
        } else { // DEPOSIT
            if (curr == Currency.BGN) {
                cashier.setBgnBalance(cashier.getBgnBalance() + amount);
            } else {
                cashier.setEurBalance(cashier.getEurBalance() + amount);
            }
        }
        cashierRepo.save(cashier);

        // 3) Persist transaction record
        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCurrency(curr);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        txRepo.save(tx);

        // 4) Adjust denominations store
        List<com.fibank.cashdesk.model.Denomination> existing = denomRepo
                .findByCashierUidAndCurrency(cashier.getUid(), curr);

        Map<Integer, com.fibank.cashdesk.model.Denomination> faceMap =
                existing.stream()
                        .collect(Collectors.toMap(
                                com.fibank.cashdesk.model.Denomination::getDenomination,
                                Function.identity()
                        ));

        for (DenominationDTO d : req.getDenominations()) {
            int face = d.getDenomination();
            int cnt = d.getCount();
            com.fibank.cashdesk.model.Denomination denom = faceMap.get(face);

            if (type == TransactionType.DEPOSIT) {
                if (denom != null) {
                    denom.setCount(denom.getCount() + cnt);
                } else {
                    denom = new com.fibank.cashdesk.model.Denomination();
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
    public CashBalanceResponse getCashBalance(
            Long cashierId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        Cashier c = cashierRepo.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cashier not found: " + cashierId));

        List<Transaction> txs;
        if (dateFrom != null && dateTo != null) {
            txs = txRepo.findByCashierUidAndTimestampBetween(
                    c.getUid(), dateFrom, dateTo);
        } else if (dateFrom != null) {
            txs = txRepo.findByCashierUidAndTimestampAfter(
                    c.getUid(), dateFrom);
        } else if (dateTo != null) {
            txs = txRepo.findByCashierUidAndTimestampBefore(
                    c.getUid(), dateTo);
        } else {
            txs = txRepo.findByCashierUid(c.getUid());
        }

        // map txs → TransactionDTOs, build balances & denominations…
        CashBalanceResponse resp = new CashBalanceResponse();
        resp.setCashierId(c.getUid());
        resp.setCashierName(c.getName());
        // … (rest of your existing mapping code) …
        resp.setTransactions(
                txs.stream()
                        .map(t -> new TransactionDTO(t.getId(), t.getCurrency(),
                                t.getTransactionType(), t.getAmount(),
                                t.getTimestamp()))
                        .collect(Collectors.toList())
        );
        return resp;
    }
}