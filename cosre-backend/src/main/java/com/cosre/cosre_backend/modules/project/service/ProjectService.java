package com.cosre.cosre_backend.modules.project.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.project.dto.*;
import com.cosre.cosre_backend.modules.project.entity.*;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import com.cosre.cosre_backend.modules.syllabus.repository.SyllabusRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.project.repository.ClassroomProjectRepository;
import com.cosre.cosre_backend.modules.project.entity.ClassroomProject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SyllabusRepository syllabusRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomProjectRepository classroomProjectRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            SyllabusRepository syllabusRepository,
            ClassroomRepository classroomRepository,
            ClassroomProjectRepository classroomProjectRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.syllabusRepository = syllabusRepository;
        this.classroomRepository = classroomRepository;
        this.classroomProjectRepository = classroomProjectRepository;
    }

    public ProjectResponse create(
            CreateProjectRequest request,
            String username
    ) {
        User lecturer = requireLecturer(username);

        if (!subjectRepository.existsById(request.subjectId())) {
            throw new ResourceNotFoundException("Subject not found");
        }
        if (request.syllabusId() == null) throw new BusinessRuleException("Project must be based on a syllabus");
        var syllabus = syllabusRepository.findDetailedById(request.syllabusId())
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        if (!syllabus.isActive() || !syllabus.getSubject().getId().equals(request.subjectId()))
            throw new BusinessRuleException("Syllabus must be active and belong to the selected subject");

        if (projectRepository.existsBySubjectIdAndTitleIgnoreCase(
                request.subjectId(),
                request.title().trim()
        )) {
            throw new BusinessRuleException(
                    "Project title already exists in this subject"
            );
        }

        Project project = new Project();
        project.setTitle(request.title().trim());
        project.setDescription(clean(request.description()));
        project.setSubjectId(request.subjectId());
        project.setSyllabusId(request.syllabusId());
        project.setCreatedBy(lecturer.getId());
        project.setStatus(ProjectStatus.DRAFT);

        project.setObjectives(cleanObjectives(request.objectives()));
        replaceMilestones(project, request.milestones());

        Project savedProject = projectRepository.save(project);

        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMine(String username) {
        User lecturer = requireLecturer(username);

        return projectRepository
                .findByCreatedByOrderByUpdatedAtDesc(lecturer.getId())
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long projectId, String username) {
        return ProjectResponse.from(requireOwnedProject(projectId, username));
    }

    public ProjectResponse update(
            Long projectId,
            UpdateProjectRequest request,
            String username
    ) {
        Project project = requireOwnedDraft(projectId, username);

        if (!subjectRepository.existsById(request.subjectId())) {
            throw new ResourceNotFoundException("Subject not found");
        }
        if (request.syllabusId() == null) throw new BusinessRuleException("Project must be based on a syllabus");
        var syllabus = syllabusRepository.findDetailedById(request.syllabusId())
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        if (!syllabus.isActive() || !syllabus.getSubject().getId().equals(request.subjectId()))
            throw new BusinessRuleException("Syllabus must be active and belong to the selected subject");

        String title = request.title().trim();
        if (projectRepository.existsBySubjectIdAndTitleIgnoreCaseAndIdNot(
                request.subjectId(), title, projectId)) {
            throw new BusinessRuleException("Project title already exists in this subject");
        }

        project.setTitle(title);
        project.setDescription(clean(request.description()));
        project.setSubjectId(request.subjectId());
        project.setSyllabusId(request.syllabusId());
        project.setObjectives(cleanObjectives(request.objectives()));
        replaceMilestones(project, request.milestones());

        return ProjectResponse.from(project);
    }

    public ProjectResponse submit(
            Long projectId,
            String username
    ) {
        Project project = requireOwnedProject(projectId, username);

        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only draft projects can be submitted"
            );
        }

        if (project.getObjectives().isEmpty()) {
            throw new BusinessRuleException(
                    "Project must contain at least one objective"
            );
        }

        if (project.getMilestones().isEmpty()) {
            throw new BusinessRuleException(
                    "Project must contain at least one milestone"
            );
        }

        project.setStatus(ProjectStatus.PENDING);

        return ProjectResponse.from(project);
    }

    public void delete(Long projectId, String username) {
        projectRepository.delete(requireOwnedDraft(projectId, username));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listForReview(ProjectStatus status) {
        List<Project> projects = status == null ? projectRepository.findAllByOrderByUpdatedAtDesc()
                : projectRepository.findByStatusOrderByUpdatedAtDesc(status);
        return projects.stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse review(Long projectId, boolean approved, String note, String username) {
        User reviewer = requireRole(username, RoleEnum.HEAD_DEPT);
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() != ProjectStatus.PENDING) throw new BusinessRuleException("Only pending projects can be reviewed");
        project.setStatus(approved ? ProjectStatus.APPROVED : ProjectStatus.DENIED);
        project.setReviewedBy(reviewer.getId());
        project.setReviewNote(clean(note));
        project.setReviewedAt(java.time.LocalDateTime.now());
        return ProjectResponse.from(project);
    }

    public void assignToClassroom(Long projectId, Long classroomId, String username) {
        User actor = requireUser(username);
        if (actor.getRole() != RoleEnum.HEAD_DEPT && actor.getRole() != RoleEnum.LECTURER)
            throw new AccessDeniedException("Only head department or lecturer can assign projects");
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() != ProjectStatus.APPROVED) throw new BusinessRuleException("Only approved projects can be assigned");
        Classroom classroom = classroomRepository.findDetailedById(classroomId).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if (!classroom.getSubject().getId().equals(project.getSubjectId())) throw new BusinessRuleException("Project and classroom must belong to the same subject");
        if (actor.getRole() == RoleEnum.LECTURER && classroom.getLecturers().stream().noneMatch(user -> user.getId().equals(actor.getId())))
            throw new AccessDeniedException("Lecturer is not assigned to this classroom");
        if (classroomProjectRepository.existsByClassroomIdAndProjectId(classroomId, projectId)) return;
        ClassroomProject assignment = new ClassroomProject(); assignment.setClassroomId(classroomId); assignment.setProjectId(projectId); assignment.setAssignedBy(actor.getId());
        classroomProjectRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listClassroomProjects(Long classroomId, String username) {
        User actor = requireUser(username);
        Classroom classroom = classroomRepository.findDetailedById(classroomId).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        boolean privileged = actor.getRole() == RoleEnum.HEAD_DEPT || actor.getRole() == RoleEnum.STAFF || actor.getRole() == RoleEnum.ADMIN;
        boolean member = classroom.getLecturers().stream().anyMatch(u -> u.getId().equals(actor.getId())) || classroom.getStudents().stream().anyMatch(u -> u.getId().equals(actor.getId()));
        if (!privileged && !member) throw new AccessDeniedException("You cannot access this classroom");
        return classroomProjectRepository.findByClassroomIdOrderByAssignedAtDesc(classroomId).stream()
                .map(item -> projectRepository.findById(item.getProjectId()).orElse(null)).filter(java.util.Objects::nonNull).map(ProjectResponse::from).toList();
    }

    private Project requireOwnedDraft(Long projectId, String username) {
        Project project = requireOwnedProject(projectId, username);
        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BusinessRuleException("Only draft projects can be changed");
        }
        return project;
    }

    private Project requireOwnedProject(
            Long projectId,
            String username
    ) {
        User lecturer = requireLecturer(username);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project not found"));

        if (!project.getCreatedBy().equals(lecturer.getId())) {
            throw new BusinessRuleException(
                    "You cannot manage this project"
            );
        }

        return project;
    }

    private User requireLecturer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getRole() != RoleEnum.LECTURER) {
            throw new BusinessRuleException(
                    "Only lecturers can manage projects"
            );
        }

        return user;
    }

    private User requireUser(String username) { return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
    private User requireRole(String username, RoleEnum role) {
        User user = requireUser(username);
        if (user.getRole() != role) throw new AccessDeniedException("Required role: " + role);
        return user;
    }

    private String clean(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private List<String> cleanObjectives(List<String> objectives) {
        return new ArrayList<>(objectives.stream().map(String::trim).distinct().toList());
    }

    private void replaceMilestones(Project project, List<MilestoneRequest> requests) {
        project.clearMilestones();
        for (int index = 0; index < requests.size(); index++) {
            MilestoneRequest item = requests.get(index);
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setTitle(item.title().trim());
            milestone.setDescription(clean(item.description()));
            milestone.setDueOffsetDays(item.dueOffsetDays());
            milestone.setDisplayOrder(index);
            project.addMilestone(milestone);
        }
    }
}
