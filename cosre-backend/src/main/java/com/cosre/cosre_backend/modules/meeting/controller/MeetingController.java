package com.cosre.cosre_backend.modules.meeting.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingRequest;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingResponse;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingJoinResponse;
import com.cosre.cosre_backend.modules.meeting.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1")
public class MeetingController {
    private final MeetingService service; public MeetingController(MeetingService service) { this.service = service; }
    @GetMapping("/teams/{teamId}/meetings") public ApiResponse<List<MeetingResponse>> list(@PathVariable Long teamId, Authentication auth) { return new ApiResponse<>(true, "Meetings loaded", service.list(teamId, auth.getName())); }
    @PostMapping("/teams/{teamId}/meetings") public ApiResponse<MeetingResponse> create(@PathVariable Long teamId, @Valid @RequestBody MeetingRequest request, Authentication auth) { return new ApiResponse<>(true, "Meeting created", service.create(teamId, request, auth.getName())); }
    @PostMapping("/teams/{teamId}/meetings/instant") public ApiResponse<MeetingJoinResponse> startInstant(@PathVariable Long teamId, Authentication auth) { return new ApiResponse<>(true, "Instant meeting started", service.startInstant(teamId, auth.getName())); }
    @PutMapping("/meetings/{id}") public ApiResponse<MeetingResponse> update(@PathVariable Long id, @Valid @RequestBody MeetingRequest request, Authentication auth) { return new ApiResponse<>(true, "Meeting updated", service.update(id, request, auth.getName())); }
    @DeleteMapping("/meetings/{id}") public ApiResponse<Void> cancel(@PathVariable Long id, Authentication auth) { service.cancel(id, auth.getName()); return new ApiResponse<>(true, "Meeting cancelled", null); }
    @PostMapping("/meetings/{id}/join") public ApiResponse<MeetingJoinResponse> join(@PathVariable Long id, Authentication auth) { return new ApiResponse<>(true, "Meeting access granted", service.join(id, auth.getName())); }
}
