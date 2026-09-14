package com.cosre.cosre_backend.modules.meeting.service;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.service.ChatRoomAccessService;
import com.cosre.cosre_backend.modules.meeting.dto.MeetingRequest;
import com.cosre.cosre_backend.modules.meeting.entity.Meeting;
import com.cosre.cosre_backend.modules.meeting.repository.MeetingRepository;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTests {
    @Mock MeetingRepository meetings; @Mock TeamRepository teams; @Mock UserRepository users;
    @Mock ChatRoomAccessService access; @Mock NotificationService notifications;

    @Test void lecturerCanCreateMeetingWithoutExposingJoinCredentials() {
        Team team = new Team(); User lecturer = user(1L, "lecturer"); team.setLecturer(lecturer);
        when(teams.findById(8L)).thenReturn(Optional.of(team)); when(users.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        when(meetings.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime start = LocalDateTime.now().plusDays(1); var result = service().create(8L, new MeetingRequest("Demo", "", start, start.plusHours(1)), "lecturer");
        assertNull(result.joinUrl()); assertEquals(8L, result.teamId());
        verify(notifications).notifyMeeting(eq(team), any(Meeting.class), eq("MEETING_CREATED"));
    }

    @Test void joiningRequiresTeamAccess() {
        Meeting meeting = new Meeting(); meeting.setTeamId(8L); meeting.setRoomCode("cosre-room");
        when(meetings.findById(4L)).thenReturn(Optional.of(meeting)); when(users.findByUsername("student")).thenReturn(Optional.of(user(2L, "student")));
        var result = service().join(4L, "student");
        assertTrue(result.joinUrl().startsWith("https://meet.example.test/cosre-room?jwt="));
        verify(access).requireAccess("student", ChatRoomType.TEAM, 8L);
    }

    @Test void outsiderCannotReceiveRoomToken() {
        doThrow(new org.springframework.security.access.AccessDeniedException("denied")).when(access).requireAccess("outsider", ChatRoomType.TEAM, 8L);
        Meeting meeting = new Meeting(); meeting.setTeamId(8L); when(meetings.findById(4L)).thenReturn(Optional.of(meeting));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service().join(4L, "outsider"));
        verify(users, never()).findByUsername("outsider");
    }

    @Test void rejectsAnEndTimeBeforeTheStartTime() {
        Team team = new Team(); User lecturer = user(1L, "lecturer"); team.setLecturer(lecturer);
        when(teams.findById(8L)).thenReturn(Optional.of(team)); when(users.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThrows(RuntimeException.class, () -> service().create(8L, new MeetingRequest("Demo", null, start, start.minusMinutes(1)), "lecturer"));
        verifyNoInteractions(meetings, notifications);
    }

    private MeetingService service() { return new MeetingService(meetings, teams, users, access, notifications, "https://meet.example.test", "cosre", "01234567890123456789012345678901", 7200); }
    private User user(Long id, String username) { User user = new User(); user.setId(id); user.setUsername(username); user.setActive(true); return user; }
}
