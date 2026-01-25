package com.beam;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@beam.chat");
        ReflectionTestUtils.setField(emailService, "appName", "BEAM");
        ReflectionTestUtils.setField(emailService, "maxRetryAttempts", 3);
    }

    @Nested
    @DisplayName("sendVerificationEmail Tests")
    class SendVerificationEmailTests {

        @Test
        @DisplayName("Should send verification email successfully")
        void shouldSendVerificationEmailSuccessfully() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            CompletableFuture<Boolean> result = emailService.sendVerificationEmail("test@example.com", "123456");

            assertTrue(result.get());
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should return false when mailSender is null")
        void shouldReturnFalseWhenMailSenderIsNull() throws Exception {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            CompletableFuture<Boolean> result = emailService.sendVerificationEmail("test@example.com", "123456");

            assertFalse(result.get());
        }

    }

    @Nested
    @DisplayName("sendWelcomeEmail Tests")
    class SendWelcomeEmailTests {

        @Test
        @DisplayName("Should send welcome email successfully")
        void shouldSendWelcomeEmailSuccessfully() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail("test@example.com", "TestUser");

            assertTrue(result.get());
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should return false when mailSender is null")
        void shouldReturnFalseWhenMailSenderIsNull() throws Exception {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            CompletableFuture<Boolean> result = emailService.sendWelcomeEmail("test@example.com", "TestUser");

            assertFalse(result.get());
        }
    }
}
