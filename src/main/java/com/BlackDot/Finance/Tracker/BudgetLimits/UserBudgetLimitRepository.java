package com.BlackDot.Finance.Tracker.BudgetLimits;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBudgetLimitRepository extends JpaRepository<UserBudgetLimit, UUID> {

    List<UserBudgetLimit> findAllByUserIdOrderByCategoryIdAscSubCategoryIdAsc(UUID userId);

    Optional<UserBudgetLimit> findByUserIdAndCategoryIdAndSubCategoryIdIsNull(
            UUID userId, UUID categoryId);

    Optional<UserBudgetLimit> findByUserIdAndCategoryIdAndSubCategoryId(
            UUID userId, UUID categoryId, UUID subCategoryId);
}
