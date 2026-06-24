package com.jdend.erp.contract.earlytermination.controller;

import com.jdend.erp.contract.earlytermination.dto.ContractLookupResponse;
import com.jdend.erp.contract.earlytermination.dto.ContractSearchRowDto;
import com.jdend.erp.contract.earlytermination.service.EarlyTerminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/early-terminations/contracts")
public class ContractLookupController {

  private final EarlyTerminationService service;

  // ✅ 계약번호 정확히 1건 조회 (중도상환 화면에서 사용)
  // GET /api/early-terminations/contracts/lookup?contractNumber=CL0001
  @GetMapping("/lookup")
  public ContractLookupResponse lookup(@RequestParam String contractNumber) {
    return service.lookupContract(contractNumber);
  }

  // ✅ 돋보기 모달 검색 (중도상환 화면에서 사용)
  // GET /api/early-terminations/contracts/search?kw=
  @GetMapping("/search")
  public List<ContractSearchRowDto> search(
      @RequestParam(required = false, defaultValue = "") String kw
  ) {
    return service.searchContracts(kw);
  }
}