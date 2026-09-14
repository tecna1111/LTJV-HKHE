package com.cosre.cosre_backend.modules.syllabus.service;

import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import com.cosre.cosre_backend.modules.syllabus.dto.SyllabusRequest;
import com.cosre.cosre_backend.modules.syllabus.entity.Syllabus;
import com.cosre.cosre_backend.modules.syllabus.repository.SyllabusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @Transactional
public class SyllabusService {
    private final SyllabusRepository repository;
    private final SubjectRepository subjects;
    public SyllabusService(SyllabusRepository repository, SubjectRepository subjects) { this.repository = repository; this.subjects = subjects; }
    @Transactional(readOnly = true) public List<Syllabus> list(Long subjectId) { return repository.findBySubjectIdOrderByUpdatedAtDesc(subjectId); }
    @Transactional(readOnly = true) public Syllabus get(Long id) { return repository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Syllabus not found")); }
    public Syllabus create(SyllabusRequest request) {
        if (repository.existsBySubjectIdAndVersionIgnoreCase(request.subjectId(), request.version().trim())) throw new DuplicateResourceException("Syllabus version already exists");
        Syllabus value = new Syllabus(); apply(value, request); return repository.save(value);
    }
    public Syllabus update(Long id, SyllabusRequest request) {
        Syllabus value = get(id);
        if ((!value.getSubject().getId().equals(request.subjectId()) || !value.getVersion().equalsIgnoreCase(request.version().trim()))
                && repository.existsBySubjectIdAndVersionIgnoreCase(request.subjectId(), request.version().trim())) throw new DuplicateResourceException("Syllabus version already exists");
        apply(value, request); return value;
    }
    public Syllabus setActive(Long id, boolean active) { Syllabus value = get(id); value.setActive(active); return value; }
    private void apply(Syllabus value, SyllabusRequest request) {
        value.setSubject(subjects.findById(request.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject not found")));
        value.setTitle(request.title().trim()); value.setContent(clean(request.content())); value.setObjectives(clean(request.objectives())); value.setVersion(request.version().trim());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
