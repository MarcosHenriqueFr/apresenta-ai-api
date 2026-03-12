package com.example.projetogroq.exception.global;

import java.time.LocalDateTime;

public class ExceptionResponse {

    private LocalDateTime timestamp;
    private int status;
    private String reason;
    private String message;

    public ExceptionResponse(int status, String reason, String message){
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.reason = reason;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
