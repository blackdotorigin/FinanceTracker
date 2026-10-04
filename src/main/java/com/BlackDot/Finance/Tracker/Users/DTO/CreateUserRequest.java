package com.BlackDot.Finance.Tracker.Users.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Email 
    String email,

    @NotBlank @Size(min = 3, max = 50)
    String username,

    @NotBlank @Size(min = 8, max = 72) 
    String password,

    @NotBlank @Size(max = 150) 
    String fullName,

    @Pattern(regexp = "[A-Z]{3}") 
    String defaultCurrency) {}
