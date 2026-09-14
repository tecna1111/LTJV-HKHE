import api from '../../config/axios';

export const askAi = async (prompt, teamId = null) =>
  (await api.post('/ai/chat', { prompt, teamId })).data.data;
