package com.cosre.cosre_backend.modules.evaluation.repository;

import com.cosre.cosre_backend.modules.evaluation.entity.PeerEvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeerEvaluationDetailRepository extends JpaRepository<PeerEvaluationDetail, Long> {

    List<PeerEvaluationDetail> findByPeerEvaluationId(Long peerEvaluationId);
}
