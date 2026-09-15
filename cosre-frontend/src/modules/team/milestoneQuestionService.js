import api from '../../config/axios';

export const getMilestoneQuestions = async (teamId, milestoneId) =>
  (await api.get(`/teams/${teamId}/milestones/${milestoneId}/questions`)).data.data || [];
export const createMilestoneQuestion = async (milestoneId, questionText) =>
  (await api.post(`/milestones/${milestoneId}/questions`, { questionText })).data.data;
export const updateMilestoneQuestion = async (questionId, questionText) =>
  (await api.put(`/milestone-questions/${questionId}`, { questionText })).data.data;
export const deleteMilestoneQuestion = async (questionId) =>
  api.delete(`/milestone-questions/${questionId}`);
export const getMilestoneAnswers = async (teamId, questionId) =>
  (await api.get(`/teams/${teamId}/milestone-questions/${questionId}/answers`)).data.data || [];
export const submitMilestoneAnswer = async (teamId, questionId, answerText) =>
  (await api.post(`/teams/${teamId}/milestone-questions/${questionId}/answers`, { answerText })).data.data;
export const reviewMilestoneAnswer = async (teamId, answerId, score, feedback) =>
  (await api.put(`/teams/${teamId}/milestone-answers/${answerId}/review`, { score, feedback })).data.data;
export const getAnswerPeerFeedback = async answerId =>
  (await api.get(`/evaluations/answers/${answerId}/peer-feedback`)).data.data || [];
export const submitAnswerPeerFeedback = async (answerId, feedback) =>
  (await api.put(`/evaluations/answers/${answerId}/peer-feedback`, { feedback })).data.data;
