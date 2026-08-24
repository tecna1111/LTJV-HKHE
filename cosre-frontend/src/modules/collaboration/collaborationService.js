import api from '../../config/axios';

export const getWhiteboard = async (teamId) =>
  (await api.get(`/collaboration/teams/${teamId}/whiteboard`)).data.data;

export const saveWhiteboard = async (teamId, canvasData, version) =>
  (await api.put(`/collaboration/teams/${teamId}/whiteboard`, { canvasData, version })).data.data;
