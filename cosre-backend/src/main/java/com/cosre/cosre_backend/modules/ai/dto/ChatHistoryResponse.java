package com.cosre.cosre_backend.modules.ai.dto;

import java.time.LocalDateTime;

public record ChatHistoryResponse(Long id, String prompt, String answer, LocalDateTime createdAt) { }
