package com.vdt2026.omnicare.channel.delivery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyDeliveryException;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyResult;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestPayload;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class EmailSmtpReplySenderTests {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailSmtpReplySender sender = new EmailSmtpReplySender(
        mailSender,
        "demo@example.test",
        "demo+omnicare@example.test"
    );

    @Test
    void sendsPlainTextEmailWithReplyThreadHeaders() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        OutboundReplyResult result = sender.send(emailPayload());

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()[0].toString()).isEqualTo("demo@example.test");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("demo+omnicare@example.test");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("tran.b@example.test");
        assertThat(message.getSubject()).isEqualTo("Re: Can ho tro don hang #42");
        assertThat(message.getHeader("In-Reply-To", null)).isEqualTo("<email-demo-1001@example.test>");
        assertThat(message.getHeader("References", null)).isEqualTo("<email-demo-1001@example.test>");
        assertThat(result.providerMessageId()).isEqualTo("smtp:50000000-0000-0000-0000-000000000010");
    }

    @Test
    void mapsMailSenderFailureToDeliveryException() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send(emailPayload()))
            .isInstanceOf(OutboundReplyDeliveryException.class)
            .hasMessageContaining("Email delivery failed");
    }

    @Test
    void extractsThreadRootMessageIdFromEmailConversationId() {
        assertThat(EmailSmtpReplySender.threadRootMessageId("email:demo@example.test:<root@example.test>"))
            .isEqualTo("<root@example.test>");
    }

    private ReplyRequestPayload emailPayload() {
        return new ReplyRequestPayload(
            UUID.fromString("50000000-0000-0000-0000-000000000010"),
            UUID.fromString("40000000-0000-0000-0000-000000000010"),
            "EMAIL",
            "EMAIL",
            "demo@example.test",
            "email:demo@example.test:<email-demo-1001@example.test>",
            "tran.b@example.test",
            "Can ho tro don hang #42",
            "Da nhan thong tin, minh se kiem tra ngay."
        );
    }
}
