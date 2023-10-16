package com.dws.challenge.exception;

import com.dws.challenge.domain.Account;

import java.math.BigDecimal;

public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(Account account, BigDecimal amount) {
        super(String.format("Account %s balance is less then %s", account, amount));
    }
}
