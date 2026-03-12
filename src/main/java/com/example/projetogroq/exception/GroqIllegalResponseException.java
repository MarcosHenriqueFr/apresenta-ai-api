package com.example.projetogroq.exception;

public class GroqIllegalResponseException extends RuntimeException {
    public GroqIllegalResponseException(String message) {
        super(message);
    }
}
