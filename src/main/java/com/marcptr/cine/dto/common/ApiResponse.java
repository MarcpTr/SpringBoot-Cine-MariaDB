package com.marcptr.cine.dto.common;

import com.marcptr.cine.model.enums.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ApiError<?> error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode code, String message, T details) {
        return new ApiResponse<>(false, null, new ApiError<>(code, message, details));
    }

    public static <T> ApiResponse<T> fail(ApiError<?> error) {
        return new ApiResponse<>(false, null, error);
    }
}