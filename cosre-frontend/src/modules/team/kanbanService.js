import api from '../../config/axios';

const base = (teamId) => `/teams/${teamId}/kanban`;
export const loadBoard = async (teamId) => (await api.get(base(teamId))).data.data;
export const changeBoard = async (teamId, path, method, payload) => {
  const response = method === 'delete'
    ? await api.delete(`${base(teamId)}${path}`, { params: { revision: payload.revision } })
    : await api[method](`${base(teamId)}${path}`, payload);
  return response.data.data;
};
