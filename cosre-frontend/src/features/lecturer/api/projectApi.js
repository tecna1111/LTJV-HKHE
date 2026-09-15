/** API dùng chung base URL /api/v1; mọi response được bọc trong ApiResponse.data. */

import apiClient from "../../../config/axios";

export async function fetchAssignedSubjects() {
  const { data } = await apiClient.get("/classrooms");
  const subjects = (data.data || []).filter((classroom) => classroom.active)
    .map((classroom) => classroom.subject).filter((subject) => subject?.active);
  return [...new Map(subjects.map((subject) => [subject.id, subject])).values()];
}

export async function fetchSyllabus(subjectId) {
  const { data } = await apiClient.get('/syllabi', { params: { subjectId } });
  const active = (data.data || []).find((item) => item.active);
  if (!active) throw new Error('No active syllabus');
  return active;
}

export async function generateMilestonesWithAI({ syllabusId, objectives }) {
  const { data } = await apiClient.post("/ai/milestones/generate", {
    syllabusId,
    objectives,
  });
  const milestones = data.data?.milestones;
  if (!Array.isArray(milestones)) throw new Error('AI response is missing milestones');
  return milestones;
}

export async function generateProjectDraftWithAI({ syllabusId, topic }) {
  const { data } = await apiClient.post('/ai/projects/draft', { syllabusId, topic });
  const draft = data.data;
  if (!draft?.title || !draft?.description || !Array.isArray(draft.objectives)
      || !Array.isArray(draft.milestones)) throw new Error('AI response is missing project information');
  return draft;
}

export async function createProject(payload) {
  const { data } = await apiClient.post("/projects", {
    ...payload,
    subjectId: Number(payload.subjectId),
    syllabusId: Number(payload.syllabusId),
  });
  return data.data;
}

export async function submitProjectForApproval(projectId) {
  const { data } = await apiClient.post(`/projects/${projectId}/submit`);
  return data.data;
}

export async function updateProject(projectId, payload) {
  const { data } = await apiClient.put(`/projects/${projectId}`, {
    ...payload,
    subjectId: Number(payload.subjectId),
    syllabusId: Number(payload.syllabusId),
  });
  return data.data;
}

export async function fetchMyProjects() {
  const { data } = await apiClient.get("/projects/mine");
  return data.data || [];
}
