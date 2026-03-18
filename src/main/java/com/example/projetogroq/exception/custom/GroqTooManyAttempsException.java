package com.example.projetogroq.exception.custom;

public class GroqTooManyAttempsException extends RuntimeException {
    public GroqTooManyAttempsException(String message) {
        super(message);
    }
}
