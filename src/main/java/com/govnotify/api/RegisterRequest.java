package com.govnotify.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Name is required")
    String fullName,

    @Email(message = "Enter a valid email")
    @NotBlank
    String email,

    @Size(min = 8, message = "Password must contain at least 8 characters")
    String password
) {
}