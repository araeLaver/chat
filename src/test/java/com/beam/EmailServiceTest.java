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
    }

    @Nested
    @DisplayName("sendVerificationEmail Tests")
    class SendVerificationEmailTests {

        @Test
        @DisplayName("Should send verification email successfully")
        void shouldSendVerificationEmailSuccessfully() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendVerificationEmail("test@example.com", "123456");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send when mailSender is null")
        void shouldNotSendWhenMailSenderIsNull() {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            emailService.sendVerificationEmail("test@example.com", "123456");

            verify(mailSender, never()).createMimeMessage();
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

            emailService.sendWelcomeEmail("test@example.com", "TestUser");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should not send when mailSender is null")
        void shouldNotSendWhenMailSenderIsNull() {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            emailService.sendWelcomeEmail("test@example.com", "TestUser");

            verify(mailSender, never()).createMimeMessage();
        }
    }
}
