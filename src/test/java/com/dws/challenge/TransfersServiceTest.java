package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.domain.TransferExecution;
import com.dws.challenge.exception.InvalidTransferException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.TransfersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TransfersServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TransfersServiceTest.class);

    private static final String ACCOUNT_1_ID = "account-1";
    private static final String ACCOUNT_2_ID = "account-2";

    @Autowired
    private TransfersService transfersService;

    @Autowired
    private AccountsRepository accountsRepository;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void beforeEach() {
        accountsRepository.clearAccounts();
        accountsRepository.createAccount(new Account(ACCOUNT_1_ID, new BigDecimal(100)));
        accountsRepository.createAccount(new Account(ACCOUNT_2_ID, new BigDecimal(20)));
    }

    @Test
    void transfersFundsBetweenAccounts() throws InvalidTransferException {
        BigDecimal transferAmount = new BigDecimal(70);
        TransferRequest transferRequest = TransferRequest.builder()
                .accountFromId(ACCOUNT_1_ID)
                .accountToId(ACCOUNT_2_ID)
                .amount(transferAmount)
                .build();

        TransferExecution transferExecution = transfersService.transfer(transferRequest);
        assertThat(transferExecution).usingRecursiveComparison()
                .ignoringFields("time")
                .isEqualTo(new TransferExecution(ACCOUNT_1_ID, ACCOUNT_2_ID, transferAmount, null));

        Account updated1 = new Account(ACCOUNT_1_ID, new BigDecimal(30));
        Account updated2 = new Account(ACCOUNT_2_ID, new BigDecimal(90));
        assertThat(accountsRepository.getAccount(ACCOUNT_1_ID)).isEqualTo(updated1);
        assertThat(accountsRepository.getAccount(ACCOUNT_2_ID)).isEqualTo(updated2);

        verify(notificationService).notifyAboutTransfer(updated1, "70 was credited to account-2");
        verify(notificationService).notifyAboutTransfer(updated2, "70 was debited from account-1");
    }

    @Test
    void failsToTransferFundsIfInsufficientAmount() {
        BigDecimal transferAmount = new BigDecimal(200);
        TransferRequest transferRequest = TransferRequest.builder()
                .accountFromId(ACCOUNT_1_ID)
                .accountToId(ACCOUNT_2_ID)
                .amount(transferAmount)
                .build();

        assertThatThrownBy(() -> transfersService.transfer(transferRequest))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Insufficient funds");

        assertThat(accountsRepository.getAccount(ACCOUNT_1_ID))
                .isEqualTo(new Account(ACCOUNT_1_ID, new BigDecimal(100)));
        assertThat(accountsRepository.getAccount(ACCOUNT_2_ID))
                .isEqualTo(new Account(ACCOUNT_2_ID, new BigDecimal(20)));

        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void failsToCreditUnknownAccount() {
        BigDecimal transferAmount = new BigDecimal(200);
        TransferRequest transferRequest = TransferRequest.builder()
                .accountFromId(ACCOUNT_1_ID)
                .accountToId("unknown-account")
                .amount(transferAmount)
                .build();

        assertThatThrownBy(() -> transfersService.transfer(transferRequest))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Invalid transfer: Account unknown-account does not exist");;

        assertThat(accountsRepository.getAccount(ACCOUNT_1_ID))
                .isEqualTo(new Account(ACCOUNT_1_ID, new BigDecimal(100)));

        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void failsToDebitUnknownAccount() {
        BigDecimal transferAmount = new BigDecimal(200);
        TransferRequest transferRequest = TransferRequest.builder()
                .accountFromId("unknown-account")
                .accountToId(ACCOUNT_2_ID)
                .amount(transferAmount)
                .build();

        assertThatThrownBy(() -> transfersService.transfer(transferRequest))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Invalid transfer: Account unknown-account does not exist");

        assertThat(accountsRepository.getAccount(ACCOUNT_2_ID))
                .isEqualTo(new Account(ACCOUNT_2_ID, new BigDecimal(20)));

        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void failsToTransferBetweenSameAccounts() {
        BigDecimal transferAmount = new BigDecimal(200);
        TransferRequest transferRequest = TransferRequest.builder()
                .accountFromId(ACCOUNT_1_ID)
                .accountToId(ACCOUNT_1_ID)
                .amount(transferAmount)
                .build();

        assertThatThrownBy(() -> transfersService.transfer(transferRequest))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Same credit and debit accounts");

        assertThat(accountsRepository.getAccount(ACCOUNT_1_ID))
                .isEqualTo(new Account(ACCOUNT_1_ID, new BigDecimal(100)));

        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void consistentlyTransfersFundsBetweenAccounts() {
        accountsRepository.clearAccounts();

        int accountsCount = 3;
        int transfersCount = 10_000;
        BigDecimal initialDeposit = new BigDecimal(3000);

        for (int i = 0; i < accountsCount; i++) {
            accountsRepository.createAccount(new Account("account-" + i, initialDeposit));
        }


        Random random = new Random();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch countDownLatch = new CountDownLatch(transfersCount);

        List<Future<TransferExecution>> transferResults = new ArrayList<>();
        for (int i = 0; i < transfersCount; i++) {
            TransferRequest transferRequest = randomTransferRequest(random, accountsCount, 100);
            int transferId = i;

            Future<TransferExecution> result = executorService.submit(() -> {
                countDownLatch.await();
                try {
                    return transfersService.transfer(transferRequest);
                } catch (InvalidTransferException e) {
                    log.info("Failed to execute transfer [{}] {}: {}", transferId, transferRequest, e.toString());
                    return null;
                }
            });

            countDownLatch.countDown();
            transferResults.add(result);
        }

        transferResults.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < accountsCount; i++) {
            Account account = accountsRepository.getAccount("account-" + i);
            total = total.add(account.getBalance());
        }

        assertThat(total).isEqualTo(initialDeposit.multiply(new BigDecimal(accountsCount)));
    }

    private TransferRequest randomTransferRequest(Random random, int accountsCount, int maxAmount) {
        int debitAccountId = random.nextInt(accountsCount);
        int creditAccountId;
        do {
            creditAccountId = random.nextInt(accountsCount);
        } while (debitAccountId == creditAccountId);

        int amount = random.nextInt(maxAmount);

        return TransferRequest.builder()
                .accountFromId("account-" + debitAccountId)
                .accountToId("account-" + creditAccountId)
                .amount(new BigDecimal(amount))
                .build();
    }
}
