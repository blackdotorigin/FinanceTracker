package com.BlackDot.Finance.Tracker.UserActivity;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.UserActivity.DTO.UserActivityDayResponse;
import com.BlackDot.Finance.Tracker.UserActivity.DTO.UserActivityResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;

    private final UserActivityRepository repository;

    @Transactional
    public void recordLogin(UUID userId) {
        repository.recordLogin(UUID.randomUUID(), userId, LocalDate.now(ZoneOffset.UTC));
    }

    @Transactional
    public void recordTransaction(UUID userId, LocalDate activityDate) {
        repository.incrementTransaction(UUID.randomUUID(), userId, activityDate);
    }

    @Transactional
    public void removeTransaction(UUID userId, LocalDate activityDate) {
        repository.decrementTransaction(userId, activityDate);
        repository.deleteIfInactive(userId, activityDate);
    }

    @Transactional(readOnly = true)
    public UserActivityResponse getActivity(UUID userId, Integer requestedDays, Integer requestedYear) {
        if (requestedDays != null && requestedYear != null) {
            throw new BadRequestException("Specify either 'days' or 'year', not both");
        }

        if (requestedYear != null) {
            int currentYear = Year.now(ZoneOffset.UTC).getValue();
            if (requestedYear < 1 || requestedYear > currentYear) {
                throw new BadRequestException("'year' must be between 1 and " + currentYear);
            }
            return getActivityForRange(
                    userId,
                    LocalDate.of(requestedYear, 1, 1),
                    LocalDate.of(requestedYear, 12, 31));
        }

        int days = requestedDays == null ? DEFAULT_DAYS : requestedDays;
        if (days < 1 || days > MAX_DAYS) {
            throw new BadRequestException("'days' must be between 1 and " + MAX_DAYS);
        }

        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(days - 1L);
        return getActivityForRange(userId, from, to);
    }

    @Transactional(readOnly = true)
    public UserActivityResponse getActivity(UUID userId, Integer requestedDays) {
        return getActivity(userId, requestedDays, null);
    }

    private UserActivityResponse getActivityForRange(UUID userId, LocalDate from, LocalDate to) {
        int days = Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
        List<UserActivity> activity = repository
                .findByUserIdAndActivityDateBetweenOrderByActivityDate(userId, from, to);
        Map<LocalDate, UserActivity> activityByDate = new HashMap<>();
        activity.forEach(day -> activityByDate.put(day.getActivityDate(), day));

        List<UserActivityDayResponse> daysResponse = IntStream.range(0, days)
                .mapToObj(offset -> {
                    LocalDate date = from.plusDays(offset);
                    UserActivity entry = activityByDate.get(date);
                    int transactionCount = entry == null ? 0 : entry.getTransactionCount();
                    int loginCount = entry == null ? 0 : entry.getLoginCount();
                    return new UserActivityDayResponse(
                            date, transactionCount > 0 || loginCount > 0,
                            transactionCount, loginCount);
                })
                .toList();

        return new UserActivityResponse(from, to, daysResponse);
    }
}
