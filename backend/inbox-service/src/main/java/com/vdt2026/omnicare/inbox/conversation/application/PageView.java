package com.vdt2026.omnicare.inbox.conversation.application;

import java.util.List;

public record PageView<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
