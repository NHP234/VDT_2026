package com.vdt2026.omnicare.channel.email.infrastructure;

import com.vdt2026.omnicare.channel.email.application.EmailInboundEventCommand;
import com.vdt2026.omnicare.channel.email.application.InvalidEmailInboundEventException;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class EmailInboundMessageMapper {

    EmailInboundEventCommand map(Message message, String providerAccountId) {
        try {
            InternetAddress sender = sender(message);
            String messageId = header(message, "Message-ID");
            String textContent = plainText(message);
            requireHasText(messageId, "Email messageId is required");
            requireHasText(textContent, "Email textContent is required");
            return new EmailInboundEventCommand(
                providerAccountId,
                sender.getAddress(),
                sender.getPersonal(),
                recipient(message),
                messageId,
                message.getSubject(),
                textContent,
                occurredAt(message),
                header(message, "In-Reply-To"),
                references(message)
            );
        }
        catch (MessagingException | IOException ex) {
            throw new InvalidEmailInboundEventException("Email message could not be mapped: " + ex.getMessage());
        }
    }

    private InternetAddress sender(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from == null || from.length == 0 || !(from[0] instanceof InternetAddress internetAddress)) {
            throw new InvalidEmailInboundEventException("Email fromEmail is required");
        }
        return internetAddress;
    }

    private String recipient(Message message) throws MessagingException {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        if (recipients == null || recipients.length == 0) {
            return null;
        }
        if (recipients[0] instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress();
        }
        return recipients[0].toString();
    }

    private String header(Message message, String name) throws MessagingException {
        String[] values = message.getHeader(name);
        if (values == null || values.length == 0 || !StringUtils.hasText(values[0])) {
            return null;
        }
        return values[0].trim();
    }

    private List<String> references(Message message) throws MessagingException {
        String references = header(message, "References");
        if (!StringUtils.hasText(references)) {
            return List.of();
        }
        return Arrays.stream(references.split("\\s+"))
            .filter(StringUtils::hasText)
            .toList();
    }

    private Instant occurredAt(Message message) throws MessagingException {
        Date receivedDate = message.getReceivedDate();
        if (receivedDate != null) {
            return receivedDate.toInstant();
        }
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            return sentDate.toInstant();
        }
        return Instant.now();
    }

    private String plainText(Part part) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            return null;
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            return plainText(multipart);
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            return plainText(multipart);
        }
        if (part.isMimeType("text/plain")) {
            return content == null ? null : content.toString();
        }
        return null;
    }

    private String plainText(Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                continue;
            }
            String text = plainText(bodyPart);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private void requireHasText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidEmailInboundEventException(message);
        }
    }
}
