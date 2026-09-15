package com.cosre.cosre_backend.modules.evaluation.repository;

import com.cosre.cosre_backend.modules.evaluation.entity.PeerEvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeerEvaluationDetailRepository extends JpaRepository<PeerEvaluationDetail, Long> {

    List<PeerEvaluationDetail> findByPeerEvaluationId(Long peerEvaluationId);

    // Dùng để kiểm tra 1 tiêu chí (criteria) đã từng được dùng để chấm điểm hay chưa.
    // Nếu đã tồn tại >=1 bản ghi thì tiêu chí đó bị khóa: không được đổi weight/maxScore hoặc xóa,
    // nhằm tránh làm sai lệch các điểm đã chấm dựa trên thang điểm/trọng số cũ.
    boolean existsByCriteriaId(Long criteriaId);
}
