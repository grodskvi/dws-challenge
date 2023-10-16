package com.dws.challenge.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class TransferFailure {
    private final String accountFromId;
    private final String accountToId;
    private final BigDecimal amount;
    private final LocalDateTime time;
    private final String failureReason;

    public TransferFailure(TransferRequest request, LocalDateTime time, String failureReason) {
        accountFromId = request.getAccountFromId();
        accountToId = request.getAccountToId();
        amount = request.getAmount();
        this.time = time;
        this.failureReason = failureReason;
    }
}
