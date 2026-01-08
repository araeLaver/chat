package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SmsService Tests")
class SmsServiceTest {

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
    }

    @Nested
    @DisplayName("sendSms Tests")
    class SendSmsTests {

        @Test
        @DisplayName("Should return false when accessKey is empty")
        void shouldReturnFalseWhenAccessKeyIsEmpty() {
            ReflectionTestUtils.setField(smsService, "accessKey", "");
            ReflectionTestUtils.setField(smsService, "secretKey", "secret");
            ReflectionTestUtils.setField(smsService, "serviceId", "service");
            ReflectionTestUtils.setField(smsService, "senderPhone", "010-1234-5678");

            boolean result = smsService.sendSms("010-9876-5432", "Test message");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when secretKey is empty")
        void shouldReturnFalseWhenSecretKeyIsEmpty() {
            ReflectionTestUtils.setField(smsService, "accessKey", "access");
            ReflectionTestUtils.setField(smsService, "secretKey", "");
            ReflectionTestUtils.setField(smsService, "serviceId", "service");
            ReflectionTestUtils.setField(smsService, "senderPhone", "010-1234-5678");

            boolean result = smsService.sendSms("010-9876-5432", "Test message");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when serviceId is empty")
        void shouldReturnFalseWhenServiceIdIsEmpty() {
            ReflectionTestUtils.setField(smsService, "accessKey", "access");
            ReflectionTestUtils.setField(smsService, "secretKey", "secret");
            ReflectionTestUtils.setField(smsService, "serviceId", "");
            ReflectionTestUtils.setField(smsService, "senderPhone", "010-1234-5678");

            boolean result = smsService.sendSms("010-9876-5432", "Test message");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when senderPhone is empty")
        void shouldReturnFalseWhenSenderPhoneIsEmpty() {
            ReflectionTestUtils.setField(smsService, "accessKey", "access");
            ReflectionTestUtils.setField(smsService, "secretKey", "secret");
            ReflectionTestUtils.setField(smsService, "serviceId", "service");
            ReflectionTestUtils.setField(smsService, "senderPhone", "");

            boolean result = smsService.sendSms("010-9876-5432", "Test message");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("sendVerificationCode Tests")
    class SendVerificationCodeTests {

        @Test
        @DisplayName("Should return false when configuration incomplete")
        void shouldReturnFalseWhenConfigurationIncomplete() {
            ReflectionTestUtils.setField(smsService, "accessKey", "");
            ReflectionTestUtils.setField(smsService, "secretKey", "");
            ReflectionTestUtils.setField(smsService, "serviceId", "");
            ReflectionTestUtils.setField(smsService, "senderPhone", "");

            boolean result = smsService.sendVerificationCode("010-9876-5432", "123456");

            assertFalse(result);
        }
    }
}
