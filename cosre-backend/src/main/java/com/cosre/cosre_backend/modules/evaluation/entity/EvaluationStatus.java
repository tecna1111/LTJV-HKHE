package com.cosre.cosre_backend.modules.evaluation.entity;

/**
 * Trạng thái của một bài đánh giá chéo (peer evaluation).
 */
public enum EvaluationStatus {
    DRAFT,      // Đang soạn, chưa nộp
    SUBMITTED,  // Đã nộp, có thể sửa lại nếu chưa bị khóa
    LOCKED      // Đã khóa (hết hạn / giảng viên chốt điểm), không sửa được nữa
}
