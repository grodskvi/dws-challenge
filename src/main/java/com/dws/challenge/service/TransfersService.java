package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.domain.TransferExecution;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidTransferException;
import com.dws.challenge.exception.NotExistingAccountException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class TransfersService {

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private NotificationService notificationService;

    public TransferExecution transfer(TransferRequest transferRequest) throws InvalidTransferException {
        log.debug("Handling transfer request {}", transferRequest);

        if (Objects.equals(transferRequest.getAccountFromId(), transferRequest.getAccountToId())) {
            log.info("Aborting transfer between same accounts");
            throw new InvalidTransferException("Same credit and debit accounts");
        }

        // ensure locks are acquired in same order
        boolean shouldLockDebitAccountFirst = isLessAccountId(
                transferRequest.getAccountFromId(),
                transferRequest.getAccountToId());

        Account debitAccount = null;
        Account creditAccount = null;

        try {
            if (shouldLockDebitAccountFirst) {
                debitAccount = accountsRepository.lockAccount(transferRequest.getAccountFromId());
                creditAccount = accountsRepository.lockAccount(transferRequest.getAccountToId());
            } else {
                creditAccount = accountsRepository.lockAccount(transferRequest.getAccountToId());
                debitAccount = accountsRepository.lockAccount(transferRequest.getAccountFromId());
            }

            if (!debitAccount.hasFunds(transferRequest.getAmount())) {
                log.info("Failed to execute transfer {} because of insufficient funds", transferRequest);
                throw new InvalidTransferException("Insufficient funds");
            }

            debitAccount = debitAccount.debit(transferRequest.getAmount());
            creditAccount = creditAccount.credit(transferRequest.getAmount());

            accountsRepository.updateAccount(debitAccount);
            accountsRepository.updateAccount(creditAccount);

            notificationService.notifyAboutTransfer(debitAccount,
                    String.format("%s was credited to %s", transferRequest.getAmount(), creditAccount.getAccountId()));
            notificationService.notifyAboutTransfer(creditAccount,
                    String.format("%s was debited from %s", transferRequest.getAmount(), debitAccount.getAccountId()));

            return new TransferExecution(transferRequest, LocalDateTime.now());
        } catch (InsufficientFundsException | NotExistingAccountException e) {
            log.info("Transfer {} failed: {}", transferRequest, e.toString());
            throw new InvalidTransferException("Invalid transfer: " + e.getMessage(), e);
        } finally {
            if (shouldLockDebitAccountFirst) {
                releaseTransferAccounts(creditAccount, debitAccount);
            } else {
                releaseTransferAccounts(debitAccount, creditAccount);
            }
        }

    }

    private void releaseTransferAccounts(Account account, Account otherAccount) {
        if (account != null) {
            accountsRepository.releaseAccount(account.getAccountId());
        }
        if (otherAccount != null) {
            accountsRepository.releaseAccount(otherAccount.getAccountId());
        }
    }

    private boolean isLessAccountId(String accountId, String otherAccountId) {
        return accountId.compareTo(otherAccountId) < 0;
    }
}
