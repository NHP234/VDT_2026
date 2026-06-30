package com.vdt2026.omnicare.channel.facebook.interfaces;

import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/facebook")
class FacebookWebhookController {

    private final FacebookWebhookVerificationService verificationService;

    FacebookWebhookController(FacebookWebhookVerificationService verificationService) {
        this.verificationService = verificationService;
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
}
