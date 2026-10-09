package com.BlackDot.Finance.Tracker.Auth;

import jakarta.validation.constraints.NotBlank;
import com.BlackDot.Finance.Tracker.Validation.NoMarkup;

public record LoginRequest(@NoMarkup @NotBlank String username, @NotBlank String password) {}
