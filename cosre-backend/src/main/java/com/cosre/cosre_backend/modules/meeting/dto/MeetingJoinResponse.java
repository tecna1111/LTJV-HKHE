package com.cosre.cosre_backend.modules.meeting.dto;

/** A short-lived, server-issued credential. Never persist or return it in meeting lists. */
public record MeetingJoinResponse(String roomName, String joinUrl, String token, long expiresAtEpochSeconds) { }
