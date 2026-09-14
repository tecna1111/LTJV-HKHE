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

export const createCriteria = async payload => (await api.post('/evaluations/criteria', payload)).data.data;
export const updateCriteria = async (id, payload) => (await api.put(`/evaluations/criteria/${id}`, payload)).data.data;
export const deleteCriteria = async id => (await api.delete(`/evaluations/criteria/${id}`)).data;
export const getRound = async (teamId, projectId) => (await api.get('/evaluations/peer/round', { params: { teamId, projectId } })).data.data;
export const openFinal = async (teamId, projectId) => (await api.post('/evaluations/peer/open-final', null, { params: { teamId, projectId } })).data;
export const getFinalGrades = async (teamId, projectId) => (await api.get('/evaluations/final', { params: { teamId, projectId } })).data.data;
export const saveFinalGrade = async payload => (await api.put('/evaluations/final', payload)).data.data;
export const getAnswerFeedback = async id => (await api.get(`/evaluations/answers/${id}/peer-feedback`)).data.data;
export const saveAnswerFeedback = async (id, feedback) => (await api.put(`/evaluations/answers/${id}/peer-feedback`, { feedback })).data.data;
