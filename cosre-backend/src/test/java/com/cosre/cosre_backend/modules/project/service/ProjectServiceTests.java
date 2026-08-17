package com.cosre.cosre_backend.modules.project.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.project.dto.CreateProjectRequest;
import com.cosre.cosre_backend.modules.project.dto.MilestoneRequest;
import com.cosre.cosre_backend.modules.project.entity.Project;
import com.cosre.cosre_backend.modules.project.entity.ProjectStatus;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import com.cosre.cosre_backend.modules.syllabus.repository.SyllabusRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.project.repository.ClassroomProjectRepository;
import com.cosre.cosre_backend.modules.syllabus.entity.Syllabus;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock SyllabusRepository syllabusRepository;
    @Mock ClassroomRepository classroomRepository;
    @Mock ClassroomProjectRepository classroomProjectRepository;
    private ProjectService service;
    private User lecturer;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, userRepository, subjectRepository, syllabusRepository, classroomRepository, classroomProjectRepository);
        lecturer = new User();
        lecturer.setId(7L);
        lecturer.setUsername("lecturer");
        lecturer.setRole(RoleEnum.LECTURER);
        lecturer.setActive(true);
        lenient().when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
    }

    @Test
    void createBuildsDraftWithObjectivesAndMilestones() {
        when(subjectRepository.existsById(2L)).thenReturn(true);
        Subject subject = mock(Subject.class); when(subject.getId()).thenReturn(2L);
        Syllabus syllabus = new Syllabus(); syllabus.setSubject(subject); syllabus.setActive(true);
        when(syllabusRepository.findDetailedById(5L)).thenReturn(Optional.of(syllabus));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateProjectRequest request = new CreateProjectRequest(
                "COSRE", "PBL platform", 2L,
                List.of("Build API", "Build API", "Demo"),
                List.of(new MilestoneRequest("Analysis", "Requirements", 7)), 5L);

        var response = service.create(request, "lecturer");

        assertEquals(ProjectStatus.DRAFT, response.status());
        assertEquals(2, response.objectives().size());
        assertEquals(1, response.milestones().size());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void submitChangesOwnedDraftToPending() {
        Project project = new Project();
        project.setCreatedBy(7L);
        project.setStatus(ProjectStatus.DRAFT);
        project.setObjectives(new java.util.ArrayList<>(List.of("Demo")));
        com.cosre.cosre_backend.modules.project.entity.ProjectMilestone milestone =
                new com.cosre.cosre_backend.modules.project.entity.ProjectMilestone();
        milestone.setTitle("Demo");
        milestone.setDueOffsetDays(7);
        milestone.setDisplayOrder(0);
        project.addMilestone(milestone);
        when(projectRepository.findById(3L)).thenReturn(Optional.of(project));

        var response = service.submit(3L, "lecturer");

        assertEquals(ProjectStatus.PENDING, response.status());
    }

    @Test
    void submitRejectsProjectOwnedByAnotherLecturer() {
        Project project = new Project();
        project.setCreatedBy(99L);
        when(projectRepository.findById(3L)).thenReturn(Optional.of(project));

        assertThrows(BusinessRuleException.class, () -> service.submit(3L, "lecturer"));
    }

    @Test
    void headDepartmentCanApprovePendingProject() {
        User head = new User(); head.setId(8L); head.setUsername("head"); head.setRole(RoleEnum.HEAD_DEPT); head.setActive(true);
        when(userRepository.findByUsername("head")).thenReturn(Optional.of(head));
        Project project = new Project(); project.setStatus(ProjectStatus.PENDING);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        var response = service.review(10L, true, "Meets requirements", "head");

        assertEquals(ProjectStatus.APPROVED, response.status());
        assertEquals(8L, response.reviewedBy());
        assertEquals("Meets requirements", response.reviewNote());
    }

    @Test
    void reviewRejectsNonPendingProject() {
        User head = new User(); head.setId(8L); head.setUsername("head"); head.setRole(RoleEnum.HEAD_DEPT); head.setActive(true);
        when(userRepository.findByUsername("head")).thenReturn(Optional.of(head));
        Project project = new Project(); project.setStatus(ProjectStatus.DRAFT);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThrows(BusinessRuleException.class, () -> service.review(10L, true, null, "head"));
    }
}
