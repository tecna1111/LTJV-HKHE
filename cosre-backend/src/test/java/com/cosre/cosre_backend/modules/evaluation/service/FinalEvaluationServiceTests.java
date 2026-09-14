package com.cosre.cosre_backend.modules.evaluation.service;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.repository.*;
import com.cosre.cosre_backend.modules.project.entity.*;
import com.cosre.cosre_backend.modules.project.repository.*;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class FinalEvaluationServiceTests {
    @Mock EvaluationAccessService access; @Mock EvaluationRoundRepository rounds;
    @Mock FinalEvaluationRepository grades; @Mock AnswerPeerFeedbackRepository feedbacks;
    @Mock MilestoneAnswerRepository answers; @Mock MilestoneQuestionRepository questions; @Mock ProjectMilestoneRepository milestones;
    @InjectMocks FinalEvaluationService service;
    @Test void gradeRequiresOpenFinalRound() {
        assertThatThrownBy(() -> service.grade(new FinalEvaluationService.GradeRequest(2L,1L,null,BigDecimal.TEN,null))).hasMessageContaining("not open");
    }
    @Test void savesIndividualGrade() {
        var round=new EvaluationRound();round.setFinalOpen(true);when(rounds.findById("2:1")).thenReturn(Optional.of(round));
        var user=new User();user.setId(3L);when(access.currentUser()).thenReturn(user);
        when(grades.save(any())).thenAnswer(i -> i.getArgument(0));
        var result=service.grade(new FinalEvaluationService.GradeRequest(2L,1L,4L,new BigDecimal("9"),"Good"));
        assertThat(result.getId()).isEqualTo("2:1:4");assertThat(result.getScore()).isEqualByComparingTo("9");
        verify(access).member(any(),eq(4L));verify(access).lecturer(2L,1L);
    }
    @Test void cannotEditFinalGradeAfterLock() {
        var round=new EvaluationRound();round.setLocked(true);when(rounds.findById("2:1")).thenReturn(Optional.of(round));
        assertThatThrownBy(() -> service.grade(new FinalEvaluationService.GradeRequest(2L,1L,null,BigDecimal.TEN,null))).hasMessageContaining("locked");
        verifyNoInteractions(grades);
    }
    void answer(long author) {
        var answer=new MilestoneAnswer();answer.setId(8L);answer.setQuestionId(7L);answer.setTeamId(2L);answer.setStudentId(author);
        var question=new MilestoneQuestion();question.setMilestoneId(6L);
        var milestone=new ProjectMilestone();var project=new Project();project.setId(1L);milestone.setProject(project);
        when(answers.findById(8L)).thenReturn(Optional.of(answer));when(questions.findById(7L)).thenReturn(Optional.of(question));when(milestones.findById(6L)).thenReturn(Optional.of(milestone));
        var team=mock(Team.class);lenient().when(team.getId()).thenReturn(2L);lenient().when(access.team(2L,1L)).thenReturn(team);
        var user=new User();user.setId(4L);user.setRole(RoleEnum.STUDENT);when(access.currentUser()).thenReturn(user);
    }
    @Test void feedbackResolvesAnswerScope() {
        answer(5L);when(feedbacks.save(any())).thenAnswer(i -> i.getArgument(0));
        var result=service.feedback(8L,new FinalEvaluationService.FeedbackRequest("Please explain"));
        assertThat(result.getAnswerId()).isEqualTo(8L);assertThat(result.getReviewerId()).isEqualTo(4L);
        verify(access).team(2L,1L);
    }
    @Test void cannotGiveFeedbackOnOwnAnswer() {
        answer(4L);
        assertThatThrownBy(() -> service.feedback(8L,new FinalEvaluationService.FeedbackRequest("Text")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(feedbacks);
    }
    @Test void feedbackCannotUseMissingAnswer() {
        assertThatThrownBy(() -> service.feedback(8L,new FinalEvaluationService.FeedbackRequest("Text"))).hasMessageContaining("Answer not found");
    }
}
