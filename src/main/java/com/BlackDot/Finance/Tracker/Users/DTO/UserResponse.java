package com.BlackDot.Finance.Tracker.Users.DTO;

import java.time.Instant;
import java.util.UUID;

import com.BlackDot.Finance.Tracker.Users.User;

public record UserResponse(UUID id, String email, String username, String fullName,
                           String defaultCurrency, Instant createdAt) {

    public static UserResponse from(User u) {
    return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getFullName(),
                                u.getDefaultCurrency(), u.getCreatedAt());
    }
}