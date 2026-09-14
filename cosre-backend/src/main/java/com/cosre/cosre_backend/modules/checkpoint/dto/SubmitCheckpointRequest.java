package com.cosre.cosre_backend.modules.checkpoint.dto;

import jakarta.validation.constraints.Size;

public record SubmitCheckpointRequest(
        @Size(max = 5000)
        String content,

        @Size(max = 1000)
        String attachmentUrl
) {
}