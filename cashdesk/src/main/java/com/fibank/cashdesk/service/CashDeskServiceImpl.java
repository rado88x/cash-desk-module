package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.CashBalanceResponseDTO;
import com.fibank.cashdesk.dto.CashOperationRequestDTO;
import com.fibank.cashdesk.dto.DenominationDTO;
import com.fibank.cashdesk.dto.TransactionDTO;
import com.fibank.cashdesk.enums.Currency;
import com.fibank.cashdesk.enums.TransactionType;
import com.fibank.cashdesk.exception.CashierNotFoundException;
import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.model.Cashier;
import com.fibank.cashdesk.model.Denomination;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.CashierRepository;
import com.fibank.cashdesk.repository.DenominationRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CashDeskServiceImpl.class);
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
    public void performOperation(CashOperationRequestDTO req) {
        // 0) Validate that denominations sum to the requested amount
        double sumOfNotes = req.getDenominations().stream()
                .mapToDouble(d -> d.getDenomination() * d.getCount())
                .sum();
        if (Double.compare(sumOfNotes, req.getAmount()) != 0) {
            log.info("Transaction failed.");
            throw new IllegalArgumentException(
                    String.format(
                            "Sum of denominations %.2f does not match requested amount %.2f",
                            sumOfNotes, req.getAmount()
                    )
            );

        }

        // 1) Load the cashier
        Cashier cashier = cashierRepo.findById(req.getCashierId())
                .orElseThrow(() -> {
                    log.error("Cashier not found: {}", req.getCashierId());
                    return new CashierNotFoundException("Cashier not found: " + req.getCashierId());
                });

        double amount = req.getAmount();
        Currency curr = req.getCurrency();
        TransactionType type = req.getType();

        // 2) Update balance
        if (type == TransactionType.WITHDRAW) {
            double available = (curr == Currency.BGN)
                    ? cashier.getBgnBalance()
                    : cashier.getEurBalance();
            if (available < amount) {
                log.error("Insufficient funds.");
                throw new InsufficientFundsException("Insufficient funds");
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
        log.info("Balance state of cashier with id: {} was updated.", cashier.getUid());

        // 3) Persist the transaction record
        Transaction tx = new Transaction();
        tx.setCashier(cashier);
        tx.setCurrency(curr);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTimestamp(java.time.LocalDateTime.now());
        txRepo.save(tx);
        log.info("Transaction of cashier with id: {} was completed.", cashier.getUid());

        // 4) Adjust denominations
        List<Denomination> existing = denomRepo
                .findByCashierUidAndCurrency(cashier.getUid(), curr);

        // Map Denomination
        Map<Integer, Denomination> denominationMap = existing.stream()
                .collect(Collectors.toMap(Denomination::getDenomination,
                        Function.identity()));

        for (DenominationDTO incoming : req.getDenominations()) {
            int face = incoming.getDenomination();
            int count = incoming.getCount();
            Denomination denom = denominationMap.get(face);

            if (type == TransactionType.DEPOSIT) {
                if (denom != null) {
                    denom.setCount(denom.getCount() + count);
                } else {
                    // Brand new note entry
                    denom = new Denomination();
                    denom.setCashier(cashier);
                    denom.setCurrency(curr);
                    denom.setDenomination(face);
                    denom.setCount(count);
                    existing.add(denom);
                }
            } else { // WITHDRAW
                if (denom == null || denom.getCount() < count) {
                    log.error(" Insufficient notes of {}, {}.", face, curr);
                    throw new InsufficientFundsException(
                            "Insufficient notes of " + face + " " + curr);
                }
                denom.setCount(denom.getCount() - count);
            }
        }

        // Persist all updates to denominations
        denomRepo.saveAll(existing);
        log.info("Denomination set of cashier with id: {} was updated.", cashier.getUid());
    }

    @Override
    @Transactional(readOnly = true)
    public CashBalanceResponseDTO getCashBalance(
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

        // 6) Prepare response and return
        CashBalanceResponseDTO resp = new CashBalanceResponseDTO();
        resp.setCashierId(c.getUid());
        resp.setCashierName(c.getName());
        resp.setBalances(balances);
        resp.setDenominations(denomMap);
        resp.setTransactions(txDtos);

        return resp;
    }
}