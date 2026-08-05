import api from '../../config/axios';

export async function getUsers() {
  const response = await api.get('/accounts');
  return response.data;
}

export async function createUser(payload) {
  const response = await api.post('/accounts', payload);
  return response.data;
}

export async function updateUser(id, payload) {
  const response = await api.put(`/accounts/${id}`, payload);
  return response.data;
}

export async function deleteUser(id) {
  const response = await api.delete(`/accounts/${id}`);
  return response.data;
}

export async function setUserStatus(id, active) {
  const response = await api.put(`/accounts/${id}/status`, null, { params: { active } });
  return response.data;
}
