package com.BlackDot.Finance.Tracker.Transactions.TransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.BlackDot.Finance.Tracker.Transactions.TransactionType;
import com.BlackDot.Finance.Tracker.Validation.NoMarkup;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record  CreateTransactionRequest(
    @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
    @NoMarkup @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull TransactionType type,
    @NotNull LocalDate transactionDate,
    @NotNull UUID categoryId,
    UUID subCategoryId,              // optional
    @NoMarkup @Size(max = 255) String description) {}