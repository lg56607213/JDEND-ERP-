package com.jdend.erp.myinfo.service;

import com.jdend.erp.myinfo.dto.*;
import com.jdend.erp.myinfo.entity.BankAccount;
import com.jdend.erp.myinfo.entity.CorporateCard;
import com.jdend.erp.myinfo.repository.BankAccountRepository;
import com.jdend.erp.myinfo.repository.CorporateCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final BankAccountRepository bankRepo;
    private final CorporateCardRepository cardRepo;

    // ─── 통장 ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listBankAccounts() {
        return bankRepo.findAllByOrderByIdAsc().stream()
                .map(BankAccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listActiveBankAccounts() {
        return bankRepo.findByIsActiveTrueOrderByIdAsc().stream()
                .map(BankAccountResponse::from).toList();
    }

    @Transactional
    public BankAccountResponse createBankAccount(BankAccountRequest req) {
        BankAccount e = BankAccount.builder()
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountHolder(req.getAccountHolder())
                .accountAlias(req.getAccountAlias())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
        return BankAccountResponse.from(bankRepo.save(e));
    }

    @Transactional
    public BankAccountResponse updateBankAccount(Long id, BankAccountRequest req) {
        BankAccount e = bankRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("통장을 찾을 수 없습니다."));
        if (req.getBankName() != null) e.setBankName(req.getBankName());
        if (req.getAccountNumber() != null) e.setAccountNumber(req.getAccountNumber());
        if (req.getAccountHolder() != null) e.setAccountHolder(req.getAccountHolder());
        if (req.getAccountAlias() != null) e.setAccountAlias(req.getAccountAlias());
        if (req.getIsActive() != null) e.setIsActive(req.getIsActive());
        return BankAccountResponse.from(bankRepo.save(e));
    }

    @Transactional
    public void deleteBankAccount(Long id) {
        bankRepo.deleteById(id);
    }

    // ─── 법인카드 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CorporateCardResponse> listCorporateCards() {
        return cardRepo.findAllByOrderByIdAsc().stream()
                .map(CorporateCardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CorporateCardResponse> listActiveCorporateCards() {
        return cardRepo.findByIsActiveTrueOrderByIdAsc().stream()
                .map(CorporateCardResponse::from).toList();
    }

    @Transactional
    public CorporateCardResponse createCorporateCard(CorporateCardRequest req) {
        CorporateCard e = CorporateCard.builder()
                .cardCompany(req.getCardCompany())
                .cardNumberLast4(req.getCardNumberLast4())
                .cardHolder(req.getCardHolder())
                .cardAlias(req.getCardAlias())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
        return CorporateCardResponse.from(cardRepo.save(e));
    }

    @Transactional
    public CorporateCardResponse updateCorporateCard(Long id, CorporateCardRequest req) {
        CorporateCard e = cardRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("법인카드를 찾을 수 없습니다."));
        if (req.getCardCompany() != null) e.setCardCompany(req.getCardCompany());
        if (req.getCardNumberLast4() != null) e.setCardNumberLast4(req.getCardNumberLast4());
        if (req.getCardHolder() != null) e.setCardHolder(req.getCardHolder());
        if (req.getCardAlias() != null) e.setCardAlias(req.getCardAlias());
        if (req.getIsActive() != null) e.setIsActive(req.getIsActive());
        return CorporateCardResponse.from(cardRepo.save(e));
    }

    @Transactional
    public void deleteCorporateCard(Long id) {
        cardRepo.deleteById(id);
    }
}
