package com.cosre.cosre_backend.modules.meeting.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record MeetingRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull @Future LocalDateTime startsAt,
        @NotNull @Future LocalDateTime endsAt) { }
