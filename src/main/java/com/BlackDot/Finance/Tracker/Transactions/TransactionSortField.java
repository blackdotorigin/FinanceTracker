package com.BlackDot.Finance.Tracker.Transactions;

import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;

public enum TransactionSortField {
    DATE("transactionDate"),
    AMOUNT("amount"),
    CREATED("createdAt");

    private final String property;

    TransactionSortField(String property) { 
        this.property = property; 
    }

    public String property() { 
        return property; 
    }

    public static TransactionSortField fromParam(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid sortBy. Allowed: DATE, AMOUNT, CREATED");
        }
    }
}
