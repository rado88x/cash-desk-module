package com.fibank.cashdesk.dto;

import com.fibank.cashdesk.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DenominationDTO {

    private Currency currency;
    private int denomination;
    private int count;
}
