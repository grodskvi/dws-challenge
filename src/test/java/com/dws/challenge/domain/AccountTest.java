package com.dws.challenge.domain;

import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountChangeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    private static final String ACCOUNT_ID = "account-1";

    @Test
    void reportsWhetherFundsAreSufficient() {
        Account account = new Account(ACCOUNT_ID, amount("100"));

        assertThat(account.hasFunds(amount("0"))).isTrue();
        assertThat(account.hasFunds(amount("99.99"))).isTrue();
        assertThat(account.hasFunds(amount("100"))).isTrue();
        assertThat(account.hasFunds(amount("100.01"))).isFalse();
        assertThat(account.hasFunds(amount("200"))).isFalse();
    }

    @Test
    void failsOnNegativeAmountCredit() {
        Account account = new Account(ACCOUNT_ID, amount("100"));

        assertThatThrownBy(() -> account.credit(amount("-10")))
                .isInstanceOf(InvalidAccountChangeException.class);
    }

    @Test
    void creditsAccount() {
        Account account = new Account(ACCOUNT_ID, amount("100"));

        Account updatedAccount = account.credit(amount("0"));
        assertThat(updatedAccount).isEqualTo(new Account(ACCOUNT_ID, amount("100")));

        updatedAccount = account.credit(amount("50"));
        assertThat(updatedAccount).isEqualTo(new Account(ACCOUNT_ID, amount("150")));
    }

    @Test
    void failsOnNegativeAmountDebit() {
        Account account = new Account(ACCOUNT_ID, amount("100"));

        assertThatThrownBy(() -> account.debit(amount("-1")))
                .isInstanceOf(InvalidAccountChangeException.class);
    }

    @Test
    void failsDebitOnInsufficientFunds() {
        Account account = new Account(ACCOUNT_ID, amount("100"));

        assertThatThrownBy(() -> account.debit(amount("150")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void debitsAccount() throws InsufficientFundsException {
        Account account = new Account(ACCOUNT_ID, amount("100"));
        Account updatedAccount = account.debit(amount("30"));

        assertThat(updatedAccount).isEqualTo(new Account(ACCOUNT_ID, amount("70")));
    }

    private static BigDecimal amount(String amount) {
        return new BigDecimal(amount);
    }
}
