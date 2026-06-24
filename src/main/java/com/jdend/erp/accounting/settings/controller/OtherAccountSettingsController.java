package com.jdend.erp.accounting.settings.controller;

import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsRequest;
import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsResponse;
import com.jdend.erp.accounting.settings.service.OtherAccountSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounting/other-account-settings")

public class OtherAccountSettingsController {

  private final OtherAccountSettingsService service;

  @GetMapping
  public OtherAccountSettingsResponse get() {
    return service.get();
  }

  @PutMapping
  public OtherAccountSettingsResponse save(@RequestBody OtherAccountSettingsRequest req) {
    return service.save(req);
  }
}