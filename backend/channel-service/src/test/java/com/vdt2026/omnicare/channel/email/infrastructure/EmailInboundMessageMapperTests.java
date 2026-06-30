package com.vdt2026.omnicare.channel.email.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vdt2026.omnicare.channel.email.application.EmailInboundEventCommand;
import com.vdt2026.omnicare.channel.email.application.InvalidEmailInboundEventException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EmailInboundMessageMapperTests {

    private final EmailInboundMessageMapper mapper = new EmailInboundMessageMapper();

    @Test
    void mapsPlainTextMimeMessageToInboundCommand() throws Exception {
        MimeMessage message = message();
        message.setText("Plain text body");

        EmailInboundEventCommand command = mapper.map(message, "demo@example.test");

        assertThat(command.providerAccountId()).isEqualTo("demo@example.test");
        assertThat(command.fromEmail()).isEqualTo("tran.b@example.test");
        assertThat(command.fromDisplayName()).isEqualTo("Tran Thi B");
        assertThat(command.toEmail()).isEqualTo("demo@example.test");
        assertThat(command.messageId()).isEqualTo("<email-demo-1001@example.test>");
        assertThat(command.subject()).isEqualTo("Can ho tro don hang #42");
        assertThat(command.textContent()).isEqualTo("Plain text body");
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-06-29T04:15:00Z"));
        assertThat(command.inReplyTo()).isEqualTo("<email-demo-1000@example.test>");
        assertThat(command.references()).containsExactly("<email-demo-999@example.test>", "<email-demo-1000@example.test>");
    }

    @Test
    void extractsFirstPlainTextPartAndIgnoresAttachments() throws Exception {
        MimeMessage message = message();
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Text from multipart");
        multipart.addBodyPart(textPart);
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setFileName("invoice.pdf");
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment.setText("attachment text must be ignored");
        multipart.addBodyPart(attachment);
        message.setContent(multipart);

        EmailInboundEventCommand command = mapper.map(message, "demo@example.test");

        assertThat(command.textContent()).isEqualTo("Text from multipart");
    }

    @Test
    void rejectsMessageWithoutMessageId() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress("tran.b@example.test", "Tran Thi B"));
        message.setText("Missing message ID");

        assertThatThrownBy(() -> mapper.map(message, "demo@example.test"))
            .isInstanceOf(InvalidEmailInboundEventException.class)
            .hasMessageContaining("Email messageId is required");
    }

    private MimeMessage message() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress("tran.b@example.test", "Tran Thi B"));
        message.setRecipients(Message.RecipientType.TO, "demo@example.test");
        message.setSubject("Can ho tro don hang #42");
        message.setHeader("Message-ID", "<email-demo-1001@example.test>");
        message.setHeader("In-Reply-To", "<email-demo-1000@example.test>");
        message.setHeader("References", "<email-demo-999@example.test> <email-demo-1000@example.test>");
        message.setSentDate(Date.from(Instant.parse("2026-06-29T04:15:00Z")));
        return message;
    }
}
