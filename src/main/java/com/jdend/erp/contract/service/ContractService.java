package com.jdend.erp.contract.service;

import com.jdend.erp.contract.dto.*;
import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.customer.Customer;
import com.jdend.erp.customer.CustomerRepository;
import com.jdend.erp.payment.schedule.service.PaymentScheduleAutoGeneratorService;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

  private final ContractRepository contractRepo;
  private final CustomerRepository customerRepo;
  private final VehicleOrderRepository vehicleOrderRepo;
  private final PaymentScheduleAutoGeneratorService scheduleAutoGen;

  @Transactional(readOnly = true)
  public List<ContractResponse> list() {
    List<Contract> list = contractRepo.findAll();
    list.sort(Comparator.comparing(Contract::getId).reversed());
    return list.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ContractResponse detail(Long id) {
    Contract c = contractRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("계약 없음 id=" + id));
    return toResponse(c);
  }

  @Transactional(readOnly = true)
  public ContractFullResponse detailFullByNumber(String contractNumber) {
    if (contractNumber == null || contractNumber.isBlank()) {
      throw new RuntimeException("계약번호(contractNumber) 필수");
    }

    Contract c = contractRepo.findWithCustomerByContractNumber(contractNumber.trim())
        .orElseThrow(() -> new RuntimeException("계약 없음 contractNumber=" + contractNumber));

    return toFullResponse(c);
  }

  @Transactional(readOnly = true)
  public ContractFullResponse detailFull(Long id) {
    Contract c = contractRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("계약 없음 id=" + id));
    return toFullResponse(c);
  }

  @Transactional(readOnly = true)
  public String nextNumberPreview() {
    return nextContractNumber();
  }

  private String nextContractNumber() {
    String max = contractRepo.findMaxContractNumber();
    int next = 1001;

    if (max != null && max.startsWith("R")) {
      String digits = max.replaceAll("[^0-9]", "");
      if (!digits.isBlank()) {
        try {
          next = Integer.parseInt(digits) + 1;
        } catch (Exception ignored) {}
      }
    }
    return String.format("R%08d", next);
  }

  @Transactional
  public ContractResponse create(ContractRequest req) {
    validateRequired(req);

    Customer customer = customerRepo.findByCustomerNumber(req.customerNumber).orElse(null);
    VehicleOrder vo = vehicleOrderRepo.findByVehicleNoNormalized(req.vehicleNo).orElse(null);

    Long monthlyRent = nvl(req.monthlyRent);
    Integer billingCount = (req.billingCount == null ? 0 : req.billingCount);
    Long totalRent = (req.totalRent != null ? req.totalRent : monthlyRent * billingCount);

    Contract c = Contract.builder()
        .contractNumber(nextContractNumber())
        .customer(customer)
        .customerNumber(req.customerNumber)
        .vehicleOrder(vo)
        .vehicleNo(req.vehicleNo)
        .vehicleModel(req.vehicleModel != null && !req.vehicleModel.isBlank()
            ? req.vehicleModel
            : (vo != null ? vo.getCarModel() : null))
        .contractType(req.contractType)
        .contractCategory(req.contractCategory)
        .status(normalizeStatus(req.status))
        .startDate(req.startDate)
        .endDate(req.endDate)
        .taxInvoiceDay(req.taxInvoiceDay)
        .paymentDueDay(req.paymentDueDay)
        .advancePayment(nvl(req.advancePayment))
        .monthlyRent(monthlyRent)
        .billingDay(req.billingDay)
        .billingCount(billingCount)
        .totalRent(totalRent)
        .deposit(nvl(req.deposit))
        .maturityOption(req.maturityOption)
        .residualValue(nvl(req.residualValue))
        .vehicleInsurance(req.vehicleInsurance)
        .insuranceAge(req.insuranceAge)
        .vehicleInsuranceLimit(req.vehicleInsuranceLimit)
        .vehicleDeductible(req.vehicleDeductible)
        .propertyLiability(req.propertyLiability)
        .propertyDeductible(req.propertyDeductible)
        .personalDeductible(req.personalDeductible)
        .passengerDeductible(req.passengerDeductible)
        .remarks(req.remarks)
        .build();

    if (c.getBillingCount() == null) c.setBillingCount(0);
    if (c.getTotalRent() == null) c.setTotalRent(0L);
    if (c.getAdvancePayment() == null) c.setAdvancePayment(0L);
    if (c.getDeposit() == null) c.setDeposit(0L);
    if (c.getResidualValue() == null) c.setResidualValue(0L);

    contractRepo.save(c);
    scheduleAutoGen.ensureGenerated(c);

    return toResponse(c);
  }

  @Transactional
  public ContractResponse update(Long id, ContractRequest req) {
    Contract c = contractRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("계약 없음 id=" + id));

    if (req.customerNumber != null && !req.customerNumber.isBlank()) {
      c.setCustomerNumber(req.customerNumber);
      c.setCustomer(customerRepo.findByCustomerNumber(req.customerNumber).orElse(null));
    }

    if (req.vehicleNo != null && !req.vehicleNo.isBlank()) {
      c.setVehicleNo(req.vehicleNo);
      VehicleOrder vo = vehicleOrderRepo.findByVehicleNoNormalized(req.vehicleNo).orElse(null);
      c.setVehicleOrder(vo);

      if (req.vehicleModel == null || req.vehicleModel.isBlank()) {
        if (vo != null) c.setVehicleModel(vo.getCarModel());
      }
    }

    if (req.vehicleModel != null) c.setVehicleModel(req.vehicleModel);

    if (req.contractType != null && !req.contractType.isBlank()) c.setContractType(req.contractType);
    if (req.contractCategory != null && !req.contractCategory.isBlank()) c.setContractCategory(req.contractCategory);
    if (req.status != null && !req.status.isBlank()) c.setStatus(req.status.trim());

    if (req.startDate != null) c.setStartDate(req.startDate);
    if (req.endDate != null) c.setEndDate(req.endDate);

    c.setTaxInvoiceDay(req.taxInvoiceDay);
    c.setPaymentDueDay(req.paymentDueDay);

    if (req.billingCount == null || req.billingCount <= 0) {
      throw new RuntimeException("청구횟수는 1 이상이어야 합니다. 청구횟수가 0이면 청구 스케줄이 생성되지 않아 청구생성에서 누락됩니다.");
    }

    c.setAdvancePayment(nvl(req.advancePayment));
    c.setMonthlyRent(nvl(req.monthlyRent));
    c.setBillingDay(req.billingDay);
    c.setBillingCount(req.billingCount);
    c.setTotalRent(req.totalRent != null ? req.totalRent : c.getMonthlyRent() * c.getBillingCount());

    c.setDeposit(nvl(req.deposit));
    c.setMaturityOption(req.maturityOption);
    c.setResidualValue(nvl(req.residualValue));

    c.setVehicleInsurance(req.vehicleInsurance);
    c.setInsuranceAge(req.insuranceAge);
    c.setVehicleInsuranceLimit(req.vehicleInsuranceLimit);
    c.setVehicleDeductible(req.vehicleDeductible);

    c.setPropertyLiability(req.propertyLiability);
    c.setPropertyDeductible(req.propertyDeductible);
    c.setPersonalDeductible(req.personalDeductible);
    c.setPassengerDeductible(req.passengerDeductible);

    c.setRemarks(req.remarks);

    contractRepo.save(c);
    scheduleAutoGen.ensureGenerated(c);

    return toResponse(c);
  }

  @Transactional
  public void delete(Long id) {
    if (!contractRepo.existsById(id)) {
      throw new RuntimeException("계약 없음 id=" + id);
    }
    contractRepo.deleteById(id);
  }

  private void validateRequired(ContractRequest req) {
    if (req.customerNumber == null || req.customerNumber.isBlank()) throw new RuntimeException("고객번호(customerNumber) 필수");
    if (req.vehicleNo == null || req.vehicleNo.isBlank()) throw new RuntimeException("차량번호(vehicleNo) 필수");
    if (req.contractType == null || req.contractType.isBlank()) throw new RuntimeException("계약구분 필수");
    if (req.contractCategory == null || req.contractCategory.isBlank()) throw new RuntimeException("계약유형 필수");
    if (req.startDate == null) throw new RuntimeException("계약시작일 필수");
    if (req.endDate == null) throw new RuntimeException("계약종료일 필수");
    if (req.billingCount == null || req.billingCount <= 0) {
      throw new RuntimeException("청구횟수는 1 이상이어야 합니다. 청구횟수가 0이면 청구 스케줄이 생성되지 않아 청구생성에서 누락됩니다.");
    }
  }

  private Long nvl(Long v) {
    return v == null ? 0L : v;
  }

  private String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      return "대기";
    }
    return status.trim();
  }

  private ContractResponse toResponse(Contract c) {
    String customerName = null;
    if (c.getCustomer() != null) {
      customerName = c.getCustomer().getCustomerName();
    }

    return ContractResponse.builder()
        .id(c.getId())
        .contractNumber(c.getContractNumber())
        .customerNumber(c.getCustomerNumber())
        .customerName(customerName)
        .vehicleNo(c.getVehicleNo())
        .vehicleModel(c.getVehicleModel())
        .contractType(c.getContractType())
        .contractCategory(c.getContractCategory())
        .status(c.getStatus())
        .startDate(c.getStartDate())
        .endDate(c.getEndDate())
        .billingCount(c.getBillingCount())
        .monthlyRent(c.getMonthlyRent())
        .build();
  }

  private ContractFullResponse toFullResponse(Contract c) {
    String customerName = null;
    String customerPhone = null;
    String customerAddress = null;
    String customerRegNo = null;

    if (c.getCustomer() != null) {
      customerName = c.getCustomer().getCustomerName();
      customerPhone = c.getCustomer().getPhone();
      customerAddress = c.getCustomer().getAddress();
      customerRegNo = c.getCustomer().getRegistrationNumber();
    }

    return ContractFullResponse.builder()
        .id(c.getId())
        .contractNumber(c.getContractNumber())
        .customerNumber(c.getCustomerNumber())
        .customerName(customerName)
        .customerPhone(customerPhone)
        .customerAddress(customerAddress)
        .customerRegistrationNumber(customerRegNo)
        .vehicleNo(c.getVehicleNo())
        .vehicleModel(c.getVehicleModel())
        .contractType(c.getContractType())
        .contractCategory(c.getContractCategory())
        .status(c.getStatus())
        .startDate(c.getStartDate())
        .endDate(c.getEndDate())
        .taxInvoiceDay(c.getTaxInvoiceDay())
        .paymentDueDay(c.getPaymentDueDay())
        .advancePayment(nvl(c.getAdvancePayment()))
        .monthlyRent(nvl(c.getMonthlyRent()))
        .billingDay(c.getBillingDay())
        .billingCount(c.getBillingCount() == null ? 0 : c.getBillingCount())
        .totalRent(nvl(c.getTotalRent()))
        .deposit(nvl(c.getDeposit()))
        .maturityOption(c.getMaturityOption())
        .residualValue(nvl(c.getResidualValue()))
        .vehicleInsurance(c.getVehicleInsurance())
        .insuranceAge(c.getInsuranceAge())
        .vehicleInsuranceLimit(c.getVehicleInsuranceLimit())
        .vehicleDeductible(c.getVehicleDeductible())
        .propertyLiability(c.getPropertyLiability())
        .propertyDeductible(c.getPropertyDeductible())
        .personalDeductible(c.getPersonalDeductible())
        .passengerDeductible(c.getPassengerDeductible())
        .remarks(c.getRemarks())
        .build();
  }
}