package com.jdend.erp.auth.controller;

import com.jdend.erp.auth.dto.CompanyApplicationRequest;
import com.jdend.erp.auth.dto.CompanyApplicationResponse;
import com.jdend.erp.auth.service.CompanyApplicationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company-applications")
public class CompanyApplicationController {

    private final CompanyApplicationService service;

    /**
     * 무료체험 신청 접수 (공개 - 로그인 불필요).
     * SessionCompanyFilter 는 DB 컨텍스트 설정만 하고 경로를 차단하지 않으므로
     * 별도 whitelist 추가 없이 인증 없이 접근 가능.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody CompanyApplicationRequest req) {
        service.create(req);
        return ResponseEntity.ok(Map.of(
                "message", "신청이 완료되었습니다. 담당자가 1-2 영업일 내 연락드리겠습니다."
        ));
    }

    /** 전체 신청 목록 조회 (PLATFORM_ADMIN만) */
    @GetMapping
    public List<CompanyApplicationResponse> list(HttpSession session) {
        return service.list(session);
    }

    /** 신청 승인 (PLATFORM_ADMIN만) */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable Long id,
            HttpSession session
    ) {
        service.approve(id, session);
        return ResponseEntity.ok(Map.of("message", "계정이 생성되고 이메일이 발송되었습니다."));
    }

    /** 신청 거절 (PLATFORM_ADMIN만) */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable Long id,
            HttpSession session
    ) {
        service.reject(id, session);
        return ResponseEntity.ok(Map.of("message", "거절 처리되었습니다."));
    }
}
