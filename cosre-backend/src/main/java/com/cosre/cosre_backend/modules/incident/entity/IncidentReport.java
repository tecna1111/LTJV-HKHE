package com.cosre.cosre_backend.modules.incident.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "incident_reports") @Getter @Setter
public class IncidentReport {
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long reporterId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 5000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(length = 2000) private String resolution;
    private Long handledBy;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Version private long version;
}
