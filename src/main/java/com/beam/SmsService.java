package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${ncp.sms.access-key:}")
    private String accessKey;

    @Value("${ncp.sms.secret-key:}")
    private String secretKey;

    @Value("${ncp.sms.service-id:}")
    private String serviceId;

    @Value("${ncp.sms.sender-phone:}")
    private String senderPhone;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SmsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean sendSms(String toPhone, String content) {
        if (accessKey.isEmpty() || secretKey.isEmpty() || serviceId.isEmpty() || senderPhone.isEmpty()) {
            logger.warn("NCP SMS configuration is incomplete. SMS not sent.");
            return false;
        }

        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String url = "/sms/v2/services/" + serviceId + "/messages";
            String signature = makeSignature(timestamp, url);

            Map<String, Object> body = new HashMap<>();
            body.put("type", "SMS");
            body.put("from", senderPhone.replaceAll("-", ""));
            body.put("content", content);

            Map<String, String> message = new HashMap<>();
            message.put("to", toPhone.replaceAll("-", ""));
            body.put("messages", List.of(message));

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sens.apigw.ntruss.com" + url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", accessKey)
                    .header("x-ncp-apigw-signature-v2", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202) {
                logger.info("SMS sent successfully to {}", toPhone);
                return true;
            } else {
                logger.error("SMS send failed. Status: {}, Body: {}", response.statusCode(), response.body());
                return false;
            }

        } catch (Exception e) {
            logger.error("SMS send error: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean sendVerificationCode(String phoneNumber, String code) {
        String message = "[BEAM] 인증번호 [" + code + "]를 입력해주세요.";
        return sendSms(phoneNumber, message);
    }

    private String makeSignature(String timestamp, String url) throws Exception {
        String space = " ";
        String newLine = "\n";
        String method = "POST";

        String message = method + space + url + newLine + timestamp + newLine + accessKey;

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }
}
