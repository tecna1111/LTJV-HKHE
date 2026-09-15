import api from '../../config/axios';

export const getAiHistory = async (teamId = null) =>
  (await api.get('/ai/chat/history', { params: teamId ? { teamId } : {} })).data.data;

export const askAi = async (prompt, teamId = null) =>
  (await api.post('/ai/chat', { prompt, teamId })).data.data;
