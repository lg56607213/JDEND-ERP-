package com.jdend.erp.contact;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.recipient-email}")
    private String recipientEmail;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void send(ContactRequest req) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("[JDEND ERP 문의] " + req.getCategory() + " - " + req.getCompanyName());

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String body = """
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    JDEND ERP 문의가 접수되었습니다
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                    📅 접수 일시  : %s
                    🏢 회사명     : %s
                    👤 담당자     : %s
                    📞 연락처     : %s
                    📧 이메일     : %s
                    📋 문의 유형  : %s

                    ─────────────────────────────
                    문의 내용
                    ─────────────────────────────
                    %s

                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    이 메일은 JDEND ERP 문의 양식을 통해 자동 발송되었습니다.
                    답변 시 위 이메일(%s)로 회신해 주세요.
                    """.formatted(
                    now,
                    req.getCompanyName(),
                    req.getContactName(),
                    req.getPhone(),
                    req.getEmail(),
                    req.getCategory(),
                    req.getContent(),
                    req.getEmail()
            );

            helper.setText(body, false);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
