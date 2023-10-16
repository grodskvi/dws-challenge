package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
public class TransfersControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsRepository.clearAccounts();
    }

    @Test
    void transfersFundsBetweenAccounts() throws Exception {
        accountsRepository.createAccount(new Account("account-1", new BigDecimal(100)));
        accountsRepository.createAccount(new Account("account-2", new BigDecimal(0)));

        MvcResult mvcResult = this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "account-2", 45)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString())
                .contains("\"accountFromId\":\"account-1\"")
                .contains("\"accountToId\":\"account-2\"")
                .contains("\"amount\":45");

        assertThat(accountsRepository.getAccount("account-1"))
                .isEqualTo(new Account("account-1", new BigDecimal(55)));
        assertThat(accountsRepository.getAccount("account-2"))
                .isEqualTo(new Account("account-2", new BigDecimal(45)));
    }

    @Test
    void doesNotTransferFundsIfInsufficientAmount() throws Exception {
        accountsRepository.createAccount(new Account("account-1", new BigDecimal(100)));
        accountsRepository.createAccount(new Account("account-2", new BigDecimal(0)));

        MvcResult mvcResult = this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "account-2", 150)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString())
                .contains("\"failureReason\":\"Insufficient funds\"");

        assertThat(accountsRepository.getAccount("account-1"))
                .isEqualTo(new Account("account-1", new BigDecimal(100)));
        assertThat(accountsRepository.getAccount("account-2"))
                .isEqualTo(new Account("account-2", new BigDecimal(0)));
    }

    @Test
    void failsOnNullAccountFrom() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest(null, "account-2", 150)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnEmptyAccountFrom() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("", "account-2", 150)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnNullAccountTo() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", null, 150)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnEmptyAccountTo() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "", 150)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnNegativeTransferAmount() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "account-2", -1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnZeroTransferAmount() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "account-2", 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failsOnNullTransferAmount() throws Exception {
        this.mockMvc
                .perform(
                        post("/v1/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest("account-1", "account-2", null)))
                .andExpect(status().isBadRequest());
    }



    private String transferRequest(String accountFrom, String accountTo, Integer amount) {
        StringBuilder request = new StringBuilder();
        request.append("{");
        if (accountFrom != null) {
            request.append("\"accountFromId\":\"").append(accountFrom).append("\",");
        }
        if (accountTo != null) {
            request.append("\"accountToId\":\"").append(accountTo).append("\",");
        }
        request.append("\"amount\":").append(amount);
        request.append("}");

        return request.toString();
    }
}
