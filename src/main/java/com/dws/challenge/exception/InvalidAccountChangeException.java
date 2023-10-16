package com.dws.challenge.exception;

public class InvalidAccountChangeException extends RuntimeException {
    public InvalidAccountChangeException(String message) {
        super(message);
    }
}
