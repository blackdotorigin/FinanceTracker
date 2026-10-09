package com.BlackDot.Finance.Tracker.Auth;

public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {}
