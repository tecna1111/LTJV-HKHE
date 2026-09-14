package com.cosre.cosre_backend.modules.evaluation.service;
import com.cosre.cosre_backend.modules.evaluation.service.impl.PeerEvaluationServiceImpl;
import com.cosre.cosre_backend.modules.evaluation.repository.*;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.dto.request.PeerEvaluationSubmitRequest;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PeerEvaluationServiceTests {
    @Mock PeerEvaluationRepository evaluations;
    @Mock EvaluationCriteriaRepository criteria;
    @Mock EvaluationAccessService access;
    @Mock EvaluationRoundRepository rounds;
    @Mock ProjectRepository projects;
    @InjectMocks PeerEvaluationServiceImpl service;
    PeerEvaluationSubmitRequest request;
    EvaluationCriteria criterion;
    @BeforeEach void setup() {
        request=new PeerEvaluationSubmitRequest(1L,null,2L,4L,null,List.of(new PeerEvaluationSubmitRequest.DetailItem(5L,new BigDecimal("8"),null)));
        criterion=EvaluationCriteria.builder().id(5L).projectId(1L).maxScore(BigDecimal.TEN).weight(BigDecimal.ONE).title("Contribution").build();
    }
    void open() {
        var round=new EvaluationRound(); round.setId("2:1"); round.setFinalOpen(true);
        when(rounds.findById("2:1")).thenReturn(Optional.of(round));
    }
    void rubric() { when(criteria.findByProjectId(1L)).thenReturn(List.of(criterion)); }
    @Test void rejectsSelfReview() {
        request.setEvaluateeId(3L);
        assertThatThrownBy(() -> service.submit(3L,request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(evaluations);
    }
    @Test void rejectsClosedFinalWindow() {
        assertThatThrownBy(() -> service.submit(3L,request)).hasMessageContaining("not been opened");
        verify(evaluations,never()).save(any());
    }
    @Test void rejectsNewSubmissionAfterLockEvenWithoutPriorReview() {
        var round=new EvaluationRound();round.setLocked(true);
        when(rounds.findById("2:1")).thenReturn(Optional.of(round));
        assertThatThrownBy(() -> service.submit(3L,request)).hasMessageContaining("locked");
        verifyNoInteractions(evaluations);
    }
    @Test void rejectsDuplicateCriteria() {
        open();rubric();request.setDetails(List.of(request.getDetails().get(0),request.getDetails().get(0)));
        assertThatThrownBy(() -> service.submit(3L,request)).hasMessageContaining("exactly once");
    }
    @Test void rejectsForeignCriteria() {
        open();rubric();request.getDetails().get(0).setCriteriaId(99L);
        assertThatThrownBy(() -> service.submit(3L,request)).hasMessageContaining("exactly once");
    }
    @Test void rejectsIncompleteWeights() {
        open();rubric();criterion.setWeight(new BigDecimal("0.5"));
        assertThatThrownBy(() -> service.submit(3L,request)).hasMessageContaining("sum to 1");
    }
    @Test void computesWeightedScoreAndUpdatesExistingReview() {
        open();rubric();when(criteria.findAllById(List.of(5L))).thenReturn(List.of(criterion));
        var existing=PeerEvaluation.builder().id(8L).status(EvaluationStatus.SUBMITTED).build();
        when(evaluations.findByEvaluatorIdAndEvaluateeIdAndProjectIdAndTeamIdAndMilestoneId(3L,4L,1L,2L,null)).thenReturn(Optional.of(existing));
        when(evaluations.save(any())).thenAnswer(i -> i.getArgument(0));
        var response=service.submit(3L,request);
        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getTotalScore()).isEqualByComparingTo("8.00");
        assertThat(existing.getDetails()).hasSize(1);
        verify(access).member(any(),eq(3L)); verify(access).member(any(),eq(4L));
    }
    @Test void rejectsAboveScale() {
        open();rubric();when(criteria.findAllById(List.of(5L))).thenReturn(List.of(criterion));
        request.getDetails().get(0).setScore(new BigDecimal("11"));
        assertThatThrownBy(() -> service.submit(3L,request)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void lockingEmptyTeamPersistsRoundLock() {
        when(evaluations.findByTeamIdAndProjectId(2L,1L)).thenReturn(List.of());
        service.lockEvaluations(2L,1L);
        verify(rounds).save(argThat(EvaluationRound::isLocked));
        verify(access).lecturer(2L,1L);
    }
    @Test void summaryExcludesMilestoneScores() {
        when(evaluations.findByEvaluateeIdAndTeamIdAndProjectIdAndStatus(4L,2L,1L,EvaluationStatus.SUBMITTED))
            .thenReturn(new ArrayList<>(List.of(PeerEvaluation.builder().milestoneId(9L).totalScore(BigDecimal.TEN).build(), PeerEvaluation.builder().totalScore(new BigDecimal("6")).build())));
        var result=service.getStudentSummary(4L,2L,1L);
        assertThat(result.getAverageScore()).isEqualByComparingTo("6");
        assertThat(result.getTotalReviewsReceived()).isEqualTo(1);
    }
}
