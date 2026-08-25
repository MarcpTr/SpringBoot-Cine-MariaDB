package com.marcptr.cine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "{error.USERNAME_OR_EMAIL_REQUIRED}")
    String usernameOrEmail,

    @NotBlank(message = "{error.PASSWORD_REQUIRED}")
    String password
) {}