package com.cosre.cosre_backend.modules.evaluation;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.project.entity.*;
import com.cosre.cosre_backend.modules.project.repository.*;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import com.cosre.cosre_backend.modules.evaluation.service.*;
import com.cosre.cosre_backend.modules.evaluation.dto.request.*;
import com.cosre.cosre_backend.modules.evaluation.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class EvaluationWorkflowTests {
    @Autowired UserRepository users; @Autowired ClassroomRepository classrooms;
    @Autowired ProjectRepository projects; @Autowired TeamRepository teams;
    @Autowired EvaluationCriteriaService criteria; @Autowired PeerEvaluationService peer;
    @Autowired PeerEvaluationRepository evaluations; @Autowired FinalEvaluationService finals;
    @Autowired EntityManager em;
    @Autowired MilestoneQuestionRepository questions; @Autowired MilestoneAnswerRepository answers;
    Project project; Team team; User lecturer; User student1; User student2; Long criterionId;
    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getUsername(),"unused",List.of()));
    }
    @BeforeEach void setup() {
        lecturer=users.findByUsername("lecturer").orElseThrow();
        student1=users.findByUsername("student1").orElseThrow(); student2=users.findByUsername("student2").orElseThrow();
        var classroom=classrooms.findByCodeIgnoreCase("SE101-DEMO").orElseThrow();
        project=new Project();project.setTitle("D evaluation test");project.setSubjectId(classroom.getSubject().getId());project.setCreatedBy(lecturer.getId());
        project=projects.saveAndFlush(project);
        team=new Team();team.setName("D workflow test");team.setClassroomId(classroom.getId());team.setProjectId(project.getId());team.setLecturer(lecturer);
        team.getMembers().add(student1);team.getMembers().add(student2);team=teams.saveAndFlush(team);
        login(lecturer);
        criterionId=criteria.create(new CriteriaRequest(project.getId(),"Contribution",null,BigDecimal.TEN,BigDecimal.ONE),lecturer.getId()).getId();
        peer.openFinal(team.getId(),project.getId());
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    PeerEvaluationSubmitRequest request(String score) {
        return new PeerEvaluationSubmitRequest(project.getId(),null,team.getId(),student2.getId(),null,
                List.of(new PeerEvaluationSubmitRequest.DetailItem(criterionId,new BigDecimal(score),null)));
    }
    @Test void finalReviewUpsertAndSummaryWorkWithDatabase() {
        login(student1); var first=peer.submit(student1.getId(),request("8"));em.flush();em.clear();
        var second=peer.submit(student1.getId(),request("9"));em.flush();em.clear();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(evaluations.findByTeamIdAndProjectId(team.getId(),project.getId())).hasSize(1);
        login(lecturer);
        var summary=peer.getStudentSummary(student2.getId(),team.getId(),project.getId());
        assertThat(summary.getAverageScore()).isEqualByComparingTo("9");
        assertThat(summary.getTotalReviewsReceived()).isEqualTo(1);
    }
    @Test void lecturerCanGradeTeamAndIndividualSeparately() {
        finals.grade(new FinalEvaluationService.GradeRequest(team.getId(),project.getId(),null,new BigDecimal("8"),"Team grade"));
        finals.grade(new FinalEvaluationService.GradeRequest(team.getId(),project.getId(),student2.getId(),new BigDecimal("9"),"Individual grade"));
        em.flush();em.clear();
        assertThat(finals.list(team.getId(),project.getId())).hasSize(2);
        login(student1);assertThat(finals.list(team.getId(),project.getId())).hasSize(1);
        login(student2);assertThat(finals.list(team.getId(),project.getId())).hasSize(2);
    }
    @Test void peerFeedbackUsesPersistedAnswerIdAndCanBeUpdated() {
        var milestone=new ProjectMilestone();milestone.setTitle("M1");milestone.setDueOffsetDays(7);milestone.setDisplayOrder(1);project.addMilestone(milestone);
        project=projects.saveAndFlush(project);milestone=project.getMilestones().get(0);
        var question=new MilestoneQuestion();question.setMilestoneId(milestone.getId());question.setQuestionText("Explain");question.setCreatedBy(lecturer.getId());question=questions.saveAndFlush(question);
        var answer=new MilestoneAnswer();answer.setQuestionId(question.getId());answer.setTeamId(team.getId());answer.setStudentId(student2.getId());answer.setAnswerText("Answer");answer=answers.saveAndFlush(answer);
        login(student1);
        var first=finals.feedback(answer.getId(),new FinalEvaluationService.FeedbackRequest("First"));em.flush();
        var second=finals.feedback(answer.getId(),new FinalEvaluationService.FeedbackRequest("Updated"));em.flush();em.clear();
        assertThat(second.getId()).isEqualTo(first.getId());assertThat(finals.feedbacks(answer.getId())).hasSize(1);
    }
    @Test void lockedEmptyRoundRejectsFirstSubmission() {
        peer.lockEvaluations(team.getId(),project.getId());em.flush();em.clear();login(student1);
        assertThatThrownBy(() -> peer.submit(student1.getId(),request("8"))).hasMessageContaining("locked");
    }
    @Test void memberCanReadRoundStateAfterLock() {
        peer.lockEvaluations(team.getId(), project.getId()); em.flush(); em.clear(); login(student1);
        assertThat(peer.getRound(team.getId(), project.getId()).isLocked()).isTrue();
    }
    @Test void outsiderCannotReadRoundState() {
        team.getMembers().remove(student1); teams.saveAndFlush(team); login(student1);
        assertThatThrownBy(() -> peer.getRound(team.getId(), project.getId()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
