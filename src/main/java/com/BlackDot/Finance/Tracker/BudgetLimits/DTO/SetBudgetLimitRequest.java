package com.BlackDot.Finance.Tracker.BudgetLimits.DTO;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SetBudgetLimitRequest(
        @NotNull UUID categoryId,
        UUID subCategoryId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal limit) {}
