package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.NotExistingAccountException;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, PersistedAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        PersistedAccount previousAccount = accounts.putIfAbsent(account.getAccountId(), new PersistedAccount(account));
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        PersistedAccount account = accounts.get(accountId);

        return account != null ? account.getAccount() : null;
    }

    @Override
    public void clearAccounts() {
        // this method should be properly synchronized: repository should stop create accounts, method should wait until all acquired locks are released
        // but as far as this method is supposed to be used only in tests, it's synchronization is omitted for simplicity

        accounts.clear();
    }

    @Override
    public Account lockAccount(String accountId) throws NotExistingAccountException {
        PersistedAccount account = accounts.get(accountId);
        if (account == null) {
            throw new NotExistingAccountException(accountId);
        }

        return account.lock();
    }

    @Override
    public void releaseAccount(String accountId) {
        PersistedAccount account = accounts.get(accountId);
        if (account != null) {
            account.unlock();
        }
    }

    @Override
    public Account updateAccount(Account account) {
        PersistedAccount persistedAccount = accounts.computeIfPresent(account.getAccountId(),
                (id, persistedAcc) -> persistedAcc.update(account));
        return persistedAccount != null ? persistedAccount.getAccount() : null;
    }

    private static class PersistedAccount {
        private Account account;
        private final Lock lock;

        PersistedAccount(Account account) {
            this.account = account.copy();
            this.lock = new ReentrantLock();
        }

        public PersistedAccount update(Account account) {
            this.account = account.copy();
            return this;
        }

        public Account getAccount() {
            return account.copy();
        }

        public Account lock() {
            lock.lock();
            return getAccount();
        }

        public void unlock() {
            lock.unlock();
        }
    }
}
