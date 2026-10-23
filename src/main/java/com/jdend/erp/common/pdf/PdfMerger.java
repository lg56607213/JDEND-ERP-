package com.jdend.erp.common.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 스캔 파일(PDF/JPG/PNG)들을 순서대로 이어 붙여 하나의 PDF로 만든다.
 * 이미지는 A4 한 장에 여백을 두고 비율을 유지해 맞춘다.
 */
public final class PdfMerger {

    private static final float MARGIN = 20f;

    public record Part(byte[] bytes, String ext) {}

    private PdfMerger() {}

    public static byte[] merge(List<Part> parts) throws IOException {
        try (PDDocument out = new PDDocument()) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (Part part : parts) {
                String ext = part.ext() == null ? "" : part.ext().toLowerCase();
                if ("pdf".equals(ext)) {
                    try (PDDocument src = PDDocument.load(part.bytes())) {
                        merger.appendDocument(out, src);
                    }
                } else {
                    addImagePage(out, part.bytes());
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            out.save(bos);
            return bos.toByteArray();
        }
    }

    private static void addImagePage(PDDocument doc, byte[] bytes) throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "scan");

        // 가로로 긴 스캔은 A4 가로 페이지에 둔다.
        PDRectangle a4 = PDRectangle.A4;
        PDRectangle size = img.getWidth() > img.getHeight()
                ? new PDRectangle(a4.getHeight(), a4.getWidth()) : a4;
        PDPage page = new PDPage(size);
        doc.addPage(page);

        float boxW = size.getWidth() - MARGIN * 2;
        float boxH = size.getHeight() - MARGIN * 2;
        float scale = Math.min(boxW / img.getWidth(), boxH / img.getHeight());
        float w = img.getWidth() * scale;
        float h = img.getHeight() * scale;
        float x = (size.getWidth() - w) / 2;
        float y = (size.getHeight() - h) / 2;

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.drawImage(img, x, y, w, h);
        }
    }
}
