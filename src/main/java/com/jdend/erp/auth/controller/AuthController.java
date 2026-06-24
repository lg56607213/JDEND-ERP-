package com.jdend.erp.auth.controller;

import com.jdend.erp.auth.dto.*;
import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService service;

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest req, HttpSession session) {
    try {
      return service.login(req, session);
    } catch (Exception e) {
      return LoginResponse.builder()
          .success(false)
          .message(e.getMessage())
          .build();
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