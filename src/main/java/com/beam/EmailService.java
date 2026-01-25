package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private EmailFailureHandler emailFailureHandler;

    @Value("${spring.mail.username:noreply@beam.chat}")
    private String fromEmail;

    @Value("${app.name:BEAM}")
    private String appName;

    @Value("${email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * 비동기 이메일 전송 (실패 시 재시도 및 알림)
     */
    @Async
    public CompletableFuture<Boolean> sendVerificationEmail(String toEmail, String code) {
        logger.info("Attempting to send verification email to {}", toEmail);

        if (mailSender == null) {
            logger.error("Mail sender is NULL. Check MAIL_USERNAME and MAIL_PASSWORD environment variables.");
            handleEmailFailure(toEmail, "VERIFICATION", "Mail sender not configured");
            return CompletableFuture.completedFuture(false);
        }

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("[" + appName + "] 이메일 인증번호");

                String htmlContent = buildVerificationEmailHtml(code);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                logger.info("Verification email sent successfully to {} (attempt {})", toEmail, attempt);
                return CompletableFuture.completedFuture(true);

            } catch (MessagingException e) {
                lastException = e;
                logger.warn("Failed to send verification email to {} (attempt {}/{}): {}",
                        toEmail, attempt, maxRetryAttempts, e.getMessage());
                if (attempt < maxRetryAttempts) {
                    sleep(1000 * attempt); // 백오프 대기
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error sending email to {} (attempt {}/{}): {}",
                        toEmail, attempt, maxRetryAttempts, e.getMessage());
                break; // 예상치 못한 오류는 재시도하지 않음
            }
        }

        // 모든 재시도 실패
        logger.error("All {} attempts failed to send verification email to {}", maxRetryAttempts, toEmail);
        handleEmailFailure(toEmail, "VERIFICATION",
                lastException != null ? lastException.getMessage() : "Unknown error");
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 이메일 전송 실패 처리 (로깅 및 알림)
     */
    private void handleEmailFailure(String toEmail, String emailType, String errorMessage) {
        logger.error("Email failure - Type: {}, To: {}, Error: {}", emailType, toEmail, errorMessage);

        // 실패 핸들러가 있으면 호출 (별도의 알림 시스템 연동 가능)
        if (emailFailureHandler != null) {
            try {
                emailFailureHandler.handleFailure(toEmail, emailType, errorMessage);
            } catch (Exception e) {
                logger.error("Failed to handle email failure notification: {}", e.getMessage());
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildVerificationEmailHtml(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .logo { font-size: 32px; font-weight: bold; color: #10B981; }
                    .code-box {
                        background: #f5f5f5;
                        border-radius: 8px;
                        padding: 30px;
                        text-align: center;
                        margin: 20px 0;
                    }
                    .code {
                        font-size: 36px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        color: #10B981;
                    }
                    .info { color: #666; font-size: 14px; margin-top: 20px; }
                    .footer { text-align: center; color: #999; font-size: 12px; margin-top: 40px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">⚡ BEAM</div>
                        <p>Messages at the speed of light</p>
                    </div>
                    <h2>이메일 인증</h2>
                    <p>아래 인증번호를 입력하여 이메일을 인증해주세요.</p>
                    <div class="code-box">
                        <div class="code">%s</div>
                    </div>
                    <p class="info">
                        • 인증번호는 5분간 유효합니다.<br>
                        • 본인이 요청하지 않은 경우 이 이메일을 무시해주세요.
                    </p>
                    <div class="footer">
                        <p>© 2024 BEAM. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }

    /**
     * 비동기 환영 이메일 전송
     */
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(String toEmail, String displayName) {
        if (mailSender == null) {
            logger.warn("Mail sender not configured. Welcome email not sent to {}", toEmail);
            return CompletableFuture.completedFuture(false);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[" + appName + "] 가입을 환영합니다!");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                        .header { text-align: center; margin-bottom: 30px; }
                        .logo { font-size: 32px; font-weight: bold; color: #10B981; }
                        .footer { text-align: center; color: #999; font-size: 12px; margin-top: 40px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">⚡ BEAM</div>
                        </div>
                        <h2>%s님, 환영합니다!</h2>
                        <p>BEAM에 가입해주셔서 감사합니다.</p>
                        <p>빛처럼 빠르고 안전한 메신저로 소중한 대화를 나눠보세요.</p>
                        <div class="footer">
                            <p>© 2024 BEAM. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(displayName);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Welcome email sent to {}", toEmail);
            return CompletableFuture.completedFuture(true);

        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            handleEmailFailure(toEmail, "WELCOME", e.getMessage());
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Unexpected error sending welcome email to {}: {}", toEmail, e.getMessage());
            handleEmailFailure(toEmail, "WELCOME", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
