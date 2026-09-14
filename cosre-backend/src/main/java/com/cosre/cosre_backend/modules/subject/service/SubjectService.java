package com.cosre.cosre_backend.modules.subject.service;

import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.subject.dto.SubjectRequest;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Logic nghiệp vụ cho việc quản lý môn học.
 * Xử lý tìm kiếm, kiểm tra dữ liệu, lưu trữ và thay đổi trạng thái môn học.
 */
@Service
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // Trả về tất cả môn học, có thể lọc theo từ khóa tìm kiếm.
    public List<Subject> findAll(String query) {
        if (query == null || query.isBlank()) return subjectRepository.findAllByOrderByCode();
        String value = query.trim();
        return subjectRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(value, value);
    }

    // Tải một môn học theo ID hoặc ném exception nếu không tồn tại.
    public Subject getById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    // Tạo một môn học mới và lưu vào cơ sở dữ liệu.
    @Transactional
    public Subject create(SubjectRequest request) {
        ensureCodeAvailable(request.code(), null);
        Subject subject = new Subject();
        apply(subject, request);
        return subjectRepository.save(subject);
    }

    // Cập nhật một môn học hiện có.
    @Transactional
    public Subject update(Long id, SubjectRequest request) {
        Subject subject = getById(id);
        ensureCodeAvailable(request.code(), id);
        apply(subject, request);
        return subjectRepository.save(subject);
    }

    // Thay đổi trạng thái hoạt động của môn học.
    @Transactional
    public Subject setActive(Long id, boolean active) {
        Subject subject = getById(id);
        subject.setActive(active);
        return subjectRepository.save(subject);
    }

    // Ngăn mã môn học bị trùng lặp trong cơ sở dữ liệu.
    private void ensureCodeAvailable(String code, Long currentId) {
        subjectRepository.findByCodeIgnoreCase(code.trim()).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId())) throw new DuplicateResourceException("Subject code already exists");
        });
    }

    // Sao chép giá trị từ request vào entity domain trước khi lưu.
    private void apply(Subject subject, SubjectRequest request) {
        subject.setCode(request.code().trim().toUpperCase());
        subject.setName(request.name().trim());
        subject.setDescription(request.description() == null ? null : request.description().trim());
        subject.setCredits(request.credits());
    }
}
