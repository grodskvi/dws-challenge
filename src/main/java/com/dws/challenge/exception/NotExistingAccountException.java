package com.dws.challenge.exception;

public class NotExistingAccountException extends Exception {
    public NotExistingAccountException(String accountId) {
        super("Account " + accountId + " does not exist");
    }
}
