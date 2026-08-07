package com.jdend.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "codef")
public class CodefProperties {
    private String clientId;
    private String clientSecret;
    private String publicKey;
    private String baseUrl;
    private String oauthUrl;
}
