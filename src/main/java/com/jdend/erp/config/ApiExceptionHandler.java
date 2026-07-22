package com.jdend.erp.config;

import com.jdend.erp.auth.exception.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

  // BUG-6차-02: 필수 파라미터 누락 → 400
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
      .body(Map.of("message", "필수 파라미터 누락: " + e.getParameterName()));
  }

  // BUG-6차-02: 미존재 경로 → 404
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<?> handleNotFound(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(Map.of("message", "경로를 찾을 수 없습니다: " + e.getResourcePath()));
  }

  // BUG-6차-01: 경로 변수 타입 변환 실패 → 400
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
      .body(Map.of("message", "잘못된 요청 파라미터: " + e.getName() + " = " + e.getValue()));
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
