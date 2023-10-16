package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.NotExistingAccountException;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Account getAccount(String accountId);

  void clearAccounts();

  Account lockAccount(String accountId) throws NotExistingAccountException;

  void releaseAccount(String accountId);

  Account updateAccount(Account account);
}
