import api from '../../config/axios';
export const listIncidents = async () => (await api.get('/incidents')).data.data;
export const createIncident = async payload => (await api.post('/incidents', payload)).data.data;
export const updateIncident = async (id, payload) => (await api.patch(`/incidents/${id}/status`, payload)).data.data;
