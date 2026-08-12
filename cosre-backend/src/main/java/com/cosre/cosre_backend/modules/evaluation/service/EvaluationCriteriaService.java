package com.cosre.cosre_backend.modules.evaluation.service;

import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.CriteriaResponse;

import java.util.List;

public interface EvaluationCriteriaService {

    CriteriaResponse create(CriteriaRequest request, Long lecturerId);

    CriteriaResponse update(Long id, CriteriaRequest request, Long lecturerId);

    void delete(Long id, Long lecturerId);

    List<CriteriaResponse> getByProject(Long projectId);
}
