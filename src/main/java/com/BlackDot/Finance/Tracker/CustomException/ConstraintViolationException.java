package com.BlackDot.Finance.Tracker.CustomException;

public class ConstraintViolationException extends RuntimeException {
    public ConstraintViolationException(String message) { 
        super(message); 
    }
}
