package com.fibank.cashdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CashBalanceRequestDTO {

    /**
     * Optional — if omitted, all cashiers are returned (or you can handle it appropriately in service).
     */
    private Long cashierId;

    /**
     * Optional start date (inclusive).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "dateFrom cannot be in the future")
    private LocalDate dateFrom;

    /**
     * Optional end date (inclusive).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "dateTo cannot be in the future")
    private LocalDate dateTo;


}