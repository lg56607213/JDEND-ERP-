package com.jdend.erp.payment.codef.util;

import com.jdend.erp.config.CodefProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CodefCryptoUtil {

    private final CodefProperties properties;

    /**
     * 공동인증서 비밀번호를 CODEF RSA 공개키로 암호화 (RSA/ECB/PKCS1Padding)
     */
    public String encryptPassword(String password) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getPublicKey());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("CODEF 비밀번호 암호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인증서 파일 바이트를 base64로 인코딩
     */
    public String encodeFile(byte[] fileBytes) {
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
