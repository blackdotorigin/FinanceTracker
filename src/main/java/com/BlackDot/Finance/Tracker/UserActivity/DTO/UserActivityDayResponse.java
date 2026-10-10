package com.BlackDot.Finance.Tracker.UserActivity.DTO;

import java.time.LocalDate;

public record UserActivityDayResponse(
        LocalDate date,
        boolean active,
        int transactionCount,
        int loginCount) {
}
