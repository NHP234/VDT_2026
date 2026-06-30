package com.vdt2026.omnicare.channel.email.infrastructure;

import com.vdt2026.omnicare.channel.email.application.EmailInboundNormalizer;
import com.vdt2026.omnicare.channel.email.application.InvalidEmailInboundEventException;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventDispatchService;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.imap.enabled", havingValue = "true")
class EmailImapPollingService {

    private static final Logger log = LoggerFactory.getLogger(EmailImapPollingService.class);

    private final EmailInboundMessageMapper mapper;
    private final EmailInboundNormalizer normalizer;
    private final InboundEventDispatchService dispatchService;
    private final ImapSettings settings;

    EmailImapPollingService(
        EmailInboundMessageMapper mapper,
        EmailInboundNormalizer normalizer,
        InboundEventDispatchService dispatchService,
        @Value("${app.email.mailbox:demo@example.test}") String providerAccountId,
        @Value("${app.email.imap.host:localhost}") String host,
        @Value("${app.email.imap.port:1143}") int port,
        @Value("${app.email.imap.username:}") String username,
        @Value("${app.email.imap.password:}") String password,
        @Value("${app.email.imap.folder:INBOX}") String folder,
        @Value("${app.email.imap.protocol:imap}") String protocol,
        @Value("${app.email.imap.max-messages-per-poll:10}") int maxMessagesPerPoll
    ) {
        this.mapper = mapper;
        this.normalizer = normalizer;
        this.dispatchService = dispatchService;
        this.settings = new ImapSettings(
            providerAccountId,
            host,
            port,
            username,
            password,
            folder,
            protocol,
            Math.max(1, maxMessagesPerPoll)
        );
    }

    @Scheduled(fixedDelayString = "${app.email.imap.poll-fixed-delay-ms:10000}")
    void poll() {
        try {
            pollOnce();
        }
        catch (MessagingException ex) {
            log.warn("IMAP polling failed for mailbox {}: {}", settings.providerAccountId(), ex.toString());
        }
    }

    int pollOnce() throws MessagingException {
        Session session = Session.getInstance(new Properties());
        try (Store store = session.getStore(settings.protocol())) {
            store.connect(settings.host(), settings.port(), settings.username(), settings.password());
            Folder folder = store.getFolder(settings.folder());
            if (!folder.exists()) {
                log.warn("IMAP folder {} does not exist for mailbox {}", settings.folder(), settings.providerAccountId());
                return 0;
            }
            try (folder) {
                folder.open(Folder.READ_WRITE);
                Message[] unseenMessages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                int processed = 0;
                for (Message message : unseenMessages) {
                    if (processed >= settings.maxMessagesPerPoll()) {
                        break;
                    }
                    if (process(message)) {
                        processed++;
                    }
                }
                return processed;
            }
        }
    }

    private boolean process(Message message) throws MessagingException {
        try {
            String correlationId = "imap-" + UUID.randomUUID();
            EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(
                mapper.map(message, settings.providerAccountId()),
                correlationId
            );
            dispatchService.dispatch(event);
            message.setFlag(Flags.Flag.SEEN, true);
            return true;
        }
        catch (InvalidEmailInboundEventException ex) {
            log.warn("Skipping invalid IMAP message for mailbox {}: {}", settings.providerAccountId(), ex.getMessage());
            message.setFlag(Flags.Flag.SEEN, true);
            return false;
        }
    }

    private record ImapSettings(
        String providerAccountId,
        String host,
        int port,
        String username,
        String password,
        String folder,
        String protocol,
        int maxMessagesPerPoll
    ) {
    }
}
