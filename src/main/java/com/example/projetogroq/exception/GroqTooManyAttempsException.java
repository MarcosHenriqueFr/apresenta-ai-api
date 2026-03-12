package com.example.projetogroq.exception;

public class GroqTooManyAttempsException extends RuntimeException {
    public GroqTooManyAttempsException(String message) {
        super(message);
    }
}
