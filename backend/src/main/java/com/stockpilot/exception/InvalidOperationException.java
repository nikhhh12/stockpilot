package com.stockpilot.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends StockPilotException {
    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION", HttpStatus.BAD_REQUEST);
    }
}
