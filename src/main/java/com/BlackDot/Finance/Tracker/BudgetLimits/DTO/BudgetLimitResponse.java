package com.BlackDot.Finance.Tracker.BudgetLimits.DTO;

import java.math.BigDecimal;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.BudgetLimits.UserBudgetLimit;

public record BudgetLimitResponse(
        UUID id, UUID categoryId, UUID subCategoryId, BigDecimal limit) {

    public static BudgetLimitResponse from(UserBudgetLimit budgetLimit) {
        return new BudgetLimitResponse(
                budgetLimit.getId(),
                budgetLimit.getCategoryId(),
                budgetLimit.getSubCategoryId(),
                budgetLimit.getLimitAmount());
    }
}
