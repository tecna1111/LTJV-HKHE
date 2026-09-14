import api from '../../config/axios';

export const getTeamRooms = async () => (await api.get('/teams')).data.data || [];
export const getClassroomRooms = async () => (await api.get('/classrooms')).data.data || [];
export const getChatHistory = async (type, id) => {
  const path = type === 'TEAM' ? `/chat/teams/${id}/messages` : `/chat/classrooms/${id}/messages`;
  return (await api.get(path)).data.data || [];
};

export const chatDestination = (type, id) => type === 'TEAM'
  ? `/app/chat/teams/${id}/send`
  : `/app/chat/classrooms/${id}/send`;

export const chatTopic = (type, id) => type === 'TEAM'
  ? `/topic/chat/teams/${id}`
  : `/topic/chat/classrooms/${id}`;
