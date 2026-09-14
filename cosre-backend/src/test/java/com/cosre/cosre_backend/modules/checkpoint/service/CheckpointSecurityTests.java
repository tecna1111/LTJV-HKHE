package com.cosre.cosre_backend.modules.checkpoint.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.checkpoint.dto.*;
import com.cosre.cosre_backend.modules.checkpoint.entity.*;
import com.cosre.cosre_backend.modules.checkpoint.repository.*;
import com.cosre.cosre_backend.modules.project.entity.*;
import com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckpointSecurityTests {
    @Mock UserRepository users;
    @Mock TeamRepository teams;
    @Mock ProjectMilestoneRepository milestones;
    @Mock CheckpointRepository checkpoints;
    @Mock CheckpointAssignmentRepository assignments;
    @Mock CheckpointSubmissionRepository submissions;
    CheckpointAccessService access;
    CheckpointService service;
    Team team;
    Checkpoint checkpoint;
    User leader, member, lecturer;

    @BeforeEach void setup() {
        access = new CheckpointAccessService(users, teams, assignments, milestones);
        service = new CheckpointService(access, checkpoints, assignments, submissions);
        leader = user(1L, RoleEnum.STUDENT);
        member = user(2L, RoleEnum.STUDENT);
        lecturer = user(3L, RoleEnum.LECTURER);
        team = new Team(); team.setProjectId(20L); team.setLeader(leader); team.setLecturer(lecturer);
        team.getMembers().addAll(List.of(leader, member));
        checkpoint = new Checkpoint(); checkpoint.setId(10L); checkpoint.setTeamId(5L);
        checkpoint.setStatus(CheckpointStatus.OPEN);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    User user(Long id, RoleEnum role) {
        var user = new User(); user.setId(id); user.setUsername("user" + id); user.setRole(role); return user;
    }
    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "unused", List.of()));
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }
    void teamExists() { when(teams.findById(5L)).thenReturn(Optional.of(team)); }
    void checkpointExists() { when(checkpoints.findById(10L)).thenReturn(Optional.of(checkpoint)); teamExists(); }
    CreateCheckpointRequest create(Set<Long> ids) {
        return new CreateCheckpointRequest(5L, null, "Checkpoint", "", LocalDateTime.now().plusDays(2), ids);
    }
    @Test void anonymousCannotRead() {
        assertThatThrownBy(() -> service.getCheckpointsByTeam(5L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(checkpoints);
    }
    @Test void inactiveAccountCannotRead() {
        member.setActive(false); login(member);
        assertThatThrownBy(() -> service.getCheckpointsByTeam(5L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void outsiderCannotList() {
        login(user(9L, RoleEnum.STUDENT)); teamExists();
        assertThatThrownBy(() -> service.getCheckpointsByTeam(5L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(checkpoints);
    }
    @Test void outsiderCannotReadDetail() {
        login(user(9L, RoleEnum.STUDENT)); checkpointExists();
        assertThatThrownBy(() -> service.getCheckpointById(10L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assignments);
    }
    @Test void memberCannotCreate() {
        login(member); teamExists();
        assertThatThrownBy(() -> service.createCheckpoint(create(Set.of(2L)))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(checkpoints);
    }
    @Test void memberCannotUpdate() {
        login(member); checkpointExists();
        var request = new UpdateCheckpointRequest("Changed", "", LocalDateTime.now().plusDays(1), Set.of(2L));
        assertThatThrownBy(() -> service.updateCheckpoint(10L, request)).isInstanceOf(AccessDeniedException.class);
        verify(checkpoints, never()).save(any());
    }
    @Test void leaderCannotAssignOutsider() {
        login(leader); teamExists();
        assertThatThrownBy(() -> service.createCheckpoint(create(Set.of(9L)))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(checkpoints, assignments);
    }
    @Test void leaderCannotReassignToOutsider() {
        login(leader); checkpointExists();
        var request = new UpdateCheckpointRequest("Changed", "", LocalDateTime.now().plusDays(1), Set.of(9L));
        assertThatThrownBy(() -> service.updateCheckpoint(10L, request)).isInstanceOf(BusinessRuleException.class);
        verify(assignments, never()).deleteByCheckpointId(any());
    }
    @Test void milestoneFromAnotherProjectRejected() {
        login(leader); teamExists();
        var project = new Project(); project.setId(21L);
        var milestone = new ProjectMilestone(); milestone.setProject(project);
        when(milestones.findById(7L)).thenReturn(Optional.of(milestone));
        var request = new CreateCheckpointRequest(5L, 7L, "Checkpoint", "", LocalDateTime.now().plusDays(1), Set.of(2L));
        assertThatThrownBy(() -> service.createCheckpoint(request)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(checkpoints);
    }
    @Test void creatorComesFromAuthenticatedAccount() {
        login(leader); teamExists();
        when(checkpoints.save(any())).thenAnswer(call -> { Checkpoint c = call.getArgument(0); c.setId(10L); return c; });
        assertThat(service.createCheckpoint(create(Set.of(2L))).createdBy()).isEqualTo(1L);
    }
    @Test void unassignedMemberCannotSubmit() {
        login(member); checkpointExists();
        assertThatThrownBy(() -> service.submitCheckpoint(10L, new SubmitCheckpointRequest("Work", null)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(submissions);
    }
    @Test void removedMemberCannotUseOldAssignment() {
        login(member); team.getMembers().remove(member); checkpointExists();
        assertThatThrownBy(() -> service.submitCheckpoint(10L, new SubmitCheckpointRequest("Work", null)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(submissions);
    }
    @Test void submissionIdentityComesFromAuthenticatedAccount() {
        login(member); checkpointExists();
        when(assignments.existsByCheckpointIdAndStudentId(10L, 2L)).thenReturn(true);
        when(checkpoints.save(checkpoint)).thenReturn(checkpoint);
        service.submitCheckpoint(10L, new SubmitCheckpointRequest("Work", null));
        var captured = ArgumentCaptor.forClass(CheckpointSubmission.class);
        verify(submissions).save(captured.capture());
        assertThat(captured.getValue().getSubmittedBy()).isEqualTo(2L);
    }
    @Test void otherLecturerCannotReview() {
        login(user(8L, RoleEnum.LECTURER)); checkpointExists();
        assertThatThrownBy(() -> service.reviewCheckpoint(10L, new ReviewCheckpointRequest(true, BigDecimal.TEN, "Good")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(submissions);
    }
    @Test void studentCannotReview() {
        login(leader); checkpointExists();
        assertThatThrownBy(() -> service.reviewCheckpoint(10L, new ReviewCheckpointRequest(true, BigDecimal.TEN, "Good")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(submissions);
    }
    @Test void managingLecturerReviewUsesAuthenticatedIdentity() {
        login(lecturer); checkpointExists(); checkpoint.setStatus(CheckpointStatus.SUBMITTED);
        var submission = new CheckpointSubmission(); submission.setCheckpointId(10L);
        when(submissions.findByCheckpointIdOrderBySubmittedAtDesc(10L)).thenReturn(List.of(submission));
        when(checkpoints.save(checkpoint)).thenReturn(checkpoint);
        service.reviewCheckpoint(10L, new ReviewCheckpointRequest(true, BigDecimal.TEN, "Good"));
        assertThat(submission.getReviewedBy()).isEqualTo(3L);
        assertThat(submission.getScore()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
