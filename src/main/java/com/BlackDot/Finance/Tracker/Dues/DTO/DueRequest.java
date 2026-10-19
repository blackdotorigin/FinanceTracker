package com.BlackDot.Finance.Tracker.Dues.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.BlackDot.Finance.Tracker.Dues.DueFrequency;
import com.BlackDot.Finance.Tracker.Dues.DueStatus;
import com.BlackDot.Finance.Tracker.Validation.NoMarkup;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DueRequest(
        @NoMarkup @NotBlank @Size(max = 120) String title,
        @NoMarkup @Size(max = 1000) String description,
        @NotNull java.util.UUID categoryId,
        java.util.UUID subCategoryId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull LocalDate dueDate,
        @NotNull DueFrequency frequency,
        @Min(1) @Max(120) int frequencyInterval,
        @NotNull @Size(max = 30) List<@NotNull @Min(0) @Max(365) Integer> reminderDaysBefore,
        @NoMarkup @Size(max = 120) String payeeName,
        String payeeUpiId,
        @Size(max = 30) String payeePhoneNumber,
        @NoMarkup @Size(max = 100) String reference,
        boolean autoPay,
        @NotNull DueStatus status) {}
