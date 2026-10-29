package com.jdend.erp.vehicle.fine.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * 과태료 고지서 사진(JPG/PNG) 또는 PDF 를 Claude 로 읽어 항목을 뽑는다.
 * API 키는 서버 환경변수 ANTHROPIC_API_KEY 로만 받는다(코드·저장소에 두지 않음).
 */
@Slf4j
@Component
public class FineNoticeReader {

  private static final String MODEL = "claude-opus-5-5";

  private static final String PROMPT = """
      이 이미지는 한국의 과태료(또는 범칙금) 고지서·사전통지서입니다. 적힌 내용을 그대로 읽어 주세요.
      - 차량번호: 고지서에 적힌 번호판 그대로(공백 없이, 예: 109하7531)
      - 위반일시: yyyy-MM-dd HH:mm (24시간). 시각이 없으면 00:00
      - 금액: 숫자만(원). 감경금액·일반금액이 같이 있으면 감경 전 '과태료 금액'
      - 납부기한: yyyy-MM-dd
      읽을 수 없거나 고지서에 없는 항목은 추측하지 말고 빈 문자열(금액은 0)로 두고, 그 사실을 note 에 적어 주세요.
      """;

  /** Claude 가 돌려주는 고지서 항목 */
  public record Notice(
      @JsonPropertyDescription("차량번호(번호판 그대로, 공백 없이). 없으면 빈 문자열") String vehicleNo,
      @JsonPropertyDescription("위반일시 yyyy-MM-dd HH:mm. 없으면 빈 문자열") String violationAt,
      @JsonPropertyDescription("위반내용(예: 속도위반, 신호위반, 주정차위반)") String violationType,
      @JsonPropertyDescription("위반장소") String violationPlace,
      @JsonPropertyDescription("부과기관(예: 서울특별시 강남구청, 경기남부경찰청)") String issuingAgency,
      @JsonPropertyDescription("과태료 금액(원), 숫자만. 없으면 0") Long amount,
      @JsonPropertyDescription("납부기한 yyyy-MM-dd. 없으면 빈 문자열") String dueDate,
      @JsonPropertyDescription("읽기 어려웠거나 확인이 필요한 부분. 없으면 빈 문자열") String note
  ) {}

  private final String apiKey;
  private volatile AnthropicClient client;

  public FineNoticeReader(@Value("${app.ai.anthropic-api-key:}") String apiKey) {
    this.apiKey = apiKey == null ? "" : apiKey.trim();
  }

  public boolean configured() {
    return !apiKey.isEmpty();
  }

  /** 고지서를 읽는다. 실패하면 이유를 담은 IllegalStateException. */
  public Notice read(byte[] bytes, String ext) {
    if (!configured()) {
      throw new IllegalStateException("고지서 자동 인식 키(ANTHROPIC_API_KEY)가 서버에 설정되지 않았습니다.");
    }
    String b64 = Base64.getEncoder().encodeToString(bytes);
    String e = ext == null ? "" : ext.toLowerCase();
    ContentBlockParam file;
    if (e.equals("pdf")) {
      file = ContentBlockParam.ofDocument(DocumentBlockParam.builder()
          .source(Base64PdfSource.builder().data(b64).build()).build());
    } else {
      Base64ImageSource.MediaType type = e.equals("png") ? Base64ImageSource.MediaType.IMAGE_PNG
          : Base64ImageSource.MediaType.IMAGE_JPEG;
      file = ContentBlockParam.ofImage(ImageBlockParam.builder()
          .source(Base64ImageSource.builder().mediaType(type).data(b64).build()).build());
    }

    StructuredMessageCreateParams<Notice> params = MessageCreateParams.builder()
        .model(MODEL)
        .maxTokens(4000L)
        .outputConfig(Notice.class)
        .addUserMessageOfBlockParams(List.of(file,
            ContentBlockParam.ofText(TextBlockParam.builder().text(PROMPT).build())))
        .build();

    StructuredMessage<Notice> res = client().messages().create(params);
    if (res.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
      throw new IllegalStateException("고지서를 읽지 못했습니다(거절됨).");
    }
    return res.content().stream()
        .flatMap(cb -> cb.text().stream())
        .map(t -> t.text())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("고지서에서 읽은 내용이 없습니다."));
  }

  private AnthropicClient client() {
    if (client == null) {
      synchronized (this) {
        if (client == null) {
          client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        }
      }
    }
    return client;
  }
}
