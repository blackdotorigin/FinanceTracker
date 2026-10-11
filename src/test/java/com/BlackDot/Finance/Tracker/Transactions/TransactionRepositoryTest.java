package com.BlackDot.Finance.Tracker.Transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes",
        "spring.security.oauth2.client.registration.google.client-id=test-google-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.github.client-id=test-github-client",
        "spring.security.oauth2.client.registration.github.client-secret=test-github-secret"
})
@Transactional
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchCombinesFiltersAndRestrictsResultsToRequestedUser() {
        UUID userId = createUser();
        UUID otherUserId = createUser();
        UUID categoryId = createCategory(TransactionType.INCOME);
        UUID otherCategoryId = createCategory(TransactionType.EXPENSE);
        UUID subCategoryId = createSubCategory(categoryId);
        LocalDate date = LocalDate.of(2024, 1, 10);

        UUID matchingId = createTransaction(
                userId, categoryId, subCategoryId, TransactionType.INCOME,
                PaymentMode.CASH, date);
        createTransaction(
                userId, categoryId, subCategoryId, TransactionType.INCOME,
                PaymentMode.BANK_ACCOUNT, date);
        createTransaction(
                userId, otherCategoryId, null, TransactionType.EXPENSE,
                PaymentMode.CASH, date);
        createTransaction(
                otherUserId, categoryId, subCategoryId, TransactionType.INCOME,
                PaymentMode.CASH, date);

        Page<Transaction> filtered = repository.findAll(
                TransactionSpecifications.search(
                        userId, null, TransactionType.INCOME, PaymentMode.CASH,
                        date, date, categoryId, subCategoryId),
                PageRequest.of(0, 20));

        assertEquals(1, filtered.getTotalElements());
        assertEquals(matchingId, filtered.getContent().getFirst().getId());

        Page<Transaction> byId = repository.findAll(
                TransactionSpecifications.search(
                        userId, matchingId, null, null, null, null, null, null),
                PageRequest.of(0, 20));
        assertEquals(1, byId.getTotalElements());
        assertEquals(matchingId, byId.getContent().getFirst().getId());
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, username, full_name, default_currency,
                     created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 'INR', ?, ?, 0)
                """, userId, userId + "@example.test", "transaction-" + userId,
                "Transaction test", now, now);
        return userId;
    }

    private UUID createCategory(TransactionType type) {
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO categories
                    (id, name, system_key, type, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, 0)
                """, categoryId, "Category " + categoryId, categoryId.toString(),
                type.name(), now, now);
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

    private UUID createTransaction(
            UUID userId, UUID categoryId, UUID subCategoryId,
            TransactionType type, PaymentMode paymentMode, LocalDate date) {
        UUID transactionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO transactions
                    (id, user_id, amount, currency, type, payment_mode,
                     transaction_date, category_id, sub_category_id,
                     created_at, updated_at, version)
                VALUES (?, ?, ?, 'INR', ?, ?, ?, ?, ?, ?, ?, 0)
                """, transactionId, userId, new BigDecimal("12.50"), type.name(),
                paymentMode.name(), date, categoryId, subCategoryId, now, now);
        return transactionId;
    }
}
