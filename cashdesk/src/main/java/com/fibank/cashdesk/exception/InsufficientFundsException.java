package com.fibank.cashdesk.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Insufficient funds.");
    }

    public InsufficientFundsException(String message) {
        super(("Insufficient funds."));
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(("Insufficient funds."), cause);
    }

    public InsufficientFundsException(Throwable cause) {
        super("Insufficient funds.", cause);
    }
}