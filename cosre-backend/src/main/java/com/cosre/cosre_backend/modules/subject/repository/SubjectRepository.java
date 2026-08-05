package com.cosre.cosre_backend.modules.subject.repository;

import com.cosre.cosre_backend.modules.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCodeIgnoreCase(String code);
    List<Subject> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(String code, String name);
    List<Subject> findAllByOrderByCode();
}
