package com.cosre.cosre_backend.modules.evaluation.service;
import com.cosre.cosre_backend.modules.evaluation.service.impl.EvaluationCriteriaServiceImpl;
import com.cosre.cosre_backend.modules.evaluation.repository.*;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.project.entity.Project;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class EvaluationCriteriaServiceTests {
    @Mock EvaluationCriteriaRepository criteriaRepository; @Mock ProjectRepository projectRepository;
    @Mock PeerEvaluationDetailRepository peerEvaluationDetailRepository; @Mock EvaluationAccessService access;
    @Mock PeerEvaluationRepository evaluations;
    @InjectMocks EvaluationCriteriaServiceImpl service;
    CriteriaRequest request=new CriteriaRequest(1L,"Code",null,BigDecimal.TEN,BigDecimal.ONE);
    void owner() { var project=new Project();project.setCreatedBy(3L);when(projectRepository.findById(1L)).thenReturn(Optional.of(project)); }
    void used() {
        owner();when(criteriaRepository.findById(5L)).thenReturn(Optional.of(EvaluationCriteria.builder().id(5L).projectId(1L).title("Code").weight(BigDecimal.ONE).maxScore(BigDecimal.TEN).build()));
        when(peerEvaluationDetailRepository.existsByCriteriaId(5L)).thenReturn(true);
    }
    @Test void refusesNewCriterionAfterReview() {
        owner();when(evaluations.existsByProjectId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request,3L)).hasMessageContaining("started");
    }
    @Test void refusesScaleChangeAfterReview() {
        used();request.setMaxScore(new BigDecimal("100"));
        assertThatThrownBy(() -> service.update(5L,request,3L)).isInstanceOf(RuntimeException.class);
        verify(criteriaRepository,never()).save(any());
    }
    @Test void refusesDeleteUsedCriterion() {
        used();assertThatThrownBy(() -> service.delete(5L,3L)).isInstanceOf(RuntimeException.class);
        verify(criteriaRepository,never()).delete(any(EvaluationCriteria.class));
    }
    @Test void allowsDescriptionCorrectionAfterReview() {
        used();request.setDescription("Corrected");when(criteriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.update(5L,request,3L).getDescription()).isEqualTo("Corrected");
    }
    @Test void refusesOtherLecturer() {
        owner();assertThatThrownBy(() -> service.create(request,99L)).isInstanceOf(RuntimeException.class);
        verify(criteriaRepository,never()).save(any());
    }
}
