package com.example.projetogroq.exception.custom;

public class IllegalSessionStateException extends RuntimeException {
    public IllegalSessionStateException(String message) {
        super(message);
    }
}
