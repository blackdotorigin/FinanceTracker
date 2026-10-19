package com.BlackDot.Finance.Tracker.Dues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.BlackDot.Finance.Tracker.Auth.AuthUser;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueRequest;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueResponse;
import com.BlackDot.Finance.Tracker.Roles.Roles;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes",
        "spring.security.oauth2.client.registration.google.client-id=test-google-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.github.client-id=test-github-client",
        "spring.security.oauth2.client.registration.github.client-secret=test-github-secret"
})
@Transactional
class DueServiceTest {

    @Autowired
    private DueService service;

    @Autowired
    private DueRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void templateLoadsActiveCategoriesAndSubcategoriesFromDatabase() {
        var template = service.template();

        var subscriptions = template.categories().stream()
                .filter(category -> category.code().equals("SUBSCRIPTIONS"))
                .findFirst().orElseThrow();
        assertEquals("Subscriptions", subscriptions.name());
        assertEquals("Netflix", subscriptions.subcategories().stream()
                .filter(subcategory -> subcategory.code().equals("NETFLIX"))
                .findFirst().orElseThrow().name());
    }

    @Test
    void persistsFiltersUpdatesAndDeletesOnlyWithinTheOwnerScope() {
        UUID userId = createUser();
        UUID otherUserId = createUser();
        authenticate(userId);

        UUID subscriptionsId = categoryId("SUBSCRIPTIONS");
        UUID netflixId = subCategoryId(subscriptionsId, "NETFLIX");
        DueResponse created = service.create(userId, request(
                "Netflix", subscriptionsId, netflixId, DueFrequency.MONTHLY, 1,
                List.of(1, 3, 7), DueStatus.ACTIVE));

        assertEquals(List.of(1, 3, 7), created.reminderDaysBefore());
        assertEquals(1, service.list(userId, subscriptionsId, netflixId, DueStatus.ACTIVE,
                null, null, pageable()).getTotalElements());
        assertEquals(0, service.list(userId, categoryId("LENDING"), null, null,
                null, null, pageable()).getTotalElements());

        assertThrows(AccessDeniedException.class, () -> service.update(
                otherUserId, created.id(), request(
                        "Netflix", subscriptionsId, netflixId, DueFrequency.MONTHLY, 1,
                        List.of(1, 3, 7), DueStatus.ACTIVE)));

        DueResponse updated = service.update(userId, created.id(), request(
                "Netflix", subscriptionsId, netflixId, DueFrequency.MONTHLY, 1,
                List.of(3, 7), DueStatus.PAUSED));
        assertEquals(DueStatus.PAUSED, updated.status());
        assertEquals(List.of(3, 7), updated.reminderDaysBefore());

        service.delete(userId, created.id());
        assertEquals(0, service.list(userId, null, null, null,
                null, null, pageable()).getTotalElements());
    }

    @Test
    void rejectsInvalidOneTimeIntervalsAndDuplicateReminderOffsets() {
        UUID userId = createUser();
        authenticate(userId);

        UUID householdId = categoryId("HOUSEHOLD");
        assertThrows(BadRequestException.class, () -> service.create(userId, request(
                "One-time bill", categoryId("OTHER"), null, DueFrequency.ONCE, 2,
                List.of(), DueStatus.ACTIVE)));
        assertThrows(BadRequestException.class, () -> service.create(userId, request(
                "Monthly bill", householdId, null, DueFrequency.MONTHLY, 1,
                List.of(1, 1), DueStatus.ACTIVE)));
        assertThrows(BadRequestException.class, () -> service.create(userId, request(
                "Wrong category", householdId,
                subCategoryId(categoryId("SUBSCRIPTIONS"), "NETFLIX"),
                DueFrequency.MONTHLY, 1, List.of(), DueStatus.ACTIVE)));
    }

    @Test
    void storesReturnsAndUpdatesPayeeUpiId() {
        UUID userId = createUser();
        authenticate(userId);
        UUID householdId = categoryId("HOUSEHOLD");

        DueResponse created = service.create(userId, request(
                "Rent", householdId, null, DueFrequency.MONTHLY, 1,
                List.of(1, 3, 7), DueStatus.ACTIVE, "  someone@oksbi  "));

        assertEquals("someone@oksbi", created.payeeUpiId());
        repository.flush();
        assertEquals("someone@oksbi", jdbcTemplate.queryForObject(
                "SELECT payee_upi_id FROM dues WHERE id = ?", String.class, created.id()));
        assertEquals("someone@oksbi", service.list(userId, null, null, null,
                null, null, pageable()).getContent().getFirst().payeeUpiId());

        DueResponse unchanged = service.update(userId, created.id(), request(
                "Rent", householdId, null, DueFrequency.MONTHLY, 1,
                List.of(1, 3, 7), DueStatus.ACTIVE, null));
        assertEquals("someone@oksbi", unchanged.payeeUpiId());

        DueResponse cleared = service.update(userId, created.id(), request(
                "Rent", householdId, null, DueFrequency.MONTHLY, 1,
                List.of(1, 3, 7), DueStatus.ACTIVE, "  "));
        assertNull(cleared.payeeUpiId());
        repository.flush();
        assertNull(jdbcTemplate.queryForObject(
                "SELECT payee_upi_id FROM dues WHERE id = ?", String.class, created.id()));
    }

    @Test
    void acceptsMissingUpiIdsAndRejectsMalformedUpiIds() {
        UUID userId = createUser();
        authenticate(userId);
        UUID otherId = categoryId("OTHER");

        DueResponse created = service.create(userId, request(
                "Reminder", otherId, null, DueFrequency.ONCE, 1,
                List.of(), DueStatus.ACTIVE));
        assertNull(created.payeeUpiId());

        assertThrows(BadRequestException.class, () -> service.create(userId, request(
                "Reminder", otherId, null, DueFrequency.ONCE, 1,
                List.of(), DueStatus.ACTIVE, "not-a-upi-id")));
    }

    private DueRequest request(
            String title, UUID categoryId, UUID subCategoryId,
            DueFrequency frequency, int interval,
            List<Integer> reminders, DueStatus status) {
        return request(title, categoryId, subCategoryId, frequency, interval,
                reminders, status, null);
    }

    private DueRequest request(
            String title, UUID categoryId, UUID subCategoryId,
            DueFrequency frequency, int interval,
            List<Integer> reminders, DueStatus status, String payeeUpiId) {
        return new DueRequest(
                title, null, categoryId, subCategoryId, new BigDecimal("649.00"), "INR",
                LocalDate.of(2026, 11, 1), frequency, interval, reminders,
                title, payeeUpiId, null, false, status);
    }

    private UUID categoryId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM due_categories WHERE code = ?", UUID.class, code);
    }

    private UUID subCategoryId(UUID categoryId, String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM due_subcategories WHERE category_id = ? AND code = ?",
                UUID.class, categoryId, code);
    }

    private PageRequest pageable() {
        return PageRequest.of(0, 20, Sort.by(
                Sort.Order.asc("dueDate"), Sort.Order.asc("id")));
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
                """, userId, userId + "@example.test", "dues-" + userId,
                "Dues test", now, now);
        return userId;
    }
}
