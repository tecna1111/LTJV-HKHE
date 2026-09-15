package com.cosre.cosre_backend.modules.resource.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.resource.repository.ResourceRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTests {
    @Mock ResourceRepository resourceRepository;
    @Mock ClassroomRepository classroomRepository;
    @Mock TeamRepository teamRepository;
    @Mock UserRepository userRepository;
    @Mock com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository milestones;
    @Mock com.cosre.cosre_backend.modules.checkpoint.repository.CheckpointRepository checkpoints;
    @TempDir Path storage;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resourceRepository, classroomRepository, teamRepository,
                userRepository, milestones, checkpoints, storage.toString(), 10);
    }

    @Test
    void uploadRejectsFileLargerThanConfiguredLimit() {
        User staff = user(1L, "staff", RoleEnum.STAFF);
        when(classroomRepository.findDetailedById(2L)).thenReturn(Optional.of(new Classroom()));
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        var file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[11]);

        assertThatThrownBy(() -> service.uploadClassMaterial(2L, null, null, file, "staff"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("maximum allowed size");
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void studentCannotListResourcesOfUnassignedClassroom() {
        User student = user(3L, "student", RoleEnum.STUDENT);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(classroomRepository.findDetailedById(9L)).thenReturn(Optional.of(new Classroom()));

        assertThatThrownBy(() -> service.listByClassroom(9L, "student"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(resourceRepository);
    }

    @Test
    void outsiderCannotDownloadTeamFile() {
        var student=user(3L,"student",RoleEnum.STUDENT);
        var lecturer=user(4L,"lecturer",RoleEnum.LECTURER);
        var team=new com.cosre.cosre_backend.modules.team.entity.Team();team.setLecturer(lecturer);
        var resource=new com.cosre.cosre_backend.modules.resource.entity.ResourceFile();resource.setTeamId(7L);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(resourceRepository.findById(8L)).thenReturn(Optional.of(resource));
        when(teamRepository.findById(7L)).thenReturn(Optional.of(team));
        assertThatThrownBy(() -> service.loadForDownload(8L,"student")).isInstanceOf(AccessDeniedException.class);
    }
    @Test
    void failedMetadataSaveRemovesPhysicalUpload() throws Exception {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user(1L,"staff",RoleEnum.STAFF)));
        when(classroomRepository.findDetailedById(2L)).thenReturn(Optional.of(new Classroom()));
        when(resourceRepository.save(any())).thenThrow(new IllegalStateException("DB failed"));
        var file=new MockMultipartFile("file","ok.txt","text/plain",new byte[2]);
        assertThatThrownBy(() -> service.uploadClassMaterial(2L,null,null,file,"staff")).hasMessage("DB failed");
        try(var paths=java.nio.file.Files.list(storage)) { org.assertj.core.api.Assertions.assertThat(paths.count()).isZero(); }
    }
    @Test
    void downloadRejectsTraversalInStoredMetadata() {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user(1L,"staff",RoleEnum.STAFF)));
        when(classroomRepository.findDetailedById(2L)).thenReturn(Optional.of(new Classroom()));
        var resource=new com.cosre.cosre_backend.modules.resource.entity.ResourceFile();resource.setClassroomId(2L);resource.setStoredFileName("../secret.txt");
        when(resourceRepository.findById(8L)).thenReturn(Optional.of(resource));
        assertThatThrownBy(() -> service.loadForDownload(8L,"staff")).hasMessageContaining("Invalid storage path");
    }

    @Test
    void metadataCannotLinkCheckpointOfAnotherTeam() {
        var staff=user(1L,"staff",RoleEnum.STAFF);
        var team=new com.cosre.cosre_backend.modules.team.entity.Team();
        var resource=new com.cosre.cosre_backend.modules.resource.entity.ResourceFile();resource.setTeamId(2L);
        var checkpoint=new com.cosre.cosre_backend.modules.checkpoint.entity.Checkpoint();checkpoint.setTeamId(99L);
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(resourceRepository.findById(8L)).thenReturn(Optional.of(resource));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        var lecturer=user(3L,"lecturer",RoleEnum.LECTURER);team.setLecturer(lecturer);
        when(checkpoints.findById(9L)).thenReturn(Optional.of(checkpoint));
        assertThatThrownBy(() -> service.updateMetadata(8L,"Title",null,null,9L,"staff")).hasMessageContaining("another team");
    }
    @Test
    void unassignedLecturerCannotUploadClassMaterial() {
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(user(1L,"lecturer",RoleEnum.LECTURER)));
        when(classroomRepository.findDetailedById(2L)).thenReturn(Optional.of(new Classroom()));
        assertThatThrownBy(() -> service.uploadClassMaterial(2L,"Title",null,new MockMultipartFile("file",new byte[1]),"lecturer"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(resourceRepository);
    }

    private User user(Long id, String username, RoleEnum role) {
        User value = new User(); value.setId(id); value.setUsername(username); value.setRole(role); value.setActive(true);
        return value;
    }
}
