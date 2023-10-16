package com.dws.challenge.domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@Builder
public class TransferRequest {
    @NotNull
    @NotEmpty
    private String accountFromId;

    @NotNull
    @NotEmpty
    private String accountToId;

    @NotNull
    @Positive(message = "Transferred amount must be positive.")
    private BigDecimal amount;
}
