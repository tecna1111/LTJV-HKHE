package com.cosre.cosre_backend.modules.project.entity;

import java.io.Serializable;
import java.util.Objects;

public class ClassroomProjectId implements Serializable {
    private Long classroomId;
    private Long projectId;
    public ClassroomProjectId() {}
    public ClassroomProjectId(Long classroomId, Long projectId) { this.classroomId = classroomId; this.projectId = projectId; }
    @Override public boolean equals(Object other) { return other instanceof ClassroomProjectId id && Objects.equals(classroomId, id.classroomId) && Objects.equals(projectId, id.projectId); }
    @Override public int hashCode() { return Objects.hash(classroomId, projectId); }
}
