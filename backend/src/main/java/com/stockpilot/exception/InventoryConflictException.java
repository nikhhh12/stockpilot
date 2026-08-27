package com.stockpilot.exception;

import org.springframework.http.HttpStatus;

public class InventoryConflictException extends StockPilotException {
    public InventoryConflictException() {
        super("Inventory was updated by another user. Please refresh and retry.", "INVENTORY_CONFLICT", HttpStatus.CONFLICT);
    }

    public InventoryConflictException(String message) {
        super(message, "INVENTORY_CONFLICT", HttpStatus.CONFLICT);
    }
}
