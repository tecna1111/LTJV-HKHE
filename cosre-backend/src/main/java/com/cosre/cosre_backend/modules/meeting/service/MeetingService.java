package com.cosre.cosre_backend.modules.meeting.service;

import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.service.ChatRoomAccessService;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingRequest;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingJoinResponse;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingResponse;
import com.cosre.cosre_backend.modules.meeting.entity.Meeting;
import com.cosre.cosre_backend.modules.meeting.entity.MeetingStatus;
import com.cosre.cosre_backend.modules.meeting.repository.MeetingRepository;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MeetingService {
    private final MeetingRepository repository; private final TeamRepository teamRepository; private final UserRepository userRepository;
    private final ChatRoomAccessService accessService; private final NotificationService notificationService; private final String meetingBaseUrl;
    private final String jitsiAppId; private final String jitsiSecret; private final long tokenTtlSeconds;
    public MeetingService(MeetingRepository repository, TeamRepository teamRepository, UserRepository userRepository,
            ChatRoomAccessService accessService, NotificationService notificationService,
            @Value("${app.meeting.base-url:https://meet.jit.si}") String meetingBaseUrl,
            @Value("${app.meeting.jitsi-app-id:}") String jitsiAppId,
            @Value("${app.meeting.jitsi-secret:}") String jitsiSecret,
            @Value("${app.meeting.token-ttl-seconds:7200}") long tokenTtlSeconds) {
        this.repository = repository; this.teamRepository = teamRepository; this.userRepository = userRepository;
        this.accessService = accessService; this.notificationService = notificationService;
        this.meetingBaseUrl = meetingBaseUrl.replaceAll("/+$", "");
        this.jitsiAppId = jitsiAppId; this.jitsiSecret = jitsiSecret; this.tokenTtlSeconds = tokenTtlSeconds;
    }
    public List<MeetingResponse> list(Long teamId, String username) {
        accessService.requireAccess(username, ChatRoomType.TEAM, teamId);
        return repository.findByTeamIdOrderByStartsAtAsc(teamId).stream().map(value -> MeetingResponse.from(value, null)).toList();
    }
    public MeetingResponse create(Long teamId, MeetingRequest request, String username) {
        Team team = requireTeam(teamId); User actor = requireUser(username); requireOrganizer(team, actor);
        validateTimes(request); Meeting value = new Meeting(); value.setTeamId(teamId); value.setOrganizerId(actor.getId());
        apply(value, request); value.setRoomCode("cosre-" + UUID.randomUUID()); value = repository.save(value);
        notificationService.notifyMeeting(team, value, "MEETING_CREATED"); return MeetingResponse.from(value, null);
    }
    /** Starts a room now while preserving the same audit/history and membership controls as scheduled meetings. */
    public MeetingJoinResponse startInstant(Long teamId, String username) {
        Team team = requireTeam(teamId); User actor = requireUser(username); requireOrganizer(team, actor);
        LocalDateTime now = LocalDateTime.now();
        Meeting value = new Meeting(); value.setTeamId(teamId); value.setOrganizerId(actor.getId());
        value.setTitle("Cuộc họp tức thì"); value.setDescription("Cuộc họp được bắt đầu ngay bởi " + displayName(actor));
        value.setStartsAt(now); value.setEndsAt(now.plusHours(2)); value.setRoomCode("cosre-" + UUID.randomUUID());
        value = repository.save(value);
        notificationService.notifyMeeting(team, value, "MEETING_STARTED");
        return join(value.getId(), username);
    }
    public MeetingResponse update(Long id, MeetingRequest request, String username) {
        Meeting value = requireMeeting(id); Team team = requireTeam(value.getTeamId()); requireOrganizer(team, requireUser(username));
        if (value.getStatus() == MeetingStatus.CANCELLED) throw new BusinessRuleException("Không thể sửa cuộc họp đã hủy");
        validateTimes(request); apply(value, request); value.setReminderSentAt(null); value = repository.save(value);
        notificationService.notifyMeeting(team, value, "MEETING_UPDATED"); return MeetingResponse.from(value, null);
    }
    public void cancel(Long id, String username) {
        Meeting value = requireMeeting(id); Team team = requireTeam(value.getTeamId()); requireOrganizer(team, requireUser(username));
        if (value.getStatus() == MeetingStatus.CANCELLED) return; value.setStatus(MeetingStatus.CANCELLED); repository.save(value);
        notificationService.notifyMeeting(team, value, "MEETING_CANCELLED");
    }
    public MeetingJoinResponse join(Long id, String username) {
        Meeting value = requireMeeting(id); accessService.requireAccess(username, ChatRoomType.TEAM, value.getTeamId());
        if (value.getStatus() == MeetingStatus.CANCELLED) throw new BusinessRuleException("Cuộc họp đã bị hủy");
        User user = requireUser(username);
        if (jitsiAppId.isBlank() || jitsiSecret.length() < 32) throw new BusinessRuleException("Dịch vụ phòng họp chưa được cấu hình JWT an toàn");
        String jitsiDomain = requireSecureJitsiDomain(meetingBaseUrl);
        Instant now = Instant.now(); Instant expires = now.plusSeconds(tokenTtlSeconds);
        Key signingKey = Keys.hmacShaKeyFor(jitsiSecret.getBytes(StandardCharsets.UTF_8));
        String displayName = displayName(user);
        String email = user.getEmail() == null ? "" : user.getEmail();
        // These claims match docker-jitsi-meet JWT authentication: app ID is both issuer/audience;
        // subject is the protected Jitsi host, never the COSRE username.
        String token = Jwts.builder().setHeaderParam("typ", "JWT").setIssuer(jitsiAppId).setAudience(jitsiAppId)
                .setSubject(jitsiDomain).setIssuedAt(java.util.Date.from(now)).setExpiration(java.util.Date.from(expires))
                .claim("room", value.getRoomCode()).claim("context", java.util.Map.of("user", java.util.Map.of(
                        "id", String.valueOf(user.getId()), "name", displayName, "email", email)))
                .signWith(signingKey, SignatureAlgorithm.HS256).compact();
        return new MeetingJoinResponse(value.getRoomCode(), meetingBaseUrl + "/" + value.getRoomCode() + "?jwt=" + token,
                token, expires.getEpochSecond());
    }
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Meeting value : repository.findByStatusAndStartsAtBetweenAndReminderSentAtIsNull(MeetingStatus.SCHEDULED, now, now.plusMinutes(15))) {
            notificationService.notifyMeeting(requireTeam(value.getTeamId()), value, "MEETING_REMINDER"); value.setReminderSentAt(now);
        }
    }
    private void apply(Meeting value, MeetingRequest request) { value.setTitle(request.title().trim()); value.setDescription(request.description() == null ? null : request.description().trim()); value.setStartsAt(request.startsAt()); value.setEndsAt(request.endsAt()); }
    private void validateTimes(MeetingRequest request) { if (!request.endsAt().isAfter(request.startsAt())) throw new BusinessRuleException("Thời gian kết thúc phải sau thời gian bắt đầu"); }
    private void requireOrganizer(Team team, User actor) { if (!team.getLecturer().getId().equals(actor.getId()) && (team.getLeader() == null || !team.getLeader().getId().equals(actor.getId()))) throw new AccessDeniedException("Chỉ giảng viên hoặc trưởng nhóm mới được quản lý lịch họp"); }
    private String displayName(User user) { return user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName(); }
    private String requireSecureJitsiDomain(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || "meet.jit.si".equalsIgnoreCase(uri.getHost()))
                throw new BusinessRuleException("MEETING_BASE_URL phải là domain Jitsi riêng sử dụng HTTPS");
            return uri.getHost();
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("MEETING_BASE_URL không hợp lệ");
        }
    }
    private Team requireTeam(Long id) { return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found")); }
    private Meeting requireMeeting(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Meeting not found")); }
    private User requireUser(String username) { return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
}
