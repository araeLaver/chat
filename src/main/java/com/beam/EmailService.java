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

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@beam.chat}")
    private String fromEmail;

    @Value("${app.name:BEAM}")
    private String appName;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        if (mailSender == null) {
            logger.warn("Mail sender not configured. Email not sent to {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[" + appName + "] 이메일 인증번호");

            String htmlContent = buildVerificationEmailHtml(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Verification email sent to {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
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

    @Async
    public void sendWelcomeEmail(String toEmail, String displayName) {
        if (mailSender == null) {
            logger.warn("Mail sender not configured. Welcome email not sent to {}", toEmail);
            return;
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

        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}
