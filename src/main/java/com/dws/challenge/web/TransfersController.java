package com.dws.challenge.web;

import com.dws.challenge.domain.TransferFailure;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.domain.TransferExecution;
import com.dws.challenge.exception.InvalidTransferException;
import com.dws.challenge.service.TransfersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/transfers")
@Slf4j
public class TransfersController {

    @Autowired
    private TransfersService transfersService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transfer(@RequestBody @Valid TransferRequest transferRequest) {
        try {
            TransferExecution transferExecution = transfersService.transfer(transferRequest);
            return ResponseEntity.ok(transferExecution);
        } catch (InvalidTransferException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new TransferFailure(transferRequest, LocalDateTime.now(), e.getMessage()));
        }
    }
}
