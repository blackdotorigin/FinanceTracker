package com.BlackDot.Finance.Tracker.BudgetLimits;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.BudgetLimitResponse;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.SetBudgetLimitRequest;
import com.BlackDot.Finance.Tracker.Categories.CategoryService;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBudgetLimitService {

    private final UserBudgetLimitRepository repository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<BudgetLimitResponse> list() {
        return repository.findAllByUserIdOrderByCategoryIdAscSubCategoryIdAsc(CurrentUser.id())
                .stream()
                .map(BudgetLimitResponse::from)
                .toList();
    }

    @Transactional
    public BudgetLimitResponse set(SetBudgetLimitRequest request) {
        categoryService.validateBudgetSelection(request.categoryId(), request.subCategoryId());
        UUID userId = CurrentUser.id();
        UserBudgetLimit budgetLimit = findByScope(
                userId, request.categoryId(), request.subCategoryId())
                .orElseGet(() -> {
                    UserBudgetLimit created = new UserBudgetLimit();
                    created.setUserId(userId);
                    created.setCategoryId(request.categoryId());
                    created.setSubCategoryId(request.subCategoryId());
                    return created;
                });
        budgetLimit.setLimitAmount(request.limit());
        return BudgetLimitResponse.from(repository.save(budgetLimit));
    }

    @Transactional
    public void delete(UUID categoryId, UUID subCategoryId) {
        UserBudgetLimit budgetLimit = findByScope(CurrentUser.id(), categoryId, subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget limit not found"));
        repository.delete(budgetLimit);
    }

    private Optional<UserBudgetLimit> findByScope(
            UUID userId, UUID categoryId, UUID subCategoryId) {
        if (subCategoryId == null) {
            return repository.findByUserIdAndCategoryIdAndSubCategoryIdIsNull(userId, categoryId);
        }
        return repository.findByUserIdAndCategoryIdAndSubCategoryId(
                userId, categoryId, subCategoryId);
    }
}
