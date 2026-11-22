package com.smart.faidahalisi.Exception;

public class IllegalArgumentException extends RuntimeException {
    private String message;

    public IllegalArgumentException(String message) {
        super(message);
        this.message = message;
    }
}
