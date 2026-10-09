package com.BlackDot.Finance.Tracker.Users.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.BlackDot.Finance.Tracker.Validation.NoMarkup;

public record UpdateUserRequest(

    @NoMarkup @NotBlank @Size(max = 150)
    String fullName,
    
    @NoMarkup @Pattern(regexp = "[A-Z]{3}")
    String defaultCurrency) {}
