package com.jdend.erp.legal.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.legal.dto.*;
import com.jdend.erp.legal.service.LegalCaseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/legal-cases")
public class LegalCaseController {

    private final LegalCaseService service;
    private final PermissionService permissionService;

    @GetMapping
    public List<LegalCaseResponse> list(
            @RequestParam(required = false) String contractNumber,
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) String status) {
        if (contractNumber != null && !contractNumber.isBlank()) {
            return service.listByContract(contractNumber);
        }
        return service.search(kw, status);
    }

    // BUG-6차-01: /search 별칭 — /{id}가 "search"를 Long 변환 시도하는 혼란 방지
    @GetMapping("/search")
    public List<LegalCaseResponse> search(
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) String status) {
        return service.search(kw, status);
    }

    @GetMapping("/{id}")
    public LegalCaseResponse getOne(@PathVariable Long id) {
        return service.getOne(id);
    }

    @PostMapping
    public LegalCaseResponse create(@RequestBody LegalCaseRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public LegalCaseResponse update(@PathVariable Long id, @RequestBody LegalCaseRequest req,
        HttpSession session) {
        permissionService.requireManager(session);
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpSession session) {
        permissionService.requireManager(session);
        service.delete(id);
    }

    @PostMapping("/{id}/progress")
    public LegalProgressResponse addProgress(@PathVariable Long id, @RequestBody LegalProgressRequest req) {
        return service.addProgress(id, req);
    }

    @DeleteMapping("/{id}/progress/{entryId}")
    public void deleteProgress(@PathVariable Long id, @PathVariable Long entryId,
        HttpSession session) {
        permissionService.requireManager(session);
        service.deleteProgress(id, entryId);
    }

    @PostMapping("/{id}/costs")
    public LegalCostItemResponse addCostItem(@PathVariable Long id, @RequestBody LegalCostItemRequest req) {
        return service.addCostItem(id, req);
    }

    @DeleteMapping("/{id}/costs/{costId}")
    public void deleteCostItem(@PathVariable Long id, @PathVariable Long costId, HttpSession session) {
        permissionService.requireManager(session);
        service.deleteCostItem(id, costId);
    }
}
