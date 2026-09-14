package com.cosre.cosre_backend.modules.evaluation.service.impl;

import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.CriteriaResponse;
import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationCriteria;
import com.cosre.cosre_backend.modules.evaluation.repository.EvaluationCriteriaRepository;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationCriteriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationCriteriaServiceImpl implements EvaluationCriteriaService {

    private final EvaluationCriteriaRepository criteriaRepository;

    @Override
    @Transactional
    public CriteriaResponse create(CriteriaRequest request, Long lecturerId) {
        if (criteriaRepository.existsByProjectIdAndTitleIgnoreCase(request.getProjectId(), request.getTitle())) {
            throw new IllegalArgumentException("Tiêu chí '" + request.getTitle() + "' đã tồn tại trong project này");
        }
        validateWeightBudget(request.getProjectId(), request.getWeight(), null);

        EvaluationCriteria criteria = EvaluationCriteria.builder()
                .projectId(request.getProjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .maxScore(request.getMaxScore())
                .weight(request.getWeight())
                .createdBy(lecturerId)
                .build();

        return toResponse(criteriaRepository.save(criteria));
    }

    @Override
    @Transactional
    public CriteriaResponse update(Long id, CriteriaRequest request, Long lecturerId) {
        EvaluationCriteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí id=" + id));

        validateWeightBudget(request.getProjectId(), request.getWeight(), id);

        criteria.setTitle(request.getTitle());
        criteria.setDescription(request.getDescription());
        criteria.setMaxScore(request.getMaxScore());
        criteria.setWeight(request.getWeight());

        return toResponse(criteriaRepository.save(criteria));
    }

    @Override
    @Transactional
    public void delete(Long id, Long lecturerId) {
        EvaluationCriteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí id=" + id));
        criteriaRepository.delete(criteria);
    }

    @Override
    public List<CriteriaResponse> getByProject(Long projectId) {
        return criteriaRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đảm bảo tổng weight của các tiêu chí trong 1 project không vượt quá 1.0
     * (excludeId dùng khi update để không tự cộng trùng chính nó).
     */
    private void validateWeightBudget(Long projectId, BigDecimal newWeight, Long excludeId) {
        BigDecimal currentTotal = criteriaRepository.findByProjectId(projectId).stream()
                .filter(c -> excludeId == null || !c.getId().equals(excludeId))
                .map(EvaluationCriteria::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = currentTotal.add(newWeight);
        if (newTotal.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "Tổng weight của các tiêu chí vượt quá 1.0 (hiện tại: " + newTotal + ")");
        }
    }

    private CriteriaResponse toResponse(EvaluationCriteria c) {
        return CriteriaResponse.builder()
                .id(c.getId())
                .projectId(c.getProjectId())
                .title(c.getTitle())
                .description(c.getDescription())
                .maxScore(c.getMaxScore())
                .weight(c.getWeight())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
