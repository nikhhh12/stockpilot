package com.stockpilot.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends StockPilotException {
    public InsufficientStockException(String message) {
        super(message, "INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST);
    }

    public InsufficientStockException(String productSku, int available, int requested) {
        super(String.format("Insufficient available stock for SKU '%s'. Available: %d, Requested: %d", productSku, available, requested),
                "INSUFFICIENT_STOCK", HttpStatus.BAD_REQUEST);
    }
}
