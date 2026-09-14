package com.cosre.cosre_backend.modules.meeting.dto;

import com.cosre.cosre_backend.modules.meeting.entity.Meeting;
import com.cosre.cosre_backend.modules.meeting.entity.MeetingStatus;
import java.time.LocalDateTime;

public record MeetingResponse(Long id, Long teamId, Long organizerId, String title, String description,
        LocalDateTime startsAt, LocalDateTime endsAt, MeetingStatus status, String joinUrl) {
    public static MeetingResponse from(Meeting value, String joinUrl) {
        return new MeetingResponse(value.getId(), value.getTeamId(), value.getOrganizerId(), value.getTitle(),
                value.getDescription(), value.getStartsAt(), value.getEndsAt(), value.getStatus(), joinUrl);
    }
}
