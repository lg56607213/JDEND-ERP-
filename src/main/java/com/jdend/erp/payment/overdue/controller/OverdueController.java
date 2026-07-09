package com.jdend.erp.payment.overdue.controller;

import com.jdend.erp.payment.overdue.dto.OverdueRowResponse;
import com.jdend.erp.payment.overdue.service.OverdueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/overdue")
public class OverdueController {

    private final OverdueService service;

    @GetMapping
    public List<OverdueRowResponse> list() {
        return service.overdueList();
    }
}
