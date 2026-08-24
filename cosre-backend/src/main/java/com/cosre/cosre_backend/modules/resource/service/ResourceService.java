package com.cosre.cosre_backend.modules.resource.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.resource.entity.ResourceCategory;
import com.cosre.cosre_backend.modules.resource.entity.ResourceFile;
import com.cosre.cosre_backend.modules.resource.repository.ResourceRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/**
 * Logic nghiệp vụ cho việc upload/download/xóa tài liệu môn học và file bài
 * nộp. File vật lý được lưu trên ổ đĩa server tại thư mục cấu hình bởi
 * {@code app.upload.dir} (mặc định "uploads" ngay tại thư mục chạy app);
 * database chỉ lưu metadata (repository pattern giống các module khác).
 */
@Service
@Transactional
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final ClassroomRepository classroomRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final Path storageRoot;
    private final long maxFileSize;

    public ResourceService(ResourceRepository resourceRepository, ClassroomRepository classroomRepository,
            TeamRepository teamRepository, UserRepository userRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.max-file-size-bytes:26214400}") long maxFileSize) {
        this.resourceRepository = resourceRepository;
        this.classroomRepository = classroomRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + storageRoot, e);
        }
    }

    // Giảng viên/Nhân viên/Admin tải tài liệu môn học lên cho một lớp học cụ thể.
    public ResourceFile uploadClassMaterial(Long classroomId, String title, String description,
            MultipartFile file, String username) {
        if (!classroomRepository.existsById(classroomId)) throw new ResourceNotFoundException("Classroom not found");
        requireClassroomAccess(classroomId, username, true);
        return store(file, title, description, ResourceCategory.CLASS_MATERIAL, classroomId, null, username);
    }

    // Giảng viên/Sinh viên tải file bài nộp lên cho một nhóm cụ thể.
    public ResourceFile uploadTeamSubmission(Long teamId, String title, String description,
            MultipartFile file, String username) {
        if (!teamRepository.existsById(teamId)) throw new ResourceNotFoundException("Team not found");
        requireTeamAccess(teamId, username);
        return store(file, title, description, ResourceCategory.TEAM_SUBMISSION, null, teamId, username);
    }

    @Transactional(readOnly = true)
    public List<ResourceFile> listByClassroom(Long classroomId, String username) {
        requireClassroomAccess(classroomId, username, false);
        return resourceRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId);
    }

    @Transactional(readOnly = true)
    public List<ResourceFile> listByTeam(Long teamId, String username) {
        requireTeamAccess(teamId, username);
        return resourceRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
    }

    // Chuẩn bị đường dẫn file vật lý trên đĩa kèm metadata để Controller stream về client.
    @Transactional(readOnly = true)
    public FileDownload loadForDownload(Long id, String username) {
        ResourceFile resource = requireResource(id);
        requireResourceAccess(resource, username);
        Path path = storageRoot.resolve(resource.getStoredFileName()).normalize();
        if (!Files.exists(path)) throw new ResourceNotFoundException("File not found on server");
        return new FileDownload(resource, path);
    }

    // Chỉ người tải lên hoặc Admin/Staff/Lecturer mới được xóa (giữ nghiệp vụ đơn giản,
    // có thể siết chặt thêm nếu cần sinh viên chỉ xóa được bài nộp của nhóm mình).
    public void delete(Long id, String username) {
        ResourceFile resource = requireResource(id);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isOwner = resource.getUploadedBy().getUsername().equals(username);
        boolean isAdministrator = user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.STAFF;
        boolean lecturerWithAccess = user.getRole() == RoleEnum.LECTURER && hasResourceAccess(resource, user);
        if (!isOwner && !isAdministrator && !lecturerWithAccess)
            throw new AccessDeniedException("You cannot delete this resource");
        try {
            Files.deleteIfExists(storageRoot.resolve(resource.getStoredFileName()));
        } catch (IOException e) {
            // Xóa file vật lý thất bại không nên chặn việc xóa bản ghi metadata;
            // ghi log ở đây nếu project đã có logger chuẩn.
        }
        resourceRepository.delete(resource);
    }

    private ResourceFile store(MultipartFile file, String title, String description, ResourceCategory category,
            Long classroomId, Long teamId, String username) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("File is required");
        if (file.getSize() > maxFileSize)
            throw new BusinessRuleException("File exceeds the maximum allowed size of 25 MB");
        User uploader = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path target = storageRoot.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        ResourceFile resource = new ResourceFile();
        resource.setTitle(title == null || title.isBlank() ? originalName : title.trim());
        resource.setDescription(description == null || description.isBlank() ? null : description.trim());
        resource.setCategory(category);
        resource.setClassroomId(classroomId);
        resource.setTeamId(teamId);
        resource.setOriginalFileName(originalName);
        resource.setStoredFileName(storedName);
        resource.setContentType(file.getContentType());
        resource.setFileSize(file.getSize());
        resource.setUploadedBy(uploader);
        try {
            return resourceRepository.save(resource);
        } catch (RuntimeException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw exception;
        }
    }

    private ResourceFile requireResource(Long id) {
        return resourceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    private void requireResourceAccess(ResourceFile resource, String username) {
        User user = requireUser(username);
        if (!hasResourceAccess(resource, user)) throw new AccessDeniedException("You cannot access this resource");
    }

    private boolean hasResourceAccess(ResourceFile resource, User user) {
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.STAFF || user.getRole() == RoleEnum.HEAD_DEPT)
            return true;
        if (resource.getCategory() == ResourceCategory.CLASS_MATERIAL) {
            return classroomRepository.findDetailedById(resource.getClassroomId())
                    .map(value -> value.getLecturers().stream().anyMatch(item -> item.getId().equals(user.getId()))
                            || value.getStudents().stream().anyMatch(item -> item.getId().equals(user.getId())))
                    .orElse(false);
        }
        return teamRepository.findById(resource.getTeamId())
                .map(value -> value.getLecturer().getId().equals(user.getId())
                        || value.getMembers().stream().anyMatch(item -> item.getId().equals(user.getId())))
                .orElse(false);
    }

    private void requireClassroomAccess(Long classroomId, String username, boolean upload) {
        User user = requireUser(username);
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.STAFF || user.getRole() == RoleEnum.HEAD_DEPT)
            return;
        var classroom = classroomRepository.findDetailedById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        boolean lecturer = classroom.getLecturers().stream().anyMatch(item -> item.getId().equals(user.getId()));
        boolean student = classroom.getStudents().stream().anyMatch(item -> item.getId().equals(user.getId()));
        if (!lecturer && (!student || upload)) throw new AccessDeniedException("You cannot access this classroom resource");
    }

    private void requireTeamAccess(Long teamId, String username) {
        User user = requireUser(username);
        var team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        boolean lecturer = team.getLecturer().getId().equals(user.getId());
        boolean member = team.getMembers().stream().anyMatch(item -> item.getId().equals(user.getId()));
        if (!lecturer && !member) throw new AccessDeniedException("You cannot access this team submission");
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Loại bỏ đường dẫn thư mục khỏi tên file gốc để tránh path traversal (../../etc).
    private String sanitizeFileName(String rawName) {
        String name = rawName == null ? "file" : Paths.get(rawName).getFileName().toString();
        return name.isBlank() ? "file" : name;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1);
    }

    public record FileDownload(ResourceFile resource, Path path) {
    }
}
