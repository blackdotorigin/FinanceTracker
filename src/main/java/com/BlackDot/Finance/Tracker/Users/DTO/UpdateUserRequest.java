package com.BlackDot.Finance.Tracker.Users.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

    @NotBlank @Size(max = 150) 
    String fullName,
    
    @Pattern(regexp = "[A-Z]{3}")
    String defaultCurrency) {}
