package com.dws.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class TransferExecution {
    private final String accountFromId;
    private final String accountToId;
    private final BigDecimal amount;
    private final LocalDateTime time;


    public TransferExecution(TransferRequest request, LocalDateTime time) {
        accountFromId = request.getAccountFromId();
        accountToId = request.getAccountToId();
        amount = request.getAmount();
        this.time = time;
    }
}
