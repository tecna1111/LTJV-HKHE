package com.cosre.cosre_backend.modules.resource.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.resource.entity.*;
import com.cosre.cosre_backend.modules.resource.repository.ResourceRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository;
import com.cosre.cosre_backend.modules.checkpoint.repository.CheckpointRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service @Transactional
public class ResourceService {
    private final ResourceRepository resources;
    private final ClassroomRepository classrooms;
    private final TeamRepository teams;
    private final UserRepository users;
    private final ProjectMilestoneRepository milestones;
    private final CheckpointRepository checkpoints;
    private final Path root;
    private final long limit;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResourceService.class);

    public ResourceService(ResourceRepository resources, ClassroomRepository classrooms, TeamRepository teams,
            UserRepository users, ProjectMilestoneRepository milestones, CheckpointRepository checkpoints,
            @Value("${app.upload.dir:uploads}") String directory,
            @Value("${app.upload.max-file-size-bytes}") long limit) {
        this.resources=resources; this.classrooms=classrooms; this.teams=teams; this.users=users;
        this.milestones=milestones; this.checkpoints=checkpoints; this.limit=limit;
        try { this.root=Files.createDirectories(Paths.get(directory).toAbsolutePath().normalize()).toRealPath(); }
        catch(IOException e) { throw new IllegalStateException("Cannot create storage directory", e); }
    }
    private User user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    private boolean staff(User user) { return user.getRole()==RoleEnum.ADMIN || user.getRole()==RoleEnum.STAFF; }
    private void classroom(Long id, User user, boolean write) {
        var classroom=classrooms.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if(staff(user)) return;
        boolean lecturer=user.getRole()==RoleEnum.LECTURER && classroom.getLecturers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        boolean student=!write && user.getRole()==RoleEnum.STUDENT && classroom.getStudents().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if(!lecturer && !student) throw new AccessDeniedException("You are not assigned to this classroom");
    }
    private Team team(Long id, User user) {
        var team=teams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        if(!staff(user) && !team.getLecturer().getId().equals(user.getId()) && team.getMembers().stream().noneMatch(u -> u.getId().equals(user.getId())))
            throw new AccessDeniedException("You are not assigned to this team");
        return team;
    }
    private void view(ResourceFile file, User user) {
        if(file.getTeamId()!=null) team(file.getTeamId(), user);
        else if(file.getClassroomId()!=null) classroom(file.getClassroomId(), user, false);
        else throw new AccessDeniedException("Resource has no valid scope");
    }
    private void manage(ResourceFile file, User user) {
        view(file,user);
        boolean lecturer=file.getTeamId()!=null ? teams.findById(file.getTeamId()).orElseThrow().getLecturer().getId().equals(user.getId())
                : user.getRole()==RoleEnum.LECTURER;
        if(!staff(user) && !lecturer && !file.getUploadedBy().getId().equals(user.getId())) throw new AccessDeniedException("Cannot manage this resource");
    }
    public ResourceFile uploadClassMaterial(Long id, String title, String description, MultipartFile file, String username) {
        var user=user(username); classroom(id,user,true);
        return store(file,title,description,ResourceCategory.CLASS_MATERIAL,id,null,user);
    }
    public ResourceFile uploadTeamSubmission(Long id, String title, String description, MultipartFile file, String username) {
        var user=user(username); team(id,user);
        return store(file,title,description,ResourceCategory.TEAM_SUBMISSION,null,id,user);
    }
    @Transactional(readOnly=true)
    public List<ResourceFile> listByClassroom(Long id, String username) {
        classroom(id,user(username),false);
        return initialize(resources.findByClassroomIdOrderByCreatedAtDesc(id));
    }
    @Transactional(readOnly=true)
    public List<ResourceFile> listByTeam(Long id, String username) {
        team(id,user(username)); return initialize(resources.findByTeamIdOrderByCreatedAtDesc(id));
    }
    private List<ResourceFile> initialize(List<ResourceFile> files) {
        files.forEach(f -> f.getUploadedBy().getUsername()); return files;
    }
    @Transactional(readOnly=true)
    public ResourceFile metadata(Long id, String username) {
        var file=require(id); view(file,user(username)); file.getUploadedBy().getUsername(); return file;
    }
    public ResourceFile updateMetadata(Long id, String title, String description, Long milestoneId, Long checkpointId, String username) {
        var file=require(id); var user=user(username); manage(file,user);
        validateMetadata(title,description);
        if(milestoneId!=null || checkpointId!=null) {
            if(file.getTeamId()==null) throw new BusinessRuleException("Milestone/checkpoint links require a team resource");
            var team=team(file.getTeamId(),user);
            if(checkpointId!=null) {
                var checkpoint=checkpoints.findById(checkpointId).orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
                if(!Objects.equals(checkpoint.getTeamId(),team.getId())) throw new BusinessRuleException("Checkpoint belongs to another team");
                if(milestoneId!=null && !Objects.equals(milestoneId,checkpoint.getMilestoneId())) throw new BusinessRuleException("Checkpoint/milestone mismatch");
                milestoneId=checkpoint.getMilestoneId();
            }
            if(milestoneId!=null) {
                var milestone=milestones.findById(milestoneId).orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
                if(!Objects.equals(milestone.getProject().getId(),team.getProjectId())) throw new BusinessRuleException("Milestone belongs to another project");
            }
        }
        if(title==null || title.isBlank()) throw new BusinessRuleException("Title is required");
        file.setTitle(title.trim()); file.setDescription(description); file.setMilestoneId(milestoneId); file.setCheckpointId(checkpointId);
        file.getUploadedBy().getUsername(); return resources.save(file);
    }
    @Transactional(readOnly=true)
    public FileDownload loadForDownload(Long id, String username) {
        var file=require(id); view(file,user(username)); var path=path(file.getStoredFileName());
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) throw new ResourceNotFoundException("File not found on server");
        return new FileDownload(file,path);
    }
    public void delete(Long id, String username) {
        var file=require(id); manage(file,user(username)); var path=path(file.getStoredFileName());
        resources.delete(file);
        // Keep the physical file if the database transaction rolls back.
        if(TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { cleanup(path); }
            });
        } else cleanup(path);
    }
    private void cleanup(Path path) {
        try { Files.deleteIfExists(path); }
        catch(IOException e) { log.error("Storage cleanup failed for {}",path,e); }
    }
    private Path path(String name) {
        Path path=root.resolve(name).normalize();
        if(!path.getParent().equals(root) || Files.isSymbolicLink(path)) throw new BusinessRuleException("Invalid storage path");
        return path;
    }
    private void validateMetadata(String title, String description) {
        if(title!=null && title.length()>255 || description!=null && description.length()>1000) throw new BusinessRuleException("Resource metadata is too long");
    }
    private ResourceFile store(MultipartFile file, String title, String description, ResourceCategory category, Long classroomId, Long teamId, User user) {
        if(file==null || file.isEmpty()) throw new BusinessRuleException("File is required");
        if(file.getSize()>limit) throw new BusinessRuleException("File exceeds the maximum allowed size of " + limit + " bytes");
        validateMetadata(title,description);
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("file").replace('\\','/');
        original=original.substring(original.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
        if(original.isBlank()) original="file";
        if(original.length()>255 || file.getContentType()!=null && file.getContentType().length()>150) throw new BusinessRuleException("File metadata is too long");
        String stored=UUID.randomUUID().toString(); Path target=path(stored);
        try(var in=file.getInputStream(); var out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
            byte[] buffer=new byte[8192]; long size=0; int count;
            while((count=in.read(buffer))!=-1) {
                size+=count; if(size>limit) throw new BusinessRuleException("File exceeds the maximum allowed size");
                out.write(buffer,0,count);
            }
        } catch(RuntimeException e) { cleanup(target); throw e; }
        catch(IOException e) { cleanup(target); throw new IllegalStateException("Failed to store file",e); }
        if(TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) cleanup(target); }
            });
        }
        var resource=new ResourceFile(); resource.setTitle(title==null || title.isBlank()?original:title.trim());
        resource.setDescription(description); resource.setCategory(category); resource.setClassroomId(classroomId); resource.setTeamId(teamId);
        resource.setOriginalFileName(original); resource.setStoredFileName(stored); resource.setContentType(file.getContentType());
        try { resource.setFileSize(Files.size(target)); } catch(IOException e) { cleanup(target); throw new IllegalStateException(e); }
        resource.setUploadedBy(user);
        try { return resources.save(resource); } catch(RuntimeException e) { cleanup(target); throw e; }
    }
    private ResourceFile require(Long id) { return resources.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found")); }
    public record FileDownload(ResourceFile resource,Path path) {}
}
