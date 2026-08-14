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

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    public ProjectResponse create(
            CreateProjectRequest request,
            String username
    ) {
        User lecturer = requireLecturer(username);

        if (!subjectRepository.existsById(request.subjectId())) {
            throw new ResourceNotFoundException("Subject not found");
        }

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

        String title = request.title().trim();
        if (projectRepository.existsBySubjectIdAndTitleIgnoreCaseAndIdNot(
                request.subjectId(), title, projectId)) {
            throw new BusinessRuleException("Project title already exists in this subject");
        }

        project.setTitle(title);
        project.setDescription(clean(request.description()));
        project.setSubjectId(request.subjectId());
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
