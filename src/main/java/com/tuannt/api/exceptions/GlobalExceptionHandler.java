package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 29/08/2023
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String SEPARATE_CHAR = ".";

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<ApiError> handleAllExceptions(Throwable throwable) {
        ApiError exceptionResponse = new ApiError(ApiStatus.INTERNAL_SERVER_ERROR);
        log.error("an unhandier exception: {} occurs", throwable.getMessage(), throwable);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BaseException.class)
    public final ResponseEntity<ApiError> handleCustomException(BaseException exception) {
        ApiError apiError = exception.getApiError();
        log.warn("a handler customException code: {} - message: {}", apiError.getErrorCode(), apiError.getErrorMessage());
        return new ResponseEntity<>(apiError, exception.getStatus());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public final ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getMessage();
        if (message.contains(SEPARATE_CHAR)) {
            String function = message.split("\\" + SEPARATE_CHAR)[0];
            message = message.replace(function + SEPARATE_CHAR, "");
        }
        ApiError apiError = new ApiError(ApiStatus.BAD_REQUEST.name(), message);
        log.warn("a handler constraintViolationException code: {} - message: {}", apiError.getErrorCode(), apiError.getErrorMessage());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @Nullable HttpHeaders headers,
                                                                  @Nullable HttpStatusCode status,
                                                                  @Nullable WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        ApiError exceptionResponse = new ApiError(ApiStatus.BAD_REQUEST.name(), errorMessage);
        log.warn("a handler methodArgumentNotValid code: {} - message: {}", exceptionResponse.getErrorCode(), exceptionResponse.getErrorMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }


    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(@Nullable NoHandlerFoundException ex,
                                                                   @Nullable HttpHeaders headers,
                                                                   @Nullable HttpStatusCode status,
                                                                   @Nullable WebRequest request) {
        return handleExceptionInternal("handleNoHandlerFoundException", HttpStatus.NOT_FOUND, ApiStatus.UNSUPPORTED);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(@Nullable NoResourceFoundException ex,
                                                                    @Nullable HttpHeaders headers,
                                                                    @Nullable HttpStatusCode status,
                                                                    @Nullable WebRequest request) {
        return handleExceptionInternal("handleNoResourceFoundException", HttpStatus.NOT_FOUND, ApiStatus.UNSUPPORTED);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(@Nullable HttpRequestMethodNotSupportedException ex,
                                                                         @Nullable HttpHeaders headers,
                                                                         @Nullable HttpStatusCode status,
                                                                         @Nullable WebRequest request) {
        return handleExceptionInternal("handleHttpRequestMethodNotSupported", HttpStatus.METHOD_NOT_ALLOWED, ApiStatus.METHOD_NOT_ALLOW);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(@Nullable HttpMediaTypeNotSupportedException ex,
                                                                     @Nullable HttpHeaders headers,
                                                                     @Nullable HttpStatusCode status,
                                                                     @Nullable WebRequest request) {
        return handleExceptionInternal("handleHttpMediaTypeNotSupported", HttpStatus.BAD_REQUEST, ApiStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@Nullable HttpMessageNotReadableException ex,
                                                                  @Nullable HttpHeaders headers,
                                                                  @Nullable HttpStatusCode status,
                                                                  @Nullable WebRequest request) {
        return handleExceptionInternal("handleHttpMessageNotReadable", HttpStatus.BAD_REQUEST, ApiStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> handleExceptionInternal(String message, HttpStatus httpStatus, ApiStatus apiStatus) {
        log.warn("a handler exception code: {} - message: {}", apiStatus.name(), message);
        return new ResponseEntity<>(new ApiError(apiStatus), httpStatus);
    }
}
