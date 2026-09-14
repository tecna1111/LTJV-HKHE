/**
 * teamApi.js
 * -------------------------------------------------------------------------
 * API service cho màn hình "Quản lý nhóm & theo dõi tiến độ" của Giảng viên.
 *
 * GIẢ ĐỊNH VỀ HỢP ĐỒNG API (đối chiếu lại với Công Duy - Backend Lead):
 *   GET  /api/lecturer/classes
 *        -> [{ id, code, name }]                    (lớp được phân công)
 *
 *   GET  /api/lecturer/classes/:classId/teams
 *        -> [{ id, name, projectTitle, memberCount, progressPercent,
 *               currentMilestone: string | null }]
 *
 *   GET  /api/lecturer/teams/:teamId/contributions
 *        -> [{ studentId, studentName, contributionPercent,
 *               completedTasks, totalTasks }]
 *
 * Nếu backend đặt tên field khác, chỉ cần sửa trong file này.
 * -------------------------------------------------------------------------
 */

import apiClient from "../../../api/apiClient";

export async function fetchAssignedClasses() {
  const { data } = await apiClient.get("/lecturer/classes");
  return data;
}

export async function fetchTeamsByClass(classId) {
  const { data } = await apiClient.get(`/lecturer/classes/${classId}/teams`);
  return data;
}

export async function fetchTeamContributions(teamId) {
  const { data } = await apiClient.get(
    `/lecturer/teams/${teamId}/contributions`
  );
  return data;
}
