package com.stockpilot.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends StockPilotException {
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.BAD_REQUEST);
    }
}
