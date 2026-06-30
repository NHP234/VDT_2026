package com.vdt2026.omnicare.channel.facebook.interfaces;

record FacebookWebhookReceiveResponse(
    String status,
    boolean processed
) {
}
