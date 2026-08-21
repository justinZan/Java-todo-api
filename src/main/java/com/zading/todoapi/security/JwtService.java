package com.zading.todoapi.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zading.todoapi.config.properties.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;

    public JwtService(
            ObjectMapper objectMapper,
            JwtProperties jwtProperties
    ) {
        this.objectMapper = objectMapper;
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.expirationMinutes() * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64UrlEncode(toJson(header)) + "." + base64UrlEncode(toJson(payload));
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String extractUsername(String token) {
        Map<String, Object> payload = parsePayload(token);
        Object subject = payload.get("sub");

        if (subject == null) {
            throw new IllegalArgumentException("token 中缺少用户名");
        }

        return subject.toString();
    }

    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isExpired(token) && hasValidSignature(token);
    }

    private boolean isExpired(String token) {
        Map<String, Object> payload = parsePayload(token);
        Object exp = payload.get("exp");

        if (exp == null) {
            return true;
        }

        long expiresAt = Long.parseLong(exp.toString());
        return Instant.now().getEpochSecond() >= expiresAt;
    }

    private boolean hasValidSignature(String token) {
        String[] parts = splitToken(token);
        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        return MessageDigestUtil.constantTimeEquals(expectedSignature, parts[2]);
    }

    private Map<String, Object> parsePayload(String token) {
        String[] parts = splitToken(token);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("token 内容格式不正确", exception);
        }
    }

    private String[] splitToken(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("token 格式不正确");
        }

        return parts;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT JSON 序列化失败", exception);
        }
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(key);
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }
}
