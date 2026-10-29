package com.jdend.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버 간 공개 API(/api/public/**) 호출 클라이언트 목록.
 * key 는 운영 설정(환경변수/application-prod.yml)에서만 넣는다. 비어 있으면 그 클라이언트는 꺼진 것으로 본다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "public-api")
public class PublicApiProperties {

    private List<Client> clients = new ArrayList<>();

    @Getter
    @Setter
    public static class Client {
        /** 식별용 이름 (예: acerentcar-homepage) */
        private String name;
        /** Bearer 키 */
        private String key;
        /** 이 키로 접근할 회사의 login_users.login_id */
        private String tenantLoginId;
    }
}
