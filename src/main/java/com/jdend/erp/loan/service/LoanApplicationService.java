package com.jdend.erp.loan.service;

import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantContext;
import com.jdend.erp.loan.dto.LoanApplicationDtos.*;
import com.jdend.erp.loan.entity.LoanApplication;
import com.jdend.erp.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 대출(할부·리스) 신청.
 *
 * <p>신청 데이터는 auth DB에 있으므로 호출마다 TenantContext를 auth로 맞춘다.
 * 회사 DB와 트랜잭션이 섞이지 않도록 클래스에 @Transactional을 두지 않는다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private static final String AUTH_DB = "auth";
    private static final Set<String> FINANCE_TYPES = Set.of("할부", "리스");
    private static final Set<Integer> DOWN_PAYMENTS = Set.of(10, 20, 30);
    private static final Set<Integer> TERMS = Set.of(36, 48, 60);
    private static final Set<String> STATUSES = Set.of(
            LoanApplication.STATUS_NEW, LoanApplication.STATUS_CONTACTED,
            LoanApplication.STATUS_DONE, LoanApplication.STATUS_CANCELED);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LoanApplicationRepository repo;
    private final LoginUserRepository loginUserRepository;
    private final JavaMailSender mailSender;

    @Value("${admin.email:${app.contact.recipient-email:}}")
    private String adminEmail;

    // ── 고객(공개) ──────────────────────────────────────────

    /** 신청 페이지 상단에 업체명을 보여주기 위한 조회. 없는 회사코드면 예외. */
    public PublicCompanyResponse publicCompany(String companyCode) {
        LoginUser company = findCompany(companyCode);
        return PublicCompanyResponse.builder()
                .companyCode(company.getLoginId())
                .companyName(company.getCompanyName())
                .build();
    }

    public SubmitResponse submit(SubmitRequest req, String ip) {
        if (req == null) throw new IllegalArgumentException("신청 내용이 없습니다.");
        if (!req.isAgreed()) {
            throw new IllegalArgumentException("개인정보 수집·이용 동의가 필요합니다.");
        }

        LoginUser company = findCompany(req.getCompanyCode());

        String phone = trim(req.getContactPhone());
        if (phone.isBlank()) {
            throw new IllegalArgumentException("연락받으실 전화번호를 입력해 주세요.");
        }
        if (!phone.matches("^[0-9\\-+ ]{8,20}$")) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }

        boolean phoneOnly = LoanApplication.TYPE_PHONE.equalsIgnoreCase(trim(req.getInquiryType()));

        LoanApplication.LoanApplicationBuilder b = LoanApplication.builder()
                .companyCode(company.getLoginId())
                .inquiryType(phoneOnly ? LoanApplication.TYPE_PHONE : LoanApplication.TYPE_FORM)
                .applicantName(cut(trim(req.getApplicantName()), 50))
                .contactPhone(phone)
                .memo(cut(trim(req.getMemo()), 500))
                .status(LoanApplication.STATUS_NEW)
                .submitIp(ip);

        if (!phoneOnly) {
            String carModel = trim(req.getCarModel());
            if (carModel.isBlank()) {
                throw new IllegalArgumentException("차종을 입력해 주세요.");
            }
            String financeType = trim(req.getFinanceType());
            if (!FINANCE_TYPES.contains(financeType)) {
                throw new IllegalArgumentException("할부 또는 리스를 선택해 주세요.");
            }
            if (req.getDownPaymentPercent() == null || !DOWN_PAYMENTS.contains(req.getDownPaymentPercent())) {
                throw new IllegalArgumentException("희망 선납금을 선택해 주세요.");
            }
            if (req.getTermMonths() == null || !TERMS.contains(req.getTermMonths())) {
                throw new IllegalArgumentException("희망 신청기간을 선택해 주세요.");
            }
            b.carModel(cut(carModel, 100))
             .expectedDelivery(cut(trim(req.getExpectedDelivery()), 50))
             .financeType(financeType)
             .downPaymentPercent(req.getDownPaymentPercent())
             .termMonths(req.getTermMonths());
        }

        LoanApplication saved = inAuth(() -> repo.save(b.build()));

        notifyAdmin(saved, company.getCompanyName());

        return SubmitResponse.builder()
                .id(saved.getId())
                .message(phoneOnly
                        ? "접수되었습니다. 담당자가 남겨주신 번호로 연락드리겠습니다."
                        : "신청이 접수되었습니다. 담당자가 확인 후 연락드리겠습니다.")
                .build();
    }

    // ── 직원(ERP) ───────────────────────────────────────────

    public List<RowResponse> search(String companyCode, String status, LocalDate from, LocalDate to) {
        String st = status == null ? "" : status.trim();
        LocalDateTime f = from == null ? null : from.atStartOfDay();
        LocalDateTime t = to == null ? null : to.atTime(23, 59, 59);
        return inAuth(() -> repo.search(companyCode, st, f, t)).stream()
                .map(RowResponse::from)
                .toList();
    }

    public SummaryResponse summary(String companyCode, String baseUrl) {
        long n = inAuth(() -> repo.countByCompanyCodeAndStatus(companyCode, LoanApplication.STATUS_NEW));
        long c = inAuth(() -> repo.countByCompanyCodeAndStatus(companyCode, LoanApplication.STATUS_CONTACTED));
        long d = inAuth(() -> repo.countByCompanyCodeAndStatus(companyCode, LoanApplication.STATUS_DONE));
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return SummaryResponse.builder()
                .newCount(n).contactedCount(c).doneCount(d)
                .applyUrl(base + "/loan-apply.html?c=" + companyCode)
                .build();
    }

    public RowResponse update(Long id, String companyCode, UpdateRequest req) {
        LoanApplication a = inAuth(() -> repo.findById(id).orElse(null));
        if (a == null || !a.getCompanyCode().equals(companyCode)) {
            throw new IllegalArgumentException("신청을 찾을 수 없습니다.");
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            if (!STATUSES.contains(req.getStatus())) {
                throw new IllegalArgumentException("알 수 없는 상태입니다: " + req.getStatus());
            }
            a.setStatus(req.getStatus());
        }
        if (req.getAdminMemo() != null) {
            a.setAdminMemo(cut(req.getAdminMemo().trim(), 500));
        }
        return RowResponse.from(inAuth(() -> repo.save(a)));
    }

    // ── 내부 ────────────────────────────────────────────────

    private LoginUser findCompany(String companyCode) {
        String code = trim(companyCode);
        if (code.isBlank()) {
            throw new IllegalArgumentException("신청 링크가 올바르지 않습니다.");
        }
        LoginUser company = inAuth(() -> loginUserRepository.findByLoginId(code).orElse(null));
        if (company == null || Boolean.FALSE.equals(company.getIsActive())) {
            throw new IllegalArgumentException("신청 링크가 올바르지 않습니다.");
        }
        return company;
    }

    /** 신청이 들어오면 담당자에게 메일로 알린다. 실패해도 접수는 정상 처리한다. */
    private void notifyAdmin(LoanApplication a, String companyName) {
        try {
            if (adminEmail == null || adminEmail.isBlank()) {
                log.warn("[LoanApply] 수신 메일이 설정되지 않아 알림을 건너뜁니다. id={}", a.getId());
                return;
            }
            boolean phoneOnly = LoanApplication.TYPE_PHONE.equals(a.getInquiryType());

            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(adminEmail);
            m.setSubject("[" + companyName + "] " + (phoneOnly ? "유선문의 요청" : "대출신청") + " 접수 - "
                    + (a.getApplicantName() == null || a.getApplicantName().isBlank()
                       ? a.getContactPhone() : a.getApplicantName()));

            StringBuilder sb = new StringBuilder();
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━\n")
              .append(phoneOnly ? "유선 문의 요청이 들어왔습니다.\n" : "새 대출신청이 접수되었습니다.\n")
              .append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
              .append("신청자: ").append(nvl(a.getApplicantName())).append("\n")
              .append("연락처: ").append(nvl(a.getContactPhone())).append("\n");

            if (!phoneOnly) {
                sb.append("차종: ").append(nvl(a.getCarModel())).append("\n")
                  .append("출고예상시기: ").append(nvl(a.getExpectedDelivery())).append("\n")
                  .append("구분: ").append(nvl(a.getFinanceType())).append("\n")
                  .append("희망 선납금: ").append(a.getDownPaymentPercent() == null ? "-"
                          : a.getDownPaymentPercent() + "%").append("\n")
                  .append("희망 기간: ").append(a.getTermMonths() == null ? "-"
                          : a.getTermMonths() + "개월").append("\n");
            }
            if (a.getMemo() != null && !a.getMemo().isBlank()) {
                sb.append("요청사항: ").append(a.getMemo()).append("\n");
            }
            sb.append("접수일시: ")
              .append(a.getCreatedAt() == null ? LocalDateTime.now().format(TS) : a.getCreatedAt().format(TS))
              .append("\n\n▶ ERP > 대출신청 메뉴에서 확인하세요.\n")
              .append("https://rentcarerp.com\n")
              .append("━━━━━━━━━━━━━━━━━━━━━━━━");

            m.setText(sb.toString());
            mailSender.send(m);
            log.info("[LoanApply] 알림 메일 발송: id={}, to={}", a.getId(), adminEmail);
        } catch (Exception e) {
            log.error("[LoanApply] 알림 메일 실패 (접수는 정상): {}", e.getMessage());
        }
    }

    private <T> T inAuth(Supplier<T> action) {
        String prev = TenantContext.getCurrentDb();
        TenantContext.setCurrentDb(AUTH_DB);
        try {
            return action.get();
        } finally {
            if (prev == null) TenantContext.clear();
            else TenantContext.setCurrentDb(prev);
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String nvl(String s) { return s == null || s.isBlank() ? "-" : s; }
    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
