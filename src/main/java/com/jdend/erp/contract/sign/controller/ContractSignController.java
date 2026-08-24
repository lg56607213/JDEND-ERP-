package com.jdend.erp.contract.sign.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.EncodeHintType;
import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.contract.sign.dto.ContractSignDtos.*;
import com.jdend.erp.contract.sign.service.ContractSignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 계약서 전자서명.
 *
 * <p>/api/contract-sign/** 은 고객이 로그인 없이 여는 공개 경로다.
 * 토큰 자체가 인증 수단이므로 다른 계약 정보는 절대 노출하지 않는다.</p>
 */
@RestController
@RequiredArgsConstructor
public class ContractSignController {

    private final ContractSignService service;

    // ── 직원용 (로그인 필요) ─────────────────────────────────

    @PostMapping("/api/contracts/{contractNumber}/sign-request")
    public CreateResponse createRequest(@PathVariable String contractNumber,
                                        HttpServletRequest http,
                                        HttpSession session) {
        String tenantDb = (String) session.getAttribute(AuthService.SESSION_TARGET_DB);
        String loginId = (String) session.getAttribute(AuthService.SESSION_LOGIN_ID);
        return service.createRequest(contractNumber, tenantDb, loginId, baseUrl(http));
    }

    @GetMapping("/api/contracts/{contractNumber}/sign-status")
    public StatusResponse status(@PathVariable String contractNumber, HttpSession session) {
        String tenantDb = (String) session.getAttribute(AuthService.SESSION_TARGET_DB);
        return service.latestStatus(contractNumber, tenantDb);
    }

    // ── 고객용 (공개) ───────────────────────────────────────

    @GetMapping("/api/contract-sign/summary")
    public ContractSummary summary(@RequestParam("t") String token) {
        return service.loadSummary(token);
    }

    @PostMapping("/api/contract-sign/submit")
    public SubmitResponse submit(@RequestParam("t") String token,
                                 @RequestBody SubmitRequest body,
                                 HttpServletRequest http) {
        return service.submit(token, body, clientIp(http), http.getHeader("User-Agent"));
    }

    @GetMapping(value = "/api/contract-sign/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@RequestParam("t") String token, HttpServletRequest http) {
        String url = baseUrl(http) + "/sign.html?t=" + token;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 260, 260,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                           EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR 생성 실패: " + e.getMessage(), e);
        }
    }

    // ── 내부 ────────────────────────────────────────────────

    /** 리버스 프록시 뒤에 있을 수 있으므로 X-Forwarded-* 를 우선 본다. */
    private String baseUrl(HttpServletRequest req) {
        String proto = header(req, "X-Forwarded-Proto");
        String host = header(req, "X-Forwarded-Host");
        if (host != null) {
            return (proto != null ? proto : "https") + "://" + host;
        }
        String scheme = req.getScheme();
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + req.getServerName() + (defaultPort ? "" : ":" + port);
    }

    private String clientIp(HttpServletRequest req) {
        String forwarded = header(req, "X-Forwarded-For");
        if (forwarded != null) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded;
        }
        return req.getRemoteAddr();
    }

    private String header(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
