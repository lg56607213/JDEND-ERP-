package com.jdend.erp.common.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * XHTML 문자열을 PDF로 변환한다.
 *
 * <p>한글이 깨지지 않도록 나눔고딕(OFL)을 jar에 포함해 등록한다.
 * openhtmltopdf는 <b>엄격한 XHTML</b>만 처리하므로 입력 HTML은
 * 모든 태그가 닫혀 있어야 하고 &amp; 같은 문자도 이스케이프되어야 한다.</p>
 */
@Slf4j
@Component
public class PdfRenderer {

    private static final String FONT_CLASSPATH = "fonts/NanumGothic-Regular.ttf";
    private static final String FONT_FAMILY = "NanumGothic";

    /** jar 안의 폰트는 파일 경로가 필요해 임시 파일로 한 번만 풀어둔다. */
    private volatile Path fontFile;

    public byte[] render(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);

            Path font = resolveFontFile();
            if (font != null) {
                builder.useFont(font.toFile(), FONT_FAMILY);
            }

            builder.toStream(out);
            builder.run();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("[PDF] 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("PDF 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private Path resolveFontFile() {
        if (fontFile != null) return fontFile;

        synchronized (this) {
            if (fontFile != null) return fontFile;
            try (InputStream in = new ClassPathResource(FONT_CLASSPATH).getInputStream()) {
                Path tmp = Files.createTempFile("erp_font_", ".ttf");
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                fontFile = tmp;
                return fontFile;
            } catch (Exception e) {
                // 폰트가 없어도 PDF 자체는 생성되게 두고, 한글만 깨진다는 사실을 남긴다.
                log.error("[PDF] 한글 폰트 로드 실패 — 한글이 깨질 수 있습니다: {}", e.getMessage());
                return null;
            }
        }
    }

    /** XHTML 텍스트 노드에 넣을 값 이스케이프 */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
