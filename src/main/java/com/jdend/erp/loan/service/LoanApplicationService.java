package com.jdend.erp.loan.service;

import com.jdend.erp.config.TenantContext;
import com.jdend.erp.loan.dto.LoanApplicationDtos.*;
import com.jdend.erp.loan.entity.LoanApplication;
import com.jdend.erp.loan.repository.LoanApplicationRepository;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
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
 * 영업용 차량 증차 자금 신청.
 *
 * <p>ERP를 쓰는 렌터카 업체가 직접 신청하고, 운영자가 접수해 제휴 금융사에 알선한다.
 * 신청 데이터는 auth DB에 있고 보유 차량 수는 업체 DB에 있으므로 호출마다
 * TenantContext를 맞춘다. 그래서 클래스에 @Transactional을 두지 않는다.</p>
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
            LoanApplication.STATUS_NEW, LoanApplication.STATUS_REVIEWING,
            LoanApplication.STATUS_DONE, LoanApplication.STATUS_REJECTED,
            LoanApplication.STATUS_CANCELED);
    private static final Set<String> OPEN_STATUSES = Set.of(
            LoanApplication.STATUS_NEW, LoanApplication.STATUS_REVIEWING);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LoanApplicationRepository repo;
    private final VehicleOrderRepository vehicleOrderRepository;
    private final JavaMailSender mailSender;

    @Value("${admin.email:${app.contact.recipient-email:}}")
    private String adminEmail;

    // ── 신청 업체 ───────────────────────────────────────────

    /** 신청 화면 상단에 보여줄 우리 회사 현황 (ERP에 이미 있는 값) */
    public MyCompanyResponse myCompany(Long companyId, String companyName, String userLoginId) {
        long vehicles = 0L;
        try {
            vehicles = vehicleOrderRepository.count();   // 현재 세션의 업체 DB 기준
        } catch (Exception e) {
            log.warn("[LoanApply] 차량 수 조회 실패: {}", e.getMessage());
        }
        long open = inAuth(() -> repo.findByCompanyIdOrderByIdDesc(companyId)).stream()
                .filter(a -> OPEN_STATUSES.contains(a.getStatus()))
                .count();

        return MyCompanyResponse.builder()
                .companyName(companyName)
                .managerName(userLoginId)
                .contactPhone("")
                .vehicleCount(vehicles)
                .openApplications(open)
                .build();
    }

    public SubmitResponse submit(SubmitRequest req, Long companyId, String companyName, String userLoginId) {
        if (companyId == null) {
            throw new IllegalArgumentException("업체 계정으로 로그인해야 신청할 수 있습니다.");
        }
        if (req == null) throw new IllegalArgumentException("신청 내용이 없습니다.");
        if (!req.isAgreed()) {
            throw new IllegalArgumentException("제휴 금융사 정보 제공에 동의해야 신청할 수 있습니다.");
        }

        String phone = trim(req.getContactPhone());
        if (phone.isBlank()) {
            throw new IllegalArgumentException("연락받을 번호를 입력해 주세요.");
        }
        if (!phone.matches("^[0-9\\-+ ]{8,20}$")) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }

        boolean phoneOnly = LoanApplication.TYPE_PHONE.equalsIgnoreCase(trim(req.getInquiryType()));

        LoanApplication.LoanApplicationBuilder b = LoanApplication.builder()
                .companyId(companyId)
                .companyName(cut(companyName, 100))
                .requestedBy(cut(userLoginId, 50))
                .inquiryType(phoneOnly ? LoanApplication.TYPE_PHONE : LoanApplication.TYPE_FORM)
                .managerName(cut(trim(req.getManagerName()), 50))
                .contactPhone(phone)
                .memo(cut(trim(req.getMemo()), 500))
                .status(LoanApplication.STATUS_NEW);

        if (!phoneOnly) {
            String carModel = trim(req.getCarModel());
            if (carModel.isBlank()) {
                throw new IllegalArgumentException("증차할 차종을 입력해 주세요.");
            }
            if (req.getVehicleCount() != null && (req.getVehicleCount() < 1 || req.getVehicleCount() > 999)) {
                throw new IllegalArgumentException("증차 대수는 1~999 사이로 입력해 주세요.");
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
             .vehicleCount(req.getVehicleCount())
             .expectedDelivery(cut(trim(req.getExpectedDelivery()), 50))
             .financeType(financeType)
             .downPaymentPercent(req.getDownPaymentPercent())
             .termMonths(req.getTermMonths());
        }

        LoanApplication saved = inAuth(() -> repo.save(b.build()));
        notifyOperator(saved);

        return SubmitResponse.builder()
                .id(saved.getId())
                .message(phoneOnly
                        ? "상담 요청이 접수되었습니다. 남겨주신 번호로 연락드리겠습니다."
                        : "신청이 접수되었습니다. 제휴 금융사 조건을 확인한 뒤 연락드리겠습니다.")
                .build();
    }

    public List<MyRowResponse> myApplications(Long companyId) {
        return inAuth(() -> repo.findByCompanyIdOrderByIdDesc(companyId)).stream()
                .map(MyRowResponse::from)
                .toList();
    }

    /** 업체가 아직 접수 단계인 자기 신청을 취소한다. */
    public MyRowResponse cancelMine(Long id, Long companyId) {
        LoanApplication a = inAuth(() -> repo.findById(id).orElse(null));
        if (a == null || !a.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("신청을 찾을 수 없습니다.");
        }
        if (!LoanApplication.STATUS_NEW.equals(a.getStatus())) {
            throw new IllegalArgumentException("이미 처리가 시작된 신청은 취소할 수 없습니다. 담당자에게 문의해 주세요.");
        }
        a.setStatus(LoanApplication.STATUS_CANCELED);
        return MyRowResponse.from(inAuth(() -> repo.save(a)));
    }

    // ── 운영자 ──────────────────────────────────────────────

    public List<AdminRowResponse> adminSearch(String status, LocalDate from, LocalDate to) {
        String st = status == null ? "" : status.trim();
        LocalDateTime f = from == null ? null : from.atStartOfDay();
        LocalDateTime t = to == null ? null : to.atTime(23, 59, 59);
        return inAuth(() -> repo.searchAll(st, f, t)).stream()
                .map(AdminRowResponse::from)
                .toList();
    }

    public AdminSummaryResponse adminSummary() {
        return AdminSummaryResponse.builder()
                .newCount(inAuth(() -> repo.countByStatus(LoanApplication.STATUS_NEW)))
                .reviewingCount(inAuth(() -> repo.countByStatus(LoanApplication.STATUS_REVIEWING)))
                .doneCount(inAuth(() -> repo.countByStatus(LoanApplication.STATUS_DONE)))
                .build();
    }

    public AdminRowResponse adminUpdate(Long id, UpdateRequest req) {
        LoanApplication a = inAuth(() -> repo.findById(id).orElse(null));
        if (a == null) throw new IllegalArgumentException("신청을 찾을 수 없습니다.");

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            if (!STATUSES.contains(req.getStatus())) {
                throw new IllegalArgumentException("알 수 없는 상태입니다: " + req.getStatus());
            }
            a.setStatus(req.getStatus());
        }
        if (req.getAdminMemo() != null)    a.setAdminMemo(cut(req.getAdminMemo().trim(), 500));
        if (req.getReplyMessage() != null) a.setReplyMessage(cut(req.getReplyMessage().trim(), 500));

        return AdminRowResponse.from(inAuth(() -> repo.save(a)));
    }

    // ── 내부 ────────────────────────────────────────────────

    /** 신청이 들어오면 운영자에게 메일로 알린다. 실패해도 접수는 정상 처리한다. */
    private void notifyOperator(LoanApplication a) {
        try {
            if (adminEmail == null || adminEmail.isBlank()) {
                log.warn("[LoanApply] 수신 메일이 설정되지 않아 알림을 건너뜁니다. id={}", a.getId());
                return;
            }
            boolean phoneOnly = LoanApplication.TYPE_PHONE.equals(a.getInquiryType());

            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(adminEmail);
            m.setSubject("[" + nvl(a.getCompanyName()) + "] 차량 증차 "
                    + (phoneOnly ? "상담 요청" : "자금 신청"));

            StringBuilder sb = new StringBuilder();
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━\n")
              .append(phoneOnly ? "증차 자금 상담 요청이 들어왔습니다.\n"
                                : "차량 증차 자금 신청이 접수되었습니다.\n")
              .append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
              .append("업체명: ").append(nvl(a.getCompanyName())).append("\n")
              .append("담당자: ").append(nvl(a.getManagerName())).append("\n")
              .append("연락처: ").append(nvl(a.getContactPhone())).append("\n")
              .append("신청자 계정: ").append(nvl(a.getRequestedBy())).append("\n");

            if (!phoneOnly) {
                sb.append("\n[신청 조건]\n")
                  .append("차종: ").append(nvl(a.getCarModel())).append("\n")
                  .append("증차 대수: ").append(a.getVehicleCount() == null ? "-"
                          : a.getVehicleCount() + "대").append("\n")
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
            sb.append("\n접수일시: ")
              .append(a.getCreatedAt() == null ? LocalDateTime.now().format(TS) : a.getCreatedAt().format(TS))
              .append("\n\n▶ ERP > 금융 > 대출신청 관리에서 확인하세요.\n")
              .append("https://rentcarerp.com\n")
              .append("━━━━━━━━━━━━━━━━━━━━━━━━");

            m.setText(sb.toString());
            mailSender.send(m);
            log.info("[LoanApply] 알림 메일 발송: id={}, company={}", a.getId(), a.getCompanyName());
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
