package com.BlackDot.Finance.Tracker.BudgetLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.BlackDot.Finance.Tracker.Auth.AuthUser;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.BudgetLimitResponse;
import com.BlackDot.Finance.Tracker.BudgetLimits.DTO.SetBudgetLimitRequest;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.Roles.Roles;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes",
        "spring.security.oauth2.client.registration.google.client-id=test-google-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.github.client-id=test-github-client",
        "spring.security.oauth2.client.registration.github.client-secret=test-github-secret"
})
@Transactional
class UserBudgetLimitServiceTest {

    @Autowired
    private UserBudgetLimitService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsCategoryAndSubCategoryLimitsIndependentlyAndScopesThemToTheUser() {
        UUID userId = createUser();
        UUID otherUserId = createUser();
        UUID categoryId = createCategory();
        UUID subCategoryId = createSubCategory(categoryId);
        authenticate(userId);

        BudgetLimitResponse categoryLimit = service.set(
                new SetBudgetLimitRequest(categoryId, null, new BigDecimal("5000.00")));
        BudgetLimitResponse updatedCategoryLimit = service.set(
                new SetBudgetLimitRequest(categoryId, null, new BigDecimal("6000.00")));
        BudgetLimitResponse subCategoryLimit = service.set(
                new SetBudgetLimitRequest(
                        categoryId, subCategoryId, new BigDecimal("1200.00")));

        assertEquals(categoryLimit.id(), updatedCategoryLimit.id());
        assertEquals(new BigDecimal("6000.00"), updatedCategoryLimit.limit());
        assertNotEquals(categoryLimit.id(), subCategoryLimit.id());
        assertEquals(2, service.list().size());

        authenticate(otherUserId);
        assertEquals(0, service.list().size());

        authenticate(userId);
        service.delete(categoryId, subCategoryId);
        assertEquals(1, service.list().size());
        assertEquals(categoryId, service.list().getFirst().categoryId());
    }

    @Test
    void rejectsSubCategoryThatDoesNotBelongToTheSelectedCategory() {
        UUID userId = createUser();
        UUID categoryId = createCategory();
        UUID otherCategoryId = createCategory();
        UUID subCategoryId = createSubCategory(categoryId);
        authenticate(userId);

        assertThrows(BadRequestException.class, () -> service.set(
                new SetBudgetLimitRequest(
                        otherCategoryId, subCategoryId, new BigDecimal("100.00"))));
    }

    private void authenticate(UUID userId) {
        AuthUser principal = AuthUser.fromClaims(userId, userId + "@example.test", Roles.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, username, full_name, default_currency,
                     created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 'INR', ?, ?, 0)
                """, userId, userId + "@example.test", "budget-" + userId,
                "Budget test", now, now);
        return userId;
    }

    private UUID createCategory() {
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO categories
                    (id, name, system_key, type, created_at, updated_at, version)
                VALUES (?, ?, ?, 'EXPENSE', ?, ?, 0)
                """, categoryId, "Category " + categoryId, categoryId.toString(), now, now);
        return categoryId;
    }

    private UUID createSubCategory(UUID categoryId) {
        UUID subCategoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO sub_categories
                    (id, name, system_key, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 0)
                """, subCategoryId, "Subcategory " + subCategoryId,
                subCategoryId.toString(), now, now);
        jdbcTemplate.update("""
                INSERT INTO category_sub_categories (category_id, sub_category_id)
                VALUES (?, ?)
                """, categoryId, subCategoryId);
        return subCategoryId;
    }
}
