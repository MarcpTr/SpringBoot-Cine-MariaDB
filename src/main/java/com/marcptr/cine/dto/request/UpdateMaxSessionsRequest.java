package com.marcptr.cine.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateMaxSessionsRequest(
        @Min(value = 1, message = "{error.MIN_SESSIONS}") @Max(value = 10, message = "{error.MAX_SESSIONS}") int maxSessions) {
}
