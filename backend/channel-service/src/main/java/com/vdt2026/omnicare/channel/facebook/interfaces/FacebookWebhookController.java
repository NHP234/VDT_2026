package com.vdt2026.omnicare.channel.facebook.interfaces;

import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationService;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookSignatureValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/facebook")
class FacebookWebhookController {

    private final FacebookWebhookVerificationService verificationService;
    private final FacebookWebhookSignatureValidator signatureValidator;

    FacebookWebhookController(
        FacebookWebhookVerificationService verificationService,
        FacebookWebhookSignatureValidator signatureValidator
    ) {
        this.verificationService = verificationService;
        this.signatureValidator = signatureValidator;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> verify(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String verifyToken,
        @RequestParam("hub.challenge") String challenge
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(verificationService.verify(mode, verifyToken, challenge));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FacebookWebhookReceiveResponse> receive(
        @RequestBody String rawBody,
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader
    ) {
        signatureValidator.validate(rawBody, signatureHeader);
        return ResponseEntity.accepted().body(new FacebookWebhookReceiveResponse("accepted", false));
    }
}
