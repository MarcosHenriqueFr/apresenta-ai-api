package com.example.projetogroq.exception.custom;

public class GroqIllegalResponseException extends RuntimeException {
    public GroqIllegalResponseException(String message) {
        super(message);
    }
}
