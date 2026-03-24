package com.example.projetogroq.exception.custom;

import java.io.IOException;

public class InvalidFileOperationException extends RuntimeException {
    public InvalidFileOperationException(String message, IOException e) {
        super(message, e);
    }
}
