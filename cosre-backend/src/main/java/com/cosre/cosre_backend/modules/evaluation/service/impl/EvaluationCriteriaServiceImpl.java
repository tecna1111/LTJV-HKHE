package com.cosre.cosre_backend.modules.evaluation.service.impl;

import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.CriteriaResponse;
import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationCriteria;
import com.cosre.cosre_backend.modules.evaluation.repository.EvaluationCriteriaRepository;
import com.cosre.cosre_backend.modules.evaluation.repository.PeerEvaluationDetailRepository;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationCriteriaService;
import com.cosre.cosre_backend.modules.project.entity.Project;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;
    private final PeerEvaluationDetailRepository peerEvaluationDetailRepository;
    private final com.cosre.cosre_backend.modules.evaluation.service.EvaluationAccessService access;
    private final com.cosre.cosre_backend.modules.evaluation.repository.PeerEvaluationRepository evaluations;

    @Override
    @Transactional
    public CriteriaResponse create(CriteriaRequest request, Long lecturerId) {
        access.lockProject(request.getProjectId());
        requireOwnedProject(request.getProjectId(), lecturerId);

        if (criteriaRepository.existsByProjectIdAndTitleIgnoreCase(request.getProjectId(), request.getTitle())) {
            throw new IllegalArgumentException("Tiêu chí '" + request.getTitle() + "' đã tồn tại trong project này");
        }
        if (evaluations.existsByProjectId(request.getProjectId()))
            throw new BusinessRuleException("Cannot add criteria after evaluation has started");
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
        access.lockProject(request.getProjectId());
        EvaluationCriteria criteria = criteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí id=" + id));

        // Không cho phép "chuyển" tiêu chí sang project khác qua request.projectId.
        if (!criteria.getProjectId().equals(request.getProjectId())) {
            throw new BusinessRuleException("Không thể đổi projectId của một tiêu chí đã tồn tại");
        }

        requireOwnedProject(request.getProjectId(), lecturerId);

        if (!criteria.getTitle().equalsIgnoreCase(request.getTitle())
                && criteriaRepository.existsByProjectIdAndTitleIgnoreCase(request.getProjectId(), request.getTitle())) {
            throw new IllegalArgumentException("Tiêu chí '" + request.getTitle() + "' đã tồn tại trong project này");
        }

        boolean alreadyEvaluated = peerEvaluationDetailRepository.existsByCriteriaId(id);
        boolean scaleChanged = criteria.getMaxScore().compareTo(request.getMaxScore()) != 0;
        boolean weightChanged = criteria.getWeight().compareTo(request.getWeight()) != 0;

        // Quy tắc: tiêu chí đã được dùng để chấm ít nhất 1 bài đánh giá thì KHÔNG được
        // đổi trọng số (weight) hoặc thang điểm (maxScore) nữa — vì các bài đã chấm đã
        // tính điểm dựa trên thang/weight cũ, đổi sẽ làm sai lệch dữ liệu lịch sử.
        // Chỉ còn được sửa tiêu đề/mô tả (ví dụ sửa lỗi chính tả).
        if (alreadyEvaluated && (scaleChanged || weightChanged)) {
            throw new BusinessRuleException(
                    "Tiêu chí '" + criteria.getTitle() + "' đã có bài đánh giá sử dụng, "
                            + "không thể thay đổi trọng số hoặc thang điểm. Chỉ có thể sửa tiêu đề/mô tả.");
        }

        if (weightChanged) {
            validateWeightBudget(request.getProjectId(), request.getWeight(), id);
        }

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

        access.lockProject(criteria.getProjectId());
        requireOwnedProject(criteria.getProjectId(), lecturerId);

        // Quy tắc: không cho xóa tiêu chí đã có bài đánh giá tham chiếu tới, để tránh
        // vỡ dữ liệu lịch sử (PeerEvaluationDetail.criteria là NOT NULL, và xóa criteria
        // cũng làm sai tổng điểm/weight đã dùng để tính các bài đánh giá cũ).
        if (peerEvaluationDetailRepository.existsByCriteriaId(id)) {
            throw new BusinessRuleException(
                    "Không thể xóa tiêu chí '" + criteria.getTitle() + "' vì đã có bài đánh giá sử dụng");
        }

        criteriaRepository.delete(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriteriaResponse> getByProject(Long projectId) {
        access.viewProject(projectId);
        return criteriaRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chỉ giảng viên là chủ sở hữu (người tạo) project mới được tạo/sửa/xóa tiêu chí
     * đánh giá của project đó.
     */
    private Project requireOwnedProject(Long projectId, Long lecturerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project id=" + projectId));

        if (!project.getCreatedBy().equals(lecturerId)) {
            throw new BusinessRuleException("Bạn không có quyền quản lý tiêu chí của project này");
        }

        return project;
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
                .locked(peerEvaluationDetailRepository.existsByCriteriaId(c.getId()))
                .build();
    }
}
