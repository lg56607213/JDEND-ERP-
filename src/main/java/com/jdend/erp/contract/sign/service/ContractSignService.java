package com.jdend.erp.contract.sign.service;

import com.jdend.erp.common.pdf.PdfRenderer;
import com.jdend.erp.config.TenantContext;
import com.jdend.erp.contract.dto.ContractFullResponse;
import com.jdend.erp.contract.service.ContractService;
import com.jdend.erp.contract.sign.dto.ContractSignDtos.*;
import com.jdend.erp.contract.sign.entity.ContractSignRequest;
import com.jdend.erp.contract.sign.repository.ContractSignRequestRepository;
import com.jdend.erp.document.service.VehicleDocumentService;
import com.jdend.erp.myinfo.dto.SupplierInfoResponse;
import com.jdend.erp.myinfo.service.MyInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

/**
 * 계약서 전자서명.
 *
 * <p>서명 요청 정보는 auth DB에, 계약/문서는 회사 DB에 있으므로 이 서비스는
 * 호출마다 {@link TenantContext}를 명시적으로 바꿔가며 동작한다. 그래서
 * <b>클래스에 @Transactional을 붙이지 않는다</b> — 하나의 트랜잭션이 두 DB에
 * 걸치면 커넥션이 고정돼 라우팅이 동작하지 않는다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignService {

    private static final String AUTH_DB = "auth";
    private static final String DOCUMENT_TYPE = "계약서";
    private static final int MAX_SIGNATURE_BYTES = 2 * 1024 * 1024;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContractSignRequestRepository signRepo;
    private final ContractService contractService;
    private final MyInfoService myInfoService;
    private final VehicleDocumentService documentService;
    private final PdfRenderer pdfRenderer;

    @Value("${app.sign.link-valid-minutes:30}")
    private int validMinutes;

    private final SecureRandom random = new SecureRandom();

    // ── 직원: 서명 요청 생성 ────────────────────────────────

    public CreateResponse createRequest(String contractNumber, String tenantDb,
                                        String createdBy, String baseUrl) {
        if (contractNumber == null || contractNumber.isBlank()) {
            throw new IllegalArgumentException("계약번호가 필요합니다.");
        }
        if (tenantDb == null || tenantDb.isBlank() || AUTH_DB.equals(tenantDb)) {
            throw new IllegalArgumentException("회사 정보를 확인할 수 없습니다. 다시 로그인해 주세요.");
        }

        // 계약이 실제로 있는지 회사 DB에서 먼저 확인한다.
        ContractFullResponse contract = inDb(tenantDb, () -> contractService.detailFullByNumber(contractNumber));
        if (contract == null) {
            throw new IllegalArgumentException("계약을 찾을 수 없습니다: " + contractNumber);
        }

        String token = newToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(validMinutes);

        ContractSignRequest saved = inDb(AUTH_DB, () -> signRepo.save(
                ContractSignRequest.builder()
                        .tokenHash(sha256(token))
                        .tenantDb(tenantDb)
                        .contractNumber(contractNumber)
                        .customerName(contract.customerName)
                        .status(ContractSignRequest.STATUS_PENDING)
                        .expiresAt(expiresAt)
                        .createdBy(createdBy)
                        .build()));

        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return CreateResponse.builder()
                .token(token)
                .signUrl(base + "/sign.html?t=" + token)
                .qrUrl(base + "/api/contract-sign/qr?t=" + token)
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    /** 직원 화면에서 계약의 최근 서명 상태를 조회한다. */
    public StatusResponse latestStatus(String contractNumber, String tenantDb) {
        List<ContractSignRequest> list = inDb(AUTH_DB,
                () -> signRepo.findByTenantDbAndContractNumberOrderByIdDesc(tenantDb, contractNumber));
        if (list.isEmpty()) {
            return StatusResponse.builder().status("NONE").build();
        }
        ContractSignRequest r = list.get(0);
        String status = r.getStatus();
        if (ContractSignRequest.STATUS_PENDING.equals(status) && r.isExpired()) {
            status = "EXPIRED";
        }
        return StatusResponse.builder()
                .status(status)
                .expiresAt(r.getExpiresAt())
                .signedAt(r.getSignedAt())
                .signerName(r.getSignerName())
                .documentId(r.getDocumentId())
                .build();
    }

    // ── 고객: 서명 페이지 ───────────────────────────────────

    public ContractSummary loadSummary(String token) {
        ContractSignRequest req = requirePending(token);

        ContractFullResponse c = inDb(req.getTenantDb(),
                () -> contractService.detailFullByNumber(req.getContractNumber()));
        if (c == null) throw new IllegalArgumentException("계약 정보를 찾을 수 없습니다.");

        String lessor = inDb(req.getTenantDb(), this::lessorName);

        return ContractSummary.builder()
                .contractNumber(c.contractNumber)
                .customerName(c.customerName)
                .vehicleNo(c.vehicleNo)
                .vehicleModel(c.vehicleModel)
                .contractType(c.contractType)
                .startDate(str(c.startDate))
                .endDate(str(c.endDate))
                .monthlyRent(c.monthlyRent)
                .deposit(c.deposit)
                .advancePayment(c.advancePayment)
                .billingCount(c.billingCount)
                .lessorName(lessor)
                .expiresAt(req.getExpiresAt())
                .status(req.getStatus())
                .build();
    }

    public SubmitResponse submit(String token, SubmitRequest body, String ip, String userAgent) {
        ContractSignRequest req = requirePending(token);

        if (body == null || !body.isAgreed()) {
            throw new IllegalArgumentException("계약 내용 확인 및 동의가 필요합니다.");
        }
        String signerName = body.getSignerName() == null ? "" : body.getSignerName().trim();
        if (signerName.isBlank()) {
            throw new IllegalArgumentException("서명자 성명을 입력해 주세요.");
        }
        byte[] signaturePng = decodeSignature(body.getSignatureImage());

        String tenantDb = req.getTenantDb();
        ContractFullResponse c = inDb(tenantDb,
                () -> contractService.detailFullByNumber(req.getContractNumber()));
        if (c == null) throw new IllegalArgumentException("계약 정보를 찾을 수 없습니다.");

        LocalDateTime signedAt = LocalDateTime.now();
        String lessor = inDb(tenantDb, this::lessorName);
        String lessorAddress = inDb(tenantDb, this::lessorAddress);

        String xhtml = buildContractHtml(c, lessor, lessorAddress, signerName,
                signaturePng, signedAt, ip);
        byte[] pdf = pdfRenderer.render(xhtml);
        String documentHash = sha256(pdf);

        String fileName = "계약서_" + c.contractNumber + "_전자서명.pdf";
        Long documentId = inDb(tenantDb, () -> documentService.saveBytes(
                DOCUMENT_TYPE, c.vehicleNo, c.contractNumber, fileName, pdf, "전자서명").id());

        inDb(AUTH_DB, () -> {
            req.setStatus(ContractSignRequest.STATUS_SIGNED);
            req.setSignedAt(signedAt);
            req.setSignerName(signerName);
            req.setSignerIp(ip);
            req.setSignerUserAgent(trim(userAgent, 400));
            req.setDocumentHash(documentHash);
            req.setDocumentId(documentId);
            return signRepo.save(req);
        });

        log.info("[ContractSign] 서명 완료: contract={}, tenant={}, signer={}, docId={}",
                c.contractNumber, tenantDb, signerName, documentId);

        return SubmitResponse.builder()
                .contractNumber(c.contractNumber)
                .signedAt(signedAt)
                .documentHash(documentHash)
                .build();
    }

    // ── 내부 ────────────────────────────────────────────────

    private ContractSignRequest requirePending(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("서명 링크가 올바르지 않습니다.");
        }
        ContractSignRequest req = inDb(AUTH_DB, () -> signRepo.findByTokenHash(sha256(token)).orElse(null));
        if (req == null) {
            throw new IllegalArgumentException("서명 링크가 올바르지 않습니다.");
        }
        if (ContractSignRequest.STATUS_SIGNED.equals(req.getStatus())) {
            throw new IllegalStateException("이미 서명이 완료된 계약입니다.");
        }
        if (ContractSignRequest.STATUS_CANCELED.equals(req.getStatus())) {
            throw new IllegalStateException("취소된 서명 요청입니다.");
        }
        if (req.isExpired()) {
            throw new IllegalStateException("서명 링크가 만료되었습니다. 담당자에게 재발급을 요청해 주세요.");
        }
        return req;
    }

    private byte[] decodeSignature(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new IllegalArgumentException("서명을 입력해 주세요.");
        }
        String base64 = dataUrl;
        int comma = base64.indexOf(',');
        if (base64.startsWith("data:")) {
            if (!base64.startsWith("data:image/png")) {
                throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.");
            }
            base64 = comma >= 0 ? base64.substring(comma + 1) : "";
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("서명 이미지를 읽을 수 없습니다.");
        }
        if (bytes.length == 0) throw new IllegalArgumentException("서명을 입력해 주세요.");
        if (bytes.length > MAX_SIGNATURE_BYTES) {
            throw new IllegalArgumentException("서명 이미지가 너무 큽니다.");
        }
        return bytes;
    }

    private String lessorName() {
        SupplierInfoResponse s = myInfoService.getSupplierInfo();
        return s == null || s.getCompanyName() == null ? "" : s.getCompanyName();
    }

    private String lessorAddress() {
        SupplierInfoResponse s = myInfoService.getSupplierInfo();
        return s == null || s.getAddress() == null ? "" : s.getAddress();
    }

    /** TenantContext를 잠시 바꿔 실행하고 원래대로 되돌린다. */
    private <T> T inDb(String db, Supplier<T> action) {
        String prev = TenantContext.getCurrentDb();
        TenantContext.setCurrentDb(db);
        try {
            return action.get();
        } finally {
            if (prev == null) TenantContext.clear();
            else TenantContext.setCurrentDb(prev);
        }
    }

    private String newToken() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String sha256(String s) {
        return sha256(s.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] data) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("해시 생성 실패", e);
        }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String won(Long v) {
        return v == null ? "" : String.format("%,d", v);
    }

    // ── 계약서 XHTML (PDF 원본) ─────────────────────────────

    private String buildContractHtml(ContractFullResponse c, String lessor, String lessorAddress,
                                     String signerName, byte[] signaturePng,
                                     LocalDateTime signedAt, String ip) {
        String sig = Base64.getEncoder().encodeToString(signaturePng);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\" />")
          .append("<style>")
          .append("@page { size: A4; margin: 14mm; }")
          .append("body { font-family: 'NanumGothic'; font-size: 10px; color: #000; }")
          .append("h1 { font-size: 18px; text-align: center; margin: 0 0 4px 0; }")
          .append(".meta { text-align: right; font-size: 9px; margin-bottom: 6px; }")
          .append("table { width: 100%; border-collapse: collapse; table-layout: fixed; }")
          .append("td, th { border: 1px solid #222; padding: 4px 5px; word-wrap: break-word; }")
          .append("th { background: #eee; font-weight: bold; text-align: center; }")
          .append(".label { background: #f2f2f2; font-weight: bold; width: 18%; }")
          .append(".section { background: #ddd; font-weight: bold; text-align: center; }")
          .append(".sign-area { margin-top: 14px; }")
          .append(".sign-box { border: 1px solid #222; height: 78px; text-align: center; padding: 4px; }")
          .append(".sign-img { height: 58px; }")
          .append(".evidence { margin-top: 10px; font-size: 8px; color: #444; ")
          .append("border: 1px solid #999; padding: 6px; }")
          .append("</style></head><body>");

        sb.append("<h1>자동차 대여 계약서</h1>");
        sb.append("<div class=\"meta\">계약번호: ").append(PdfRenderer.esc(c.contractNumber)).append("</div>");

        sb.append("<table>");
        row(sb, "고객명", c.customerName, "연락처", c.customerPhone);
        row(sb, "사업자/주민번호", c.customerRegistrationNumber, "주소", c.customerAddress);
        sectionRow(sb, "차량 정보");
        row(sb, "차량번호", c.vehicleNo, "차종", c.vehicleModel);
        sectionRow(sb, "계약 구분");
        row(sb, "계약구분", c.contractType, "유형", c.contractCategory);
        row(sb, "시작일", str(c.startDate), "종료일", str(c.endDate));
        sectionRow(sb, "렌트료");
        row(sb, "선납금", won(c.advancePayment), "보증금", won(c.deposit));
        row(sb, "월 렌트료", won(c.monthlyRent), "청구 횟수",
                c.billingCount == null ? "" : String.valueOf(c.billingCount));
        row(sb, "총액", won(c.totalRent), "만기시", c.maturityOption);
        sectionRow(sb, "보험");
        row(sb, "자차보험", c.vehicleInsurance, "보험연령", c.insuranceAge);
        row(sb, "자차한도", c.vehicleInsuranceLimit, "자차면책", c.vehicleDeductible);
        row(sb, "대물", c.propertyLiability, "대물면책", c.propertyDeductible);
        row(sb, "대인면책", c.personalDeductible, "자손면책", c.passengerDeductible);
        sectionRow(sb, "기타사항");
        sb.append("<tr><td colspan=\"4\" style=\"height:46px; vertical-align:top;\">")
          .append(PdfRenderer.esc(c.remarks)).append("</td></tr>");
        sb.append("</table>");

        sb.append("<div class=\"sign-area\"><table>");
        sb.append("<tr><th style=\"width:50%\">임대인</th><th style=\"width:50%\">임차인</th></tr>");
        sb.append("<tr>");
        sb.append("<td class=\"sign-box\">").append(PdfRenderer.esc(lessor))
          .append("<div style=\"font-size:8px; margin-top:4px;\">")
          .append(PdfRenderer.esc(lessorAddress)).append("</div></td>");
        sb.append("<td class=\"sign-box\">").append(PdfRenderer.esc(signerName))
          .append("<div><img class=\"sign-img\" src=\"data:image/png;base64,").append(sig)
          .append("\" alt=\"signature\" /></div></td>");
        sb.append("</tr></table></div>");

        sb.append("<div class=\"evidence\">")
          .append("본 계약서는 전자문서 및 전자거래 기본법에 따른 전자문서로, 아래 정보로 서명 사실을 확인할 수 있습니다.<br />")
          .append("서명자: ").append(PdfRenderer.esc(signerName))
          .append(" &#183; 서명일시: ").append(signedAt.format(TS))
          .append(" &#183; 접속 IP: ").append(PdfRenderer.esc(ip))
          .append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private void row(StringBuilder sb, String l1, String v1, String l2, String v2) {
        sb.append("<tr><td class=\"label\">").append(PdfRenderer.esc(l1)).append("</td><td>")
          .append(PdfRenderer.esc(v1)).append("</td><td class=\"label\">")
          .append(PdfRenderer.esc(l2)).append("</td><td>")
          .append(PdfRenderer.esc(v2)).append("</td></tr>");
    }

    private void sectionRow(StringBuilder sb, String title) {
        sb.append("<tr><td class=\"section\" colspan=\"4\">")
          .append(PdfRenderer.esc(title)).append("</td></tr>");
    }
}
