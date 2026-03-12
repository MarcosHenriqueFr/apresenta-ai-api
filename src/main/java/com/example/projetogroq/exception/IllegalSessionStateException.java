package com.example.projetogroq.exception;

public class IllegalSessionStateException extends RuntimeException {
    public IllegalSessionStateException(String message) {
        super(message);
    }
}
