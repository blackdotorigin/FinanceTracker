package com.BlackDot.Finance.Tracker.Dues.DTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.Dues.Due;
import com.BlackDot.Finance.Tracker.Dues.DueCategory;
import com.BlackDot.Finance.Tracker.Dues.DueFrequency;
import com.BlackDot.Finance.Tracker.Dues.DueStatus;
import com.BlackDot.Finance.Tracker.Dues.DueSubcategory;

public record DueResponse(
        UUID id,
        String title,
        String description,
        CategoryRef category,
        CategoryRef subCategory,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        DueFrequency frequency,
        int frequencyInterval,
        List<Integer> reminderDaysBefore,
        String payeeName,
        String payeeUpiId,
        String payeePhoneNumber,
        String reference,
        boolean autoPay,
        DueStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public record CategoryRef(UUID id, String code, String name) {}

    public static DueResponse from(Due due) {
        return new DueResponse(
                due.getId(),
                due.getTitle(),
                due.getDescription(),
                categoryRef(due.getCategory()),
                due.getSubCategory() == null ? null : categoryRef(due.getSubCategory()),
                due.getAmount(),
                due.getCurrency(),
                due.getDueDate(),
                due.getFrequency(),
                due.getFrequencyInterval(),
                java.util.Arrays.stream(due.getReminderDaysBefore()).boxed().toList(),
                due.getPayeeName(),
                due.getPayeeUpiId(),
                due.getPayeePhoneNumber(),
                due.getReference(),
                due.isAutoPay(),
                due.getStatus(),
                due.getCreatedAt(),
                due.getUpdatedAt());
    }

    private static CategoryRef categoryRef(DueCategory category) {
        return new CategoryRef(category.getId(), category.getCode(), category.getName());
    }

    private static CategoryRef categoryRef(DueSubcategory category) {
        return new CategoryRef(category.getId(), category.getCode(), category.getName());
    }
}
