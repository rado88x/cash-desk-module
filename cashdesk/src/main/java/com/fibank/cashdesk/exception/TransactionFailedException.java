package com.fibank.cashdesk.exception;

public class TransactionFailedException extends RuntimeException {
    public TransactionFailedException() {
        super("Transaction failed.");
    }

    public TransactionFailedException(String message) {
        super("Transaction failed.");
    }

    public TransactionFailedException(String message, Throwable cause) {
        super("Transaction failed.", cause);
    }

    public TransactionFailedException(Throwable cause) {
        super("Transaction failed.", cause);
    }
}
