package com.cosre.cosre_backend.modules.meeting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
public class Meeting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "team_id", nullable = false) private Long teamId;
    @Column(name = "organizer_id", nullable = false, updatable = false) private Long organizerId;
    @Column(nullable = false, length = 150) private String title;
    @Column(length = 1000) private String description;
    @Column(name = "starts_at", nullable = false) private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalDateTime endsAt;
    @Column(name = "room_code", nullable = false, unique = true, length = 100) private String roomCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MeetingStatus status;
    @Column(name = "reminder_sent_at") private LocalDateTime reminderSentAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); if (status == null) status = MeetingStatus.SCHEDULED; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public Long getTeamId() { return teamId; } public void setTeamId(Long v) { teamId = v; }
    public Long getOrganizerId() { return organizerId; } public void setOrganizerId(Long v) { organizerId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public LocalDateTime getStartsAt() { return startsAt; } public void setStartsAt(LocalDateTime v) { startsAt = v; }
    public LocalDateTime getEndsAt() { return endsAt; } public void setEndsAt(LocalDateTime v) { endsAt = v; }
    public String getRoomCode() { return roomCode; } public void setRoomCode(String v) { roomCode = v; }
    public MeetingStatus getStatus() { return status; } public void setStatus(MeetingStatus v) { status = v; }
    public LocalDateTime getReminderSentAt() { return reminderSentAt; } public void setReminderSentAt(LocalDateTime v) { reminderSentAt = v; }
}
