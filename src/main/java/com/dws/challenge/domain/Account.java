package com.dws.challenge.domain;

import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidAccountChangeException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  @Min(value = 0, message = "Initial balance must be positive.")
  private BigDecimal balance;

  public Account(String accountId) {
    this.accountId = accountId;
    this.balance = BigDecimal.ZERO;
  }

  @JsonCreator
  public Account(@JsonProperty("accountId") String accountId,
    @JsonProperty("balance") BigDecimal balance) {
    this.accountId = accountId;
    this.balance = balance;
  }

  public Account copy() {
    return new Account(accountId, balance);
  }

  public boolean hasFunds(BigDecimal amount) {
    return balance.compareTo(amount) >= 0;
  }

  public Account credit(BigDecimal amount) {
    if (BigDecimal.ZERO.compareTo(amount) > 0) {
      throw new InvalidAccountChangeException("Can not credit negative amount " + amount);
    }

    return new Account(accountId, balance.add(amount));
  }

  public Account debit(BigDecimal amount) throws InsufficientFundsException {
    if (BigDecimal.ZERO.compareTo(amount) > 0) {
      throw new InvalidAccountChangeException("Can not debit negative amount " + amount);
    }

    if (hasFunds(amount)) {
      return new Account(accountId, balance.subtract(amount));
    } else {
      throw new InsufficientFundsException(this, amount);
    }
  }
}
