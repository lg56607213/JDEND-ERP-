package com.jdend.erp.auth.controller;

import com.jdend.erp.auth.dto.CompanyUserRequest;
import com.jdend.erp.auth.dto.CompanyUserResponse;
import com.jdend.erp.auth.service.CompanyUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company-users")
public class CompanyUserController {

  private final CompanyUserService service;

  @GetMapping
  public List<CompanyUserResponse> list(HttpSession session) {
    return service.list(session);
  }

  @PostMapping
  public CompanyUserResponse create(@RequestBody CompanyUserRequest req, HttpSession session) {
    return service.create(req, session);
  }

  @PutMapping("/{id}")
  public CompanyUserResponse update(@PathVariable Long id, @RequestBody CompanyUserRequest req, HttpSession session) {
    return service.update(id, req, session);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id, HttpSession session) {
    service.delete(id, session);
  }
}
