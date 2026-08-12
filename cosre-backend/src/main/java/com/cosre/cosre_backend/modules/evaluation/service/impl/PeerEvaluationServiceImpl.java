package com.cosre.cosre_backend.modules.evaluation.service.impl;

import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.evaluation.dto.request.PeerEvaluationSubmitRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.PeerEvaluationResponse;
import com.cosre.cosre_backend.modules.evaluation.dto.response.StudentEvaluationSummaryResponse;
import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationCriteria;
import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationStatus;
import com.cosre.cosre_backend.modules.evaluation.entity.PeerEvaluation;
import com.cosre.cosre_backend.modules.evaluation.entity.PeerEvaluationDetail;
import com.cosre.cosre_backend.modules.evaluation.repository.EvaluationCriteriaRepository;
import com.cosre.cosre_backend.modules.evaluation.repository.PeerEvaluationRepository;
import com.cosre.cosre_backend.modules.evaluation.service.PeerEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeerEvaluationServiceImpl implements PeerEvaluationService {

    private final PeerEvaluationRepository peerEvaluationRepository;
    private final EvaluationCriteriaRepository criteriaRepository;

    @Override
    @Transactional
    public PeerEvaluationResponse submit(Long evaluatorId, PeerEvaluationSubmitRequest request) {
        if (evaluatorId.equals(request.getEvaluateeId())) {
            throw new IllegalArgumentException("Không thể tự đánh giá bản thân");
        }

        // Nạp toàn bộ tiêu chí liên quan 1 lần để tránh N+1 query
        List<Long> criteriaIds = request.getDetails().stream()
                .map(PeerEvaluationSubmitRequest.DetailItem::getCriteriaId)
                .collect(Collectors.toList());
        Map<Long, EvaluationCriteria> criteriaMap = criteriaRepository.findAllById(criteriaIds).stream()
                .collect(Collectors.toMap(EvaluationCriteria::getId, c -> c));

        for (PeerEvaluationSubmitRequest.DetailItem item : request.getDetails()) {
            EvaluationCriteria criteria = criteriaMap.get(item.getCriteriaId());
            if (criteria == null) {
                throw new ResourceNotFoundException("Không tìm thấy tiêu chí id=" + item.getCriteriaId());
            }
            if (item.getScore().compareTo(BigDecimal.ZERO) < 0
                    || item.getScore().compareTo(criteria.getMaxScore()) > 0) {
                throw new IllegalArgumentException(
                        "Điểm cho tiêu chí '" + criteria.getTitle() + "' phải trong khoảng 0 - " + criteria.getMaxScore());
            }
        }

        // Upsert: nếu đã có bài đánh giá cho cặp evaluator-evaluatee-milestone này thì cập nhật lại,
        // trừ khi bài đó đã bị LOCKED.
        PeerEvaluation peerEvaluation = peerEvaluationRepository
                .findByEvaluatorIdAndEvaluateeIdAndProjectIdAndMilestoneId(
                        evaluatorId, request.getEvaluateeId(), request.getProjectId(), request.getMilestoneId())
                .orElse(null);

        if (peerEvaluation != null) {
            if (peerEvaluation.getStatus() == EvaluationStatus.LOCKED) {
                throw new IllegalStateException("Bài đánh giá này đã bị khóa, không thể chỉnh sửa");
            }
            peerEvaluation.clearDetails();
        } else {
            peerEvaluation = PeerEvaluation.builder()
                    .projectId(request.getProjectId())
                    .milestoneId(request.getMilestoneId())
                    .teamId(request.getTeamId())
                    .evaluatorId(evaluatorId)
                    .evaluateeId(request.getEvaluateeId())
                    .status(EvaluationStatus.DRAFT)
                    .build();
        }

        peerEvaluation.setComment(request.getComment());
        peerEvaluation.setStatus(EvaluationStatus.SUBMITTED);
        peerEvaluation.setSubmittedAt(LocalDateTime.now());

        BigDecimal weightedTotal = BigDecimal.ZERO;
        for (PeerEvaluationSubmitRequest.DetailItem item : request.getDetails()) {
            EvaluationCriteria criteria = criteriaMap.get(item.getCriteriaId());
            PeerEvaluationDetail detail = PeerEvaluationDetail.builder()
                    .criteria(criteria)
                    .score(item.getScore())
                    .comment(item.getComment())
                    .build();
            peerEvaluation.addDetail(detail);

            // Quy điểm về thang 10 theo tỉ lệ score/maxScore, rồi nhân weight của tiêu chí
            BigDecimal ratio = item.getScore().divide(criteria.getMaxScore(), 4, RoundingMode.HALF_UP);
            weightedTotal = weightedTotal.add(ratio.multiply(criteria.getWeight()).multiply(BigDecimal.TEN));
        }
        peerEvaluation.setTotalScore(weightedTotal.setScale(2, RoundingMode.HALF_UP));

        return toResponse(peerEvaluationRepository.save(peerEvaluation));
    }

    @Override
    public List<PeerEvaluationResponse> getGivenEvaluations(Long evaluatorId, Long projectId) {
        return peerEvaluationRepository.findByEvaluatorIdAndProjectId(evaluatorId, projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PeerEvaluationResponse> getReceivedEvaluations(Long evaluateeId, Long projectId) {
        return peerEvaluationRepository.findByEvaluateeIdAndProjectId(evaluateeId, projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public StudentEvaluationSummaryResponse getStudentSummary(Long studentId, Long teamId, Long projectId) {
        List<PeerEvaluation> received = peerEvaluationRepository
                .findByEvaluateeIdAndTeamIdAndProjectIdAndStatus(studentId, teamId, projectId, EvaluationStatus.SUBMITTED);
        received.addAll(peerEvaluationRepository
                .findByEvaluateeIdAndTeamIdAndProjectIdAndStatus(studentId, teamId, projectId, EvaluationStatus.LOCKED));

        return buildSummary(studentId, teamId, projectId, received);
    }

    @Override
    public List<StudentEvaluationSummaryResponse> getTeamSummary(Long teamId, Long projectId, List<Long> memberIds) {
        return memberIds.stream()
                .map(memberId -> getStudentSummary(memberId, teamId, projectId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void lockEvaluations(Long teamId, Long projectId) {
        List<PeerEvaluation> evaluations = peerEvaluationRepository.findByTeamIdAndProjectId(teamId, projectId);
        for (PeerEvaluation pe : evaluations) {
            if (pe.getStatus() == EvaluationStatus.SUBMITTED) {
                pe.setStatus(EvaluationStatus.LOCKED);
            }
        }
        peerEvaluationRepository.saveAll(evaluations);
    }

    private StudentEvaluationSummaryResponse buildSummary(Long studentId, Long teamId, Long projectId,
                                                            List<PeerEvaluation> received) {
        if (received.isEmpty()) {
            return StudentEvaluationSummaryResponse.builder()
                    .studentId(studentId)
                    .teamId(teamId)
                    .projectId(projectId)
                    .averageScore(BigDecimal.ZERO)
                    .totalReviewsReceived(0)
                    .criteriaAverageBreakdown(Map.of())
                    .receivedEvaluations(List.of())
                    .build();
        }

        BigDecimal sumTotal = received.stream()
                .map(PeerEvaluation::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sumTotal.divide(BigDecimal.valueOf(received.size()), 2, RoundingMode.HALF_UP);

        // Tính điểm trung bình theo từng tiêu chí (criteriaId -> avg score)
        Map<Long, List<BigDecimal>> scoresByCriteria = new HashMap<>();
        for (PeerEvaluation pe : received) {
            for (PeerEvaluationDetail d : pe.getDetails()) {
                scoresByCriteria
                        .computeIfAbsent(d.getCriteria().getId(), k -> new java.util.ArrayList<>())
                        .add(d.getScore());
            }
        }
        Map<Long, BigDecimal> breakdown = scoresByCriteria.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP)
                ));

        return StudentEvaluationSummaryResponse.builder()
                .studentId(studentId)
                .teamId(teamId)
                .projectId(projectId)
                .averageScore(average)
                .totalReviewsReceived(received.size())
                .criteriaAverageBreakdown(breakdown)
                .receivedEvaluations(received.stream().map(this::toResponse).collect(Collectors.toList()))
                .build();
    }

    private PeerEvaluationResponse toResponse(PeerEvaluation pe) {
        List<PeerEvaluationResponse.DetailResponse> detailResponses = pe.getDetails().stream()
                .map(d -> PeerEvaluationResponse.DetailResponse.builder()
                        .criteriaId(d.getCriteria().getId())
                        .criteriaTitle(d.getCriteria().getTitle())
                        .score(d.getScore())
                        .maxScore(d.getCriteria().getMaxScore())
                        .comment(d.getComment())
                        .build())
                .collect(Collectors.toList());

        return PeerEvaluationResponse.builder()
                .id(pe.getId())
                .projectId(pe.getProjectId())
                .milestoneId(pe.getMilestoneId())
                .teamId(pe.getTeamId())
                .evaluatorId(pe.getEvaluatorId())
                .evaluateeId(pe.getEvaluateeId())
                .status(pe.getStatus())
                .totalScore(pe.getTotalScore())
                .comment(pe.getComment())
                .submittedAt(pe.getSubmittedAt())
                .details(detailResponses)
                .build();
    }
}
