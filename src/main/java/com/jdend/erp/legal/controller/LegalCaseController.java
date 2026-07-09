package com.jdend.erp.legal.controller;

import com.jdend.erp.legal.dto.*;
import com.jdend.erp.legal.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/legal-cases")
public class LegalCaseController {

    private final LegalCaseService service;

    @GetMapping
    public List<LegalCaseResponse> listByContract(@RequestParam String contractNumber) {
        return service.listByContract(contractNumber);
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
    public LegalCaseResponse update(@PathVariable Long id, @RequestBody LegalCaseRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/progress")
    public LegalProgressResponse addProgress(@PathVariable Long id, @RequestBody LegalProgressRequest req) {
        return service.addProgress(id, req);
    }

    @DeleteMapping("/{id}/progress/{entryId}")
    public void deleteProgress(@PathVariable Long id, @PathVariable Long entryId) {
        service.deleteProgress(id, entryId);
    }
}
