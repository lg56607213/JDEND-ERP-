package com.jdend.erp.contract.controller;

import com.jdend.erp.contract.dto.*;
import com.jdend.erp.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")

public class ContractController {

  private final ContractService service;

  @GetMapping
  public List<ContractResponse> list() {
    return service.list();
  }

  // ✅ 계약번호 미리보기
  @GetMapping("/next-number")
  public NextContractNumberResponse nextNumber() {
    return NextContractNumberResponse.builder()
      .contractNumber(service.nextNumberPreview())
      .build();
  }

  // ✅✅✅ 계약번호로 full 상세 (프론트가 ?id=R00001002 같은 상황일 때 쓰면 됨)
  // GET /api/contracts/by-number/R00001002/full
  @GetMapping("/by-number/{contractNumber}/full")
  public ContractFullResponse detailFullByNumber(@PathVariable String contractNumber) {
    return service.detailFullByNumber(contractNumber);
  }

  // ✅ full 상세(수정/출력용) - 숫자 id만 받음
  @GetMapping("/{id:\\d+}/full")
  public ContractFullResponse detailFull(@PathVariable Long id) {
    return service.detailFull(id);
  }

  // ✅ 숫자만 id로 받게 제한
  @GetMapping("/{id:\\d+}")
  public ContractResponse detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PostMapping
  public ContractResponse create(@RequestBody ContractRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id:\\d+}")
  public ContractResponse update(@PathVariable Long id, @RequestBody ContractRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id:\\d+}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}