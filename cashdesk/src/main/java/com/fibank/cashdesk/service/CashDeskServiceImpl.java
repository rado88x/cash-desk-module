package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponse;
import com.fibank.cashdesk.dto.CashOperationRequest;
import com.fibank.cashdesk.dto.DenominationDTO;
import com.fibank.cashdesk.dto.TransactionDTO;
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
        // 0) Validate that denominations sum to the requested amount
        double sumOfNotes = req.getDenominations().stream()
                .mapToDouble(d -> d.getDenomination() * d.getCount())
                .sum();
        if (Double.compare(sumOfNotes, req.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Sum of denominations %.2f does not match requested amount %.2f",
                            sumOfNotes, req.getAmount()
                    )
            );
        }

        // 1) Load the cashier
        Cashier cashier = cashierRepo.findById(req.getCashierId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cashier not found: " + req.getCashierId()));

        double amount = req.getAmount();
        Currency curr = req.getCurrency();
        TransactionType type = req.getType();

        // 2) Update numeric balance
        if (type == TransactionType.WITHDRAW) {
            double available = (curr == Currency.BGN)
                    ? cashier.getBgnBalance()
                    : cashier.getEurBalance();
            if (available < amount) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            if (curr == Currency.BGN) {
                cashier.setBgnBalance(available - amount);
            } else {
                cashier.setEurBalance(available - amount);
            }
        } else { // DEPOSIT
            if (curr == Currency.BGN) {
                cashier.setBgnBalance(cashier.getBgnBalance() + amount);
            } else {
                cashier.setEurBalance(cashier.getEurBalance() + amount);
            }
        }
        cashierRepo.save(cashier);

        // 3) Persist the transaction record
        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCurrency(curr);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTimestamp(java.time.LocalDateTime.now());
        txRepo.save(tx);

        // 4) Adjust denominations
        List<Denomination> existing = denomRepo
                .findByCashierUidAndCurrency(cashier.getUid(), curr);

        // Map face‐value → Denomination
        Map<Integer, Denomination> faceMap = existing.stream()
                .collect(Collectors.toMap(Denomination::getDenomination,
                        Function.identity()));

        for (DenominationDTO incoming : req.getDenominations()) {
            int face = incoming.getDenomination();
            int cnt = incoming.getCount();
            Denomination denom = faceMap.get(face);

            if (type == TransactionType.DEPOSIT) {
                if (denom != null) {
                    denom.setCount(denom.getCount() + cnt);
                } else {
                    // Brand new note entry
                    denom = new Denomination();
                    denom.setCashier(cashier);
                    denom.setCurrency(curr);
                    denom.setDenomination(face);
                    denom.setCount(cnt);
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

        // Persist all updates to denominations
        denomRepo.saveAll(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public CashBalanceResponse getCashBalance(
            Long cashierId,
            LocalDate from,
            LocalDate to
    ) {

        Cashier c = cashierRepo.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cashier not found: " + cashierId));


        List<Transaction> txs;
        if (from == null && to == null) {
            // no date filters → return *all* transactions
            txs = txRepo.findByCashierUid(c.getUid());
        } else {
            // apply a real BETWEEN on LocalDate
            LocalDate start = (from != null ? from : LocalDate.MIN);
            LocalDate end = (to != null ? to : LocalDate.MAX);
            txs = txRepo.findByCashierUidAndTimestampBetween(
                    c.getUid(), start, end);
        }


        List<TransactionDTO> txDtos = txs.stream()
                .map(t -> new TransactionDTO(
                        t.getId(),
                        t.getCurrency(),
                        t.getTransactionType(),
                        t.getAmount(),
                        t.getTimestamp()
                ))
                .collect(Collectors.toList());


        Map<Currency, Double> balances = new EnumMap<>(Currency.class);
        balances.put(Currency.BGN, c.getBgnBalance());
        balances.put(Currency.EUR, c.getEurBalance());


        Map<Currency, List<DenominationDTO>> denomMap = c.getDenominations().stream()
                .map(d -> new DenominationDTO(
                        d.getCurrency(),
                        d.getDenomination(),
                        d.getCount()
                ))
                .collect(Collectors.groupingBy(
                        DenominationDTO::getCurrency,
                        () -> new EnumMap<>(Currency.class),
                        Collectors.toList()
                ));

        // 6) Assemble and return
        CashBalanceResponse resp = new CashBalanceResponse();
        resp.setCashierId(c.getUid());
        resp.setCashierName(c.getName());
        resp.setBalances(balances);
        resp.setDenominations(denomMap);
        resp.setTransactions(txDtos);

        return resp;
    }
}