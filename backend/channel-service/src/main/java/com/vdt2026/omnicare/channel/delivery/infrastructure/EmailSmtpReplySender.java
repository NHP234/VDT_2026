package com.vdt2026.omnicare.channel.delivery.infrastructure;

import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyDeliveryException;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyResult;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplySender;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestPayload;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.email.smtp.enabled", havingValue = "true", matchIfMissing = true)
class EmailSmtpReplySender implements OutboundReplySender {

    private final JavaMailSender mailSender;
    private final String mailFrom;

    EmailSmtpReplySender(
        JavaMailSender mailSender,
        @Value("${app.email.mail-from:demo@example.test}") String mailFrom
    ) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Override
    public boolean supports(ReplyRequestPayload payload) {
        return "EMAIL".equals(payload.channel());
    }

    @Override
    public OutboundReplyResult send(ReplyRequestPayload payload) {
        requireText(payload.externalIdentityId(), "Email recipient is required");
        requireText(payload.externalConversationId(), "Email conversation ID is required");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(MimeMessage.RecipientType.TO, payload.externalIdentityId());
            message.setSubject(replySubject(payload.subject()));
            message.setText(payload.content());
            String threadRootMessageId = threadRootMessageId(payload.externalConversationId());
            message.setHeader("In-Reply-To", threadRootMessageId);
            message.setHeader("References", threadRootMessageId);
            mailSender.send(message);
            return new OutboundReplyResult("smtp:" + payload.messageId());
        }
        catch (MessagingException | MailException ex) {
            throw new OutboundReplyDeliveryException("Email delivery failed: " + ex.getMessage(), null, ex);
        }
    }

    static String threadRootMessageId(String externalConversationId) {
        String[] parts = externalConversationId.split(":", 3);
        if (parts.length < 3 || !"email".equals(parts[0])) {
            throw new OutboundReplyDeliveryException("Invalid email conversation ID");
        }
        return parts[2];
    }

    static String replySubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            return "Re: Customer conversation";
        }
        String trimmed = subject.trim();
        if (trimmed.regionMatches(true, 0, "Re:", 0, 3)) {
            return trimmed;
        }
        return "Re: " + trimmed;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new OutboundReplyDeliveryException(message);
        }
    }
}
