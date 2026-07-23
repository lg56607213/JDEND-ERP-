package com.jdend.erp.auth.controller;

import com.jdend.erp.auth.dto.*;
import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService service;

  // BUG-12-02: 로그인 실패 시 HTTP 401 반환 (기존 200)
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req, HttpSession session) {
    try {
      return ResponseEntity.ok(service.login(req, session));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(LoginResponse.builder()
              .success(false)
              .message(e.getMessage())
              .build());
    }
  }

  @GetMapping("/me")
  public LoginResponse me(HttpSession session) {
    return service.me(session);
  }

  @PostMapping("/logout")
  public void logout(HttpSession session) {
    service.logout(session);
  }

  @GetMapping("/users")
  public List<LoginUserAdminResponse> users(
      @RequestParam(required = false, defaultValue = "") String kw,
      HttpSession session
  ) {
    return service.adminList(kw, session);
  }

  @PostMapping("/users")
  public LoginUserAdminResponse createUser(
      @RequestBody LoginUserCreateRequest req,
      HttpSession session
  ) {
    return service.adminCreate(req, session);
  }

  @PutMapping("/users/{id}")
  public LoginUserAdminResponse updateUser(
      @PathVariable Long id,
      @RequestBody LoginUserUpdateRequest req,
      HttpSession session
  ) {
    return service.adminUpdate(id, req, session);
  }

  @DeleteMapping("/users/{id}")
  public void deleteUser(@PathVariable Long id, HttpSession session) {
    service.adminDelete(id, session);
  }
}