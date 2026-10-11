package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

public record MobileOAuthRequest(String redirectUri, String state, String codeChallenge) {}
