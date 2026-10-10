package com.BlackDot.Finance.Tracker.UserActivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.BlackDot.Finance.Tracker.UserActivity.DTO.UserActivityDayResponse;
import com.BlackDot.Finance.Tracker.UserActivity.DTO.UserActivityResponse;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes",
        "spring.security.oauth2.client.registration.google.client-id=test-google-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.github.client-id=test-github-client",
        "spring.security.oauth2.client.registration.github.client-secret=test-github-secret"
})
@Transactional
class UserActivityRepositoryTest {

    @Autowired
    private UserActivityRepository repository;

    @Autowired
    private UserActivityService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tracksDailyActivityAndReturnsInactiveDates() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate inactiveDate = today.minusDays(1);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, username, full_name, default_currency,
                     created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 'INR', ?, ?, 0)
                """, userId, userId + "@example.test", "activity-" + userId,
                "Activity test", now, now);

        repository.incrementTransaction(UUID.randomUUID(), userId, today);
        repository.incrementTransaction(UUID.randomUUID(), userId, today);
        repository.recordLogin(UUID.randomUUID(), userId, today);
        repository.incrementTransaction(UUID.randomUUID(), userId, inactiveDate);

        List<UserActivity> activity = repository
                .findByUserIdAndActivityDateBetweenOrderByActivityDate(
                        userId, inactiveDate, today);
        assertEquals(2, activity.size());
        assertEquals(1, activity.get(0).getTransactionCount());
        assertEquals(2, activity.get(1).getTransactionCount());
        assertEquals(1, activity.get(1).getLoginCount());

        repository.decrementTransaction(userId, inactiveDate);
        repository.deleteIfInactive(userId, inactiveDate);
        assertTrue(repository
                .findByUserIdAndActivityDateBetweenOrderByActivityDate(
                        userId, inactiveDate, inactiveDate)
                .isEmpty());

        UserActivityResponse response = service.getActivity(userId, 3);
        assertEquals(3, response.days().size());
        assertEquals(today, response.to());
        assertFalse(response.days().get(0).active());
        assertFalse(response.days().get(1).active());
        assertTrue(response.days().get(2).active());
        assertEquals(2, response.days().get(2).transactionCount());
        assertEquals(1, response.days().get(2).loginCount());

        LocalDate historicalDate = LocalDate.of(2020, 6, 15);
        repository.incrementTransaction(UUID.randomUUID(), userId, historicalDate);
        UserActivityResponse yearResponse = service.getActivity(userId, null, 2020);
        assertEquals(LocalDate.of(2020, 1, 1), yearResponse.from());
        assertEquals(LocalDate.of(2020, 12, 31), yearResponse.to());
        assertEquals(366, yearResponse.days().size());
        UserActivityDayResponse historicalDay = yearResponse.days().stream()
                .filter(day -> day.date().equals(historicalDate))
                .findFirst()
                .orElseThrow();
        assertTrue(historicalDay.active());
        assertEquals(1, historicalDay.transactionCount());
    }
}
