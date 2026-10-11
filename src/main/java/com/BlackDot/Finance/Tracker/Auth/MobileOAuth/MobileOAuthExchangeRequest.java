package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MobileOAuthExchangeRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$")
        String code,
        @NotBlank
        @Size(min = 43, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9._~-]+$")
        String codeVerifier,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$")
        String state) {}
