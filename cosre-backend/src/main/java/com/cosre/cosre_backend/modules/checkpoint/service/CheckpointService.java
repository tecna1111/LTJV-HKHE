package com.cosre.cosre_backend.modules.checkpoint.service;

import com.cosre.cosre_backend.modules.checkpoint.dto.*;
import com.cosre.cosre_backend.modules.checkpoint.entity.*;
import com.cosre.cosre_backend.modules.checkpoint.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckpointService {

    private final CheckpointRepository checkpointRepository;
    private final CheckpointAssignmentRepository assignmentRepository;
    private final CheckpointSubmissionRepository submissionRepository;

    @Transactional
    public CheckpointResponse createCheckpoint(CreateCheckpointRequest request, Long currentUserId) {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTeamId(request.teamId());
        checkpoint.setMilestoneId(request.milestoneId());
        checkpoint.setTitle(request.title());
        checkpoint.setDescription(request.description());
        checkpoint.setDueAt(request.dueAt());
        checkpoint.setCreatedBy(currentUserId);
        checkpoint.setStatus(CheckpointStatus.OPEN);

        Checkpoint savedCheckpoint = checkpointRepository.save(checkpoint);

        if (request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
            assignStudentsToCheckpoint(savedCheckpoint.getId(), request.assigneeIds());
        }

        return mapToResponse(savedCheckpoint);
    }

    @Transactional
    public CheckpointResponse updateCheckpoint(Long id, UpdateCheckpointRequest request) {
        Checkpoint checkpoint = checkpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checkpoint không tồn tại"));

        if (checkpoint.getStatus() == CheckpointStatus.APPROVED) {
            throw new IllegalStateException("Không thể chỉnh sửa Checkpoint đã được duyệt");
        }

        checkpoint.setTitle(request.title());
        checkpoint.setDescription(request.description());
        checkpoint.setDueAt(request.dueAt());

        if (request.assigneeIds() != null) {
            assignmentRepository.deleteByCheckpointId(id);
            assignStudentsToCheckpoint(id, request.assigneeIds());
        }

        return mapToResponse(checkpointRepository.save(checkpoint));
    }

    @Transactional
    public CheckpointResponse submitCheckpoint(Long checkpointId, SubmitCheckpointRequest request, Long currentUserId) {
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new RuntimeException("Checkpoint không tồn tại"));

        // Kiểm tra chuyển trạng thái hợp lệ
        if (checkpoint.getStatus() == CheckpointStatus.APPROVED) {
            throw new IllegalStateException("Checkpoint đã được duyệt, không thể nộp lại");
        }

        // Tạo bản nộp bài
        CheckpointSubmission submission = new CheckpointSubmission();
        submission.setCheckpointId(checkpointId);
        submission.setSubmittedBy(currentUserId);
        submission.setContent(request.content());
        submission.setAttachmentUrl(request.attachmentUrl());
        submissionRepository.save(submission);

        // Chuyển trạng thái: OPEN/IN_PROGRESS/REJECTED -> SUBMITTED
        checkpoint.setStatus(CheckpointStatus.SUBMITTED);
        return mapToResponse(checkpointRepository.save(checkpoint));
    }

    @Transactional
    public CheckpointResponse reviewCheckpoint(Long checkpointId, ReviewCheckpointRequest request, Long reviewerId) {
        Checkpoint checkpoint = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> new RuntimeException("Checkpoint không tồn tại"));

        if (checkpoint.getStatus() != CheckpointStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể đánh giá bài nộp ở trạng thái SUBMITTED");
        }

        List<CheckpointSubmission> submissions = submissionRepository.findByCheckpointIdOrderBySubmittedAtDesc(checkpointId);
        if (submissions.isEmpty()) {
            throw new RuntimeException("Không tìm thấy bài nộp cho Checkpoint này");
        }

        // Cập nhật kết quả review cho lần nộp mới nhất
        CheckpointSubmission latestSubmission = submissions.get(0);
        latestSubmission.setScore(request.score());
        latestSubmission.setFeedback(request.feedback());
        latestSubmission.setReviewedBy(reviewerId);
        latestSubmission.setReviewedAt(LocalDateTime.now());
        submissionRepository.save(latestSubmission);

        // Chuyển trạng thái quy tắc: SUBMITTED -> APPROVED hoặc SUBMITTED -> REJECTED
        if (Boolean.TRUE.equals(request.approved())) {
            checkpoint.setStatus(CheckpointStatus.APPROVED);
            checkpoint.setCompletedAt(LocalDateTime.now());
        } else {
            checkpoint.setStatus(CheckpointStatus.REJECTED);
        }

        return mapToResponse(checkpointRepository.save(checkpoint));
    }

    @Transactional(readOnly = true)
    public List<CheckpointResponse> getCheckpointsByTeam(Long teamId) {
        return checkpointRepository.findByTeamIdOrderByDueAtAsc(teamId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CheckpointResponse getCheckpointById(Long id) {
        Checkpoint checkpoint = checkpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checkpoint không tồn tại"));
        return mapToResponse(checkpoint);
    }

    private void assignStudentsToCheckpoint(Long checkpointId, Set<Long> studentIds) {
        List<CheckpointAssignment> assignments = studentIds.stream().map(studentId -> {
            CheckpointAssignment assignment = new CheckpointAssignment();
            assignment.setCheckpointId(checkpointId);
            assignment.setStudentId(studentId);
            return assignment;
        }).collect(Collectors.toList());

        assignmentRepository.saveAll(assignments);
    }

    private CheckpointResponse mapToResponse(Checkpoint checkpoint) {
        List<Long> assigneeIds = assignmentRepository.findByCheckpointId(checkpoint.getId())
                .stream()
                .map(CheckpointAssignment::getStudentId)
                .collect(Collectors.toList());

        return new CheckpointResponse(
                checkpoint.getId(),
                checkpoint.getTeamId(),
                checkpoint.getMilestoneId(),
                checkpoint.getTitle(),
                checkpoint.getDescription(),
                checkpoint.getDueAt(),
                checkpoint.getStatus(),
                checkpoint.getCreatedBy(),
                checkpoint.getCreatedAt(),
                checkpoint.getUpdatedAt(),
                checkpoint.getCompletedAt(),
                assigneeIds
        );
    }
}