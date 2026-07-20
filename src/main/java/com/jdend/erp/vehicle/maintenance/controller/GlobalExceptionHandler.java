package com.jdend.erp.vehicle.maintenance.controller;

import com.jdend.erp.auth.exception.ForbiddenException;
import com.jdend.erp.vehicle.maintenance.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ForbiddenException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiErrorResponse handleForbidden(ForbiddenException e) {
    return new ApiErrorResponse(e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleBadRequest(IllegalArgumentException e) {
    return new ApiErrorResponse(e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleServerError(Exception e) {
    return new ApiErrorResponse("서버 오류: " + e.getMessage());
  }
}