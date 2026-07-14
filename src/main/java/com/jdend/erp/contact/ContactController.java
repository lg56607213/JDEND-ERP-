package com.jdend.erp.contact;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public Map<String, Object> submit(@RequestBody ContactRequest req) {
        if (req.getCompanyName() == null || req.getCompanyName().isBlank()) {
            return Map.of("success", false, "message", "회사명을 입력해주세요.");
        }
        if (req.getContactName() == null || req.getContactName().isBlank()) {
            return Map.of("success", false, "message", "담당자명을 입력해주세요.");
        }
        if (req.getPhone() == null || req.getPhone().isBlank()) {
            return Map.of("success", false, "message", "연락처를 입력해주세요.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return Map.of("success", false, "message", "문의 내용을 입력해주세요.");
        }

        contactService.send(req);
        return Map.of("success", true, "message", "문의가 접수되었습니다. 빠른 시일 내 연락드리겠습니다.");
    }
}
