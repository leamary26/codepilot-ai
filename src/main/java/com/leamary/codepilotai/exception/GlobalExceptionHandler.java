
package com.leamary.codepilotai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    // Only approved messages are returned to users
    private static final Set<String> SAFE_MESSAGES =
            Set.of(
                    "Review request is required.",
                    "Please provide code to review.",
                    "Please select a programming language.",
                    "Unsupported programming language.",
                    "Code must not exceed 20000 characters.",
                    "Invalid email or password.",
                    "User not found.",
                    "Review not found.",
                    "AI service is temporarily unavailable. Please try again later.",
                    "Unable to connect to the AI service. Please try again later."
            );

    // ================================
    // 1. Handle known application errors
    // ================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>>
    handleResponseStatusException(
            ResponseStatusException exception) {

        int status =
                exception.getStatusCode().value();

        HttpStatus httpStatus =
                HttpStatus.resolve(status);

        String error = httpStatus != null
                ? httpStatus.getReasonPhrase()
                : "Request Failed";

        String reason = exception.getReason();

        String message;

        // Return only approved application messages
        if (reason != null &&
                SAFE_MESSAGES.contains(reason)) {

            message = reason;

        } else {

            message = getDefaultMessage(status);
        }

        return ResponseEntity
                .status(status)
                .body(createError(
                        status,
                        error,
                        message
                ));
    }

    // ================================
    // 2. Handle invalid JSON requests
    // ================================

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleInvalidJson(
            HttpMessageNotReadableException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createError(
                        400,
                        "Bad Request",
                        "Invalid or missing request data. Please check your input."
                ));
    }

    // ================================
    // 3. Handle unexpected server errors
    // ================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpectedException(
            Exception exception) {

        // Log the exception type internally.
        // Do not expose submitted code, passwords,
        // tokens, API keys or raw error details.

        logger.error(
                "Unexpected application error: {}",
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createError(
                        500,
                        "Internal Server Error",
                        "Something went wrong. Please try again later."
                ));
    }

    // ================================
    // 4. Safe default error messages
    // ================================

    private String getDefaultMessage(int status) {

        return switch (status) {

            case 400 ->
                    "Invalid input. Please check your request.";

            case 401 ->
                    "Authentication failed. Please sign in again.";

            case 403 ->
                    "You do not have permission to perform this action.";

            case 404 ->
                    "The requested resource was not found.";

            case 409 ->
                    "An account with this email may already exist.";

            case 413 ->
                    "The submitted request is too large.";

            case 429 ->
                    "Too many requests. Please try again later.";

            case 502 ->
                    "The AI service returned an invalid response. Please try again.";

            case 503 ->
                    "The AI service is temporarily unavailable. Please try again later.";

            default ->
                    "Something went wrong. Please try again later.";
        };
    }

    // ================================
    // 5. Create consistent JSON response
    // ================================

    private Map<String, Object> createError(
            int status,
            String error,
            String message) {

        return Map.of(
                "status", status,
                "error", error,
                "message", message
        );
    }
}

