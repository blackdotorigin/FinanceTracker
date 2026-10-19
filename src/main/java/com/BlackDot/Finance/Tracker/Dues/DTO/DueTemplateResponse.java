package com.BlackDot.Finance.Tracker.Dues.DTO;

import java.util.List;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.Dues.DueFrequency;
import com.BlackDot.Finance.Tracker.Dues.DueStatus;

public record DueTemplateResponse(
        List<CategoryOption> categories,
        List<FieldOption> fields,
        List<DueFrequency> frequencies,
        List<DueStatus> statuses) {

    public record CategoryOption(
            UUID id, String code, String name, List<SubcategoryOption> subcategories) {}

    public record SubcategoryOption(UUID id, String code, String name) {}

    public record FieldOption(
            String name, String type, boolean required, String description, String defaultValue) {}

    public static DueTemplateResponse of(List<CategoryOption> categories) {
        List<FieldOption> fields = List.of(
                new FieldOption("title", "string", true, "Short name for this due.", null),
                new FieldOption("description", "string", false, "Additional details.", null),
                new FieldOption("categoryId", "uuid", true, "ID of a category returned in categories.", null),
                new FieldOption("subCategoryId", "uuid", false, "Optional subcategory ID belonging to categoryId.", null),
                new FieldOption("amount", "decimal", true, "Positive amount, up to 4 decimals.", null),
                new FieldOption("currency", "string", true, "Three-letter uppercase currency code.", "INR"),
                new FieldOption("dueDate", "date", true, "Next due date in YYYY-MM-DD format.", null),
                new FieldOption("frequency", "enum", true, "How often this due recurs.", "ONCE"),
                new FieldOption("frequencyInterval", "integer", true, "Repeat every this many frequency units (1-120).", "1"),
                new FieldOption("reminderDaysBefore", "integer[]", true, "Days before the due date to remind; use [] to disable.", "[1,3,7]"),
                new FieldOption("payeeName", "string", false, "Person or organization to pay or collect from.", null),
                new FieldOption("payeePhoneNumber", "string", false, "Phone number with optional spaces, dots, hyphens, parentheses, or leading +; up to 30 characters.", null),
                new FieldOption("reference", "string", false, "Optional account, contract, or external reference.", null),
                new FieldOption("autoPay", "boolean", true, "Whether the payment is marked for autopay.", "false"),
                new FieldOption("status", "enum", true, "Current lifecycle state.", "ACTIVE"));
        return new DueTemplateResponse(
                categories, fields, List.of(DueFrequency.values()), List.of(DueStatus.values()));
    }
}
