package com.marcptr.cine.exception;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.marcptr.cine.dto.common.ApiError;
import com.marcptr.cine.dto.common.ApiResponse;
import com.marcptr.cine.exception.tmdb.TmdbException;
import com.marcptr.cine.exception.tmdb.TmdbNotFoundException;
import com.marcptr.cine.model.enums.ErrorCode;
import com.marcptr.cine.utils.MessageResolver;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

        private final MessageResolver messageResolver;

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleGenericException(Exception ex) {

                ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode), null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
                        MethodArgumentNotValidException ex) {

                ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult().getFieldErrors().forEach(error -> {

                        if ("period".equals(error.getField())) {
                                errors.put("period", messageResolver.resolveMessage(ErrorCode.INVALID_PERIOD));
                        } else {
                                errors.put(error.getField(), error.getDefaultMessage());
                        }
                });

                return ResponseEntity.badRequest().body(
                                ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                errors));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
                        ConstraintViolationException ex) {

                ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
                Map<String, String> errors = new HashMap<>();

                ex.getConstraintViolations().forEach(violation -> {
                        String field = violation.getPropertyPath().toString();
                        String message = violation.getMessage();
                        errors.put(field, message);
                });

                return ResponseEntity.badRequest().body(
                                ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                errors));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex) {
                ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
                Map<String, String> errors = new HashMap<>();

                String field = ex.getName();
                String expectedType = ex.getRequiredType() != null
                                ? ex.getRequiredType().getSimpleName()
                                : "Valid type";

                errors.put(field, "Required type: " + expectedType);

                return ResponseEntity.badRequest()
                                .body(ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                errors));
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex) {

                ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
                Map<String, String> errors = new HashMap<>();

                String method = ex.getMethod();
                String supportedMethods = ex.getSupportedMethods() != null
                                ? Arrays.toString(ex.getSupportedMethods())
                                : "N/A";
                errors.put("method", "Method not supported: " + method);
                errors.put("allowed", "Allowed methods: " + supportedMethods);

                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                errors));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidJson(HttpMessageNotReadableException ex) {

                ErrorCode errrorCode = ErrorCode.INVALID_JSON;

                return ResponseEntity.badRequest().body(
                                ApiResponse.fail(errrorCode, messageResolver.resolveMessage(errrorCode),
                                                null));
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleMissingParams(
                        MissingServletRequestParameterException ex) {

                ErrorCode errorCode = ErrorCode.MISSING_PARAMETER;
                Map<String, String> errors = new HashMap<>();

                errors.put(ex.getParameterName(), "Parameter is required");

                return ResponseEntity.badRequest()
                                .body(ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                errors));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleAccessDenied(AccessDeniedException ex) {

                ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.fail(errorCode, messageResolver.resolveMessage(errorCode),
                                                null));
        }

        // Custom Exceptions

        @ExceptionHandler(TmdbException.class)
        public ResponseEntity<ApiResponse<Void>> handleTmdbClientEntity(
                        TmdbException ex) {

                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());

                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail(error));
        }

        @ExceptionHandler(TmdbNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleTmdbResourceNotFound(
                        TmdbNotFoundException ex) {

                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(error));
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
                        InvalidCredentialsException ex) {

                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError<Object> error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(error));
        }

        @ExceptionHandler(JwtAuthenticationException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleJwtAuthenticationException(
                        JwtAuthenticationException ex) {
                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError<Object> error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.fail(error));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleResouceNotFound(ResourceNotFoundException ex) {

                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError<Object> error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.fail(error));
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleResourceAlreadyExist(
                        ResourceAlreadyExistsException ex) {
                String message = messageResolver.resolveMessage(ex.getCode());
                ApiError<Object> error = new ApiError<>(
                                ex.getCode(),
                                message,
                                ex.getDetails());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.fail(error));
        }

}