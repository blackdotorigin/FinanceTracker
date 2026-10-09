package com.BlackDot.Finance.Tracker.Auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
