package com.jdend.erp.vehicle.maintenance.controller;

import com.jdend.erp.vehicle.maintenance.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

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