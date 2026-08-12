/**
 * projectApi.js
 * -------------------------------------------------------------------------
 * API service cho màn hình "Tạo Project + Milestone (AI hỗ trợ)" của Giảng viên.
 *
 * GIẢ ĐỊNH VỀ HỢP ĐỒNG API (cần đối chiếu lại với Công Duy - Backend Lead,
 * package com.cosre.cosre_backend):
 *   GET    /api/lecturer/subjects
 *          -> [{ id, code, name }]                (môn học được phân công)
 *
 *   GET    /api/subjects/:subjectId/syllabus
 *          -> { id, subjectId, content, objectives: string[] }
 *
 *   POST   /api/ai/milestones/generate
 *          body: { syllabusId, objectives: string[] }
 *          -> { milestones: [{ title, description, dueOffsetDays }] }
 *
 *   POST   /api/lecturer/projects
 *          body: { title, description, subjectId, objectives: string[],
 *                   milestones: [{ title, description, dueOffsetDays }] }
 *          -> { id, status: "DRAFT" | "PENDING", ... }
 *
 *   POST   /api/lecturer/projects/:projectId/submit
 *          -> { id, status: "PENDING" }
 *
 * Nếu Spring Boot đặt tên field/endpoint khác (ví dụ do @RequestMapping
 * hoặc DTO khác), chỉ cần sửa trong file này — các component không cần
 * biết chi tiết endpoint thật.
 * -------------------------------------------------------------------------
 */

import apiClient from "../../../api/apiClient";

export async function fetchAssignedSubjects() {
  const { data } = await apiClient.get("/lecturer/subjects");
  return data;
}

export async function fetchSyllabus(subjectId) {
  const { data } = await apiClient.get(`/subjects/${subjectId}/syllabus`);
  return data;
}

export async function generateMilestonesWithAI({ syllabusId, objectives }) {
  const { data } = await apiClient.post("/ai/milestones/generate", {
    syllabusId,
    objectives,
  });
  return data.milestones;
}

export async function createProject(payload) {
  const { data } = await apiClient.post("/lecturer/projects", payload);
  return data;
}

export async function submitProjectForApproval(projectId) {
  const { data } = await apiClient.post(
    `/lecturer/projects/${projectId}/submit`
  );
  return data;
}
