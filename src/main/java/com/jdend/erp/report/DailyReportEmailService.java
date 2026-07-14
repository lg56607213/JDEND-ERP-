package com.jdend.erp.report;

import com.jdend.erp.accounting.cash.dto.DailyFundReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    public void sendDailyReport(String companyName, LocalDate reportDate,
                                DailyFundReportResponse report, List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(String.format("[%s] 일일 자금일보 (%s)",
                    companyName,
                    reportDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))));
            helper.setText(buildHtml(companyName, reportDate, report), true);

            mailSender.send(message);
            log.info("[일일자금보고] 발송 완료 company={} date={} to={}",
                    companyName, reportDate, recipients);

        } catch (Exception e) {
            log.error("[일일자금보고] 발송 실패 company={} date={} error={}",
                    companyName, reportDate, e.getMessage());
        }
    }

    private String fmt(Long v) {
        return FMT.format(v == null ? 0 : v);
    }

    private String diffColor(Long v) {
        if (v == null || v == 0) return "#374151";
        return v > 0 ? "#16a34a" : "#dc2626";
    }

    private String buildHtml(String companyName, LocalDate reportDate, DailyFundReportResponse r) {
        String dateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN));

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html lang="ko">
            <head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f5f7fb;font-family:'맑은 고딕','Apple SD Gothic Neo',Arial,sans-serif;">
            <div style="max-width:640px;margin:24px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08);">

            <!-- 헤더 -->
            <div style="background:#1b253a;padding:24px 32px;">
              <div style="color:#94aac4;font-size:12px;letter-spacing:.08em;text-transform:uppercase;margin-bottom:4px;">JDEND ERP · 일일 자금일보</div>
              <div style="color:#fff;font-size:20px;font-weight:700;">""").append(companyName).append("""
              </div>
              <div style="color:#60a5fa;font-size:14px;margin-top:4px;">""").append(dateStr).append("""
              </div>
            </div>

            """);

        // 은행 입출금 요약
        sb.append("""
            <!-- 요약 카드 -->
            <div style="padding:24px 32px 0;">
              <div style="display:flex;gap:12px;flex-wrap:wrap;">
            """);

        long bankIn = r.getBankIncomeTotal() == null ? 0 : r.getBankIncomeTotal();
        long bankOut = r.getBankExpenseTotal() == null ? 0 : r.getBankExpenseTotal();
        long vIn = r.getVoucherIncomeTotal() == null ? 0 : r.getVoucherIncomeTotal();
        long vOut = r.getVoucherExpenseTotal() == null ? 0 : r.getVoucherExpenseTotal();
        long inDiff = r.getIncomeDiff() == null ? 0 : r.getIncomeDiff();
        long outDiff = r.getExpenseDiff() == null ? 0 : r.getExpenseDiff();

        sb.append(summaryCard("은행 입금", bankIn, "#1b253a"));
        sb.append(summaryCard("은행 출금", bankOut, "#1b253a"));
        sb.append(summaryCard("전표 입금", vIn, "#374151"));
        sb.append(summaryCard("전표 출금", vOut, "#374151"));
        sb.append("</div></div>");

        // 차이(미확인) 행
        sb.append("""
            <div style="padding:12px 32px;">
              <table style="width:100%;border-collapse:collapse;background:#f0fdf4;border-radius:8px;overflow:hidden;">
                <tr>
                  <td style="padding:10px 16px;font-size:13px;color:#374151;font-weight:700;">미확인 차이 (입금)</td>
                  <td style="padding:10px 16px;font-size:14px;font-weight:700;text-align:right;color:""")
          .append(diffColor((long) inDiff))
          .append("\">").append(fmt(inDiff)).append(" 원</td>")
          .append("</tr><tr>")
          .append("<td style=\"padding:10px 16px;font-size:13px;color:#374151;font-weight:700;\">미확인 차이 (출금)</td>")
          .append("<td style=\"padding:10px 16px;font-size:14px;font-weight:700;text-align:right;color:")
          .append(diffColor((long) outDiff))
          .append("\">").append(fmt(outDiff)).append(" 원</td>")
          .append("</tr></table></div>");

        // 은행별 명세
        if (r.getBanks() != null && !r.getBanks().isEmpty()) {
            sb.append(sectionTitle("은행별 입출금"));
            sb.append("<div style=\"padding:0 32px 8px;\"><table style=\"width:100%;border-collapse:collapse;\">")
              .append("<thead><tr style=\"border-bottom:2px solid #1b253a;\">")
              .append(th("은행명")).append(th("입금")).append(th("출금"))
              .append("</tr></thead><tbody>");
            for (var row : r.getBanks()) {
                sb.append("<tr style=\"border-bottom:1px solid #f1f5f9;\">")
                  .append(td(row.getBankName()))
                  .append(tdAmt(row.getIncome()))
                  .append(tdAmt(row.getExpense()))
                  .append("</tr>");
            }
            sb.append("<tr style=\"background:#f8fafc;font-weight:700;\">")
              .append(td("합계")).append(tdAmt(bankIn)).append(tdAmt(bankOut))
              .append("</tr></tbody></table></div>");
        }

        // 전표 입금 명세
        if (r.getVoucherIncomes() != null && !r.getVoucherIncomes().isEmpty()) {
            sb.append(sectionTitle("전표 입금 (현금성 계정)"));
            sb.append("<div style=\"padding:0 32px 8px;\"><table style=\"width:100%;border-collapse:collapse;\">")
              .append("<thead><tr style=\"border-bottom:2px solid #1b253a;\">")
              .append(th("계정명")).append(th("금액"))
              .append("</tr></thead><tbody>");
            for (var row : r.getVoucherIncomes()) {
                sb.append("<tr style=\"border-bottom:1px solid #f1f5f9;\">")
                  .append(td(row.getAccountName())).append(tdAmt(row.getAmount()))
                  .append("</tr>");
            }
            sb.append("<tr style=\"background:#f8fafc;font-weight:700;\">")
              .append(td("합계")).append(tdAmt(vIn))
              .append("</tr></tbody></table></div>");
        }

        // 전표 출금 명세
        if (r.getVoucherExpenses() != null && !r.getVoucherExpenses().isEmpty()) {
            sb.append(sectionTitle("전표 출금 (현금성 계정)"));
            sb.append("<div style=\"padding:0 32px 8px;\"><table style=\"width:100%;border-collapse:collapse;\">")
              .append("<thead><tr style=\"border-bottom:2px solid #1b253a;\">")
              .append(th("계정명")).append(th("금액"))
              .append("</tr></thead><tbody>");
            for (var row : r.getVoucherExpenses()) {
                sb.append("<tr style=\"border-bottom:1px solid #f1f5f9;\">")
                  .append(td(row.getAccountName())).append(tdAmt(row.getAmount()))
                  .append("</tr>");
            }
            sb.append("<tr style=\"background:#f8fafc;font-weight:700;\">")
              .append(td("합계")).append(tdAmt(vOut))
              .append("</tr></tbody></table></div>");
        }

        // 푸터
        sb.append("""
            <div style="padding:20px 32px;border-top:1px solid #f1f5f9;margin-top:8px;">
              <p style="margin:0;font-size:11px;color:#9ca3af;line-height:1.6;">
                이 메일은 JDEND ERP에서 자동 발송되었습니다.<br>
                발송 수신 설정은 ERP 총괄관리 &gt; 이메일 보고서 설정에서 변경하실 수 있습니다.
              </p>
            </div>
            </div>
            </body></html>
            """);

        return sb.toString();
    }

    private String summaryCard(String label, long amount, String color) {
        return String.format("""
            <div style="flex:1;min-width:130px;background:%s;border-radius:8px;padding:14px 16px;">
              <div style="font-size:11px;color:#94aac4;margin-bottom:4px;">%s</div>
              <div style="font-size:16px;font-weight:700;color:#fff;">%s</div>
              <div style="font-size:11px;color:#94aac4;">원</div>
            </div>
            """, color, label, fmt(amount));
    }

    private String sectionTitle(String title) {
        return String.format("""
            <div style="padding:16px 32px 6px;">
              <div style="font-size:12px;font-weight:700;color:#374151;letter-spacing:.06em;
                          border-left:3px solid #2563eb;padding-left:8px;">%s</div>
            </div>
            """, title);
    }

    private String th(String text) {
        return String.format(
            "<th style=\"padding:8px 10px;font-size:12px;font-weight:700;color:#374151;" +
            "background:#f8fafc;text-align:left;\">%s</th>", text);
    }

    private String td(String text) {
        return String.format(
            "<td style=\"padding:9px 10px;font-size:13px;color:#374151;\">%s</td>",
            text == null ? "-" : text);
    }

    private String tdAmt(Long amount) {
        return String.format(
            "<td style=\"padding:9px 10px;font-size:13px;color:#1b253a;" +
            "text-align:right;font-variant-numeric:tabular-nums;\">%s</td>",
            fmt(amount));
    }
}
