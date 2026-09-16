package com.internalmarketplace.api.common.exception;

import com.internalmarketplace.api.common.firestore.FirestoreOperationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Central error translator, replacing middleware/errorHandler.js. Every
 * handler here reproduces the same {@code {error}}/{@code {error, details}}
 * body shape and status code the Express app returned for the equivalent
 * failure.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getPublicMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldIssue(fe.getField(), message(fe)))
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of("Invalid request payload", issues));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("Invalid request payload"));
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiError> handleMultipart(Exception ex) {
        return ResponseEntity.badRequest().body(ApiError.of(ex.getMessage()));
    }

    @ExceptionHandler(FirestoreOperationException.class)
    public ResponseEntity<ApiError> handleFirestore(FirestoreOperationException ex) {
        log.error("Firestore operation failed", ex);
        return ResponseEntity.internalServerError().body(ApiError.of("Internal server error"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex) {
        String message = "No route for " + ex.getHttpMethod() + " " + ex.getRequestURL();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ApiError.of("Internal server error"));
    }

    private static String message(FieldError fe) {
        return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value";
    }
}
