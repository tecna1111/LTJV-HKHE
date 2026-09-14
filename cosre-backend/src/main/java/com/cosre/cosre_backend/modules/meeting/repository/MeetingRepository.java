package com.cosre.cosre_backend.modules.meeting.repository;

import com.cosre.cosre_backend.modules.meeting.entity.Meeting;
import com.cosre.cosre_backend.modules.meeting.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByTeamIdOrderByStartsAtAsc(Long teamId);
    List<Meeting> findByStatusAndStartsAtBetweenAndReminderSentAtIsNull(MeetingStatus status, LocalDateTime from, LocalDateTime to);
}
