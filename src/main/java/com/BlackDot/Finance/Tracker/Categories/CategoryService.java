package com.BlackDot.Finance.Tracker.Categories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategory;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategoryRepository;
import com.BlackDot.Finance.Tracker.SubCategories.SubCategoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final SubCategoryRepository subCategories;

    public record RefData(Map<UUID, Category> categories, Map<UUID, SubCategory> subCategories) {}

    /** The dropdown data: every category with the sub-categories allowed under it. */
    @Cacheable("categoryTree")
    @Transactional(readOnly = true)
    public List<CategoryResponse> tree() {
        return categories.findAllActiveWithSubCategories().stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getName))
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getIcon(), c.getColor(),
                        c.getSubCategories().stream()
                                .filter(SubCategory::isActive)
                                .sorted(Comparator.comparingInt(SubCategory::getSortOrder)
                                        .thenComparing(SubCategory::getName))
                                .map(s -> new SubCategoryResponse(s.getId(), s.getName(), s.getIcon()))
                                .toList()))
                .toList();
    }

    /** Category is required, sub-category optional but must belong to that category. */
    @Transactional(readOnly = true)
    public void validateSelection(UUID categoryId, UUID subCategoryId) {
        if (subCategoryId == null) {
            if (!categories.existsByIdAndActiveTrue(categoryId))
                throw new BadRequestException("Invalid category");
        } else if (!categories.existsActivePair(categoryId, subCategoryId)) {
            throw new BadRequestException("Invalid category / sub-category combination");
        }
    }

    /** For rendering names. Includes inactive rows so old transactions keep their labels. */
    @Transactional(readOnly = true)
    public RefData lookup(Set<UUID> categoryIds, Set<UUID> subCategoryIds) {
        Map<UUID, Category> cats = new HashMap<>();
        categories.findAllById(categoryIds).forEach(c -> cats.put(c.getId(), c));
        Map<UUID, SubCategory> subs = new HashMap<>();
        subCategories.findAllById(subCategoryIds).forEach(s -> subs.put(s.getId(), s));
        return new RefData(cats, subs);
    }
}