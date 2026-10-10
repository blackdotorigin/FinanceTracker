package com.BlackDot.Finance.Tracker.UserActivity.DTO;

import java.time.LocalDate;
import java.util.List;

public record UserActivityResponse(
        LocalDate from,
        LocalDate to,
        List<UserActivityDayResponse> days) {
}
