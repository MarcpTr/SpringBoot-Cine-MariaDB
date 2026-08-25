package com.marcptr.cine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "{error.refresh.REFRESH_TOKEN_REQUIRED}")
    String refreshToken
) {}