package com.BlackDot.Finance.Tracker.CustomException;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { 
        super(message); 
    }
}