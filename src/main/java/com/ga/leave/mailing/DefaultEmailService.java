package com.ga.leave.mailing;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class DefaultEmailService implements EmailService {
    @Value("${spring.mail.from}")
    private String fromEmail;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * Send email asynchronously using a dedicated thread pool.
     * This prevents the main request thread from blocking on SMTP operations,
     * improving response times for registration and password reset endpoints.
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendMail(AbstractEmailContext email) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariables(email.getContext());

            String emailContent =
                    templateEngine.process(email.getTemplateLocation(), context);

            helper.setTo(email.getTo());
            helper.setSubject(email.getSubject());
            helper.setFrom(fromEmail);
            helper.setText(emailContent, true);

            emailSender.send(message);
            log.info("Email sent successfully to: {}", email.getTo());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email.getTo(), e.getMessage(), e);
        }
    }
}