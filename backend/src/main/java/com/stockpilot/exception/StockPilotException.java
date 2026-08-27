package com.stockpilot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StockPilotException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public StockPilotException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
