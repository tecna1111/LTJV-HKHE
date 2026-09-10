import api from '../../config/axios';

// ----- Tiêu chí đánh giá (rubric) -----
export const getCriteria = async (projectId) =>
  (await api.get(`/evaluations/criteria/project/${projectId}`)).data;

// ----- Bài đánh giá chéo -----
export const submitPeerEvaluation = async (payload) =>
  (await api.post('/evaluations/peer', payload)).data;

export const getGivenEvaluations = async (projectId) =>
  (await api.get('/evaluations/peer/given', { params: { projectId } })).data;

export const getReceivedEvaluations = async (projectId) =>
  (await api.get('/evaluations/peer/received', { params: { projectId } })).data;

// ----- Dành cho giảng viên -----
export const getStudentSummary = async (studentId, teamId, projectId) =>
  (await api.get(`/evaluations/peer/summary/student/${studentId}`, { params: { teamId, projectId } })).data;

// memberIds truyền dạng chuỗi "1,2,3" — Spring tự convert sang List<Long>,
// tránh phụ thuộc vào cách axios serialize mảng trong query string.
export const getTeamSummary = async (teamId, projectId, memberIds) =>
  (await api.get(`/evaluations/peer/summary/team/${teamId}`, {
    params: { projectId, memberIds: memberIds.join(',') },
  })).data;

export const lockTeamEvaluations = async (teamId, projectId) =>
  (await api.post('/evaluations/peer/lock', null, { params: { teamId, projectId } })).data;
