package com.fibank.cashdesk.exception;

public class CashierNotFoundException extends RuntimeException {
    public CashierNotFoundException() {
        super("Cashier not found.");
    }

    public CashierNotFoundException(String message) {
        super("Cashier not found.");
    }

    public CashierNotFoundException(String message, Throwable cause) {
        super("Cashier not found.", cause);
    }

    public CashierNotFoundException(Throwable cause) {
        super("Cashier not found.", cause);
    }
}
