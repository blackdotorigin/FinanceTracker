package com.BlackDot.Finance.Tracker.Categories;

import java.util.List;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategoryResponse;

public record CategoryResponse(UUID id, String name, String icon, String color,
                               List<SubCategoryResponse> subCategories) {}
