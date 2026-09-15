import api from '../../config/axios';

export const getMeetings = async (teamId) => (await api.get(`/teams/${teamId}/meetings`)).data.data || [];
export const createMeeting = async (teamId, payload) => (await api.post(`/teams/${teamId}/meetings`, payload)).data.data;
export const startInstantMeeting = async (teamId) => (await api.post(`/teams/${teamId}/meetings/instant`)).data.data;
export const updateMeeting = async (id, payload) => (await api.put(`/meetings/${id}`, payload)).data.data;
export const cancelMeeting = async (id) => api.delete(`/meetings/${id}`);
export const joinMeeting = async (id) => (await api.post(`/meetings/${id}/join`)).data.data;
