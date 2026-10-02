package com.example.orderservice.exception;

public class DownstreamUnavailableException extends RuntimeException {
    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
