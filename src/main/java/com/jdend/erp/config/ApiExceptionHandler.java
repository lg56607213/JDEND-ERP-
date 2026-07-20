package com.jdend.erp.config;

import com.jdend.erp.auth.exception.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<?> handleForbidden(ForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
      .body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
      .body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<?> handleRuntime(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
      .body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleServerError(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(Map.of("message", "서버 오류: " + e.getMessage()));
  }
}
