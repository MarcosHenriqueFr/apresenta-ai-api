package com.example.projetogroq.exception.custom;

public class GroqResponseParseException extends RuntimeException {
    public GroqResponseParseException(String message) {
        super(message);
    }
}
