package com.BlackDot.Finance.Tracker.Categories;

import java.util.List;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategoryResponse;
import com.BlackDot.Finance.Tracker.Transactions.TransactionType;

public record CategoryResponse(UUID id, String name, TransactionType type, String icon, String color,
                               List<SubCategoryResponse> subCategories) {}
