import api from '../../config/axios';

export const getTeams = async () => (await api.get('/teams')).data;
export const getAvailableStudents = async (classroomId) => (await api.get('/teams/available-students', { params: { classroomId } })).data;
export const createTeam = async (payload) => (await api.post('/teams', payload)).data;
export const addTeamMember = async (teamId, studentId, leader = false) => (await api.post(`/teams/${teamId}/members`, { studentId, leader })).data;
export const removeTeamMember = async (teamId, studentId) => (await api.delete(`/teams/${teamId}/members/${studentId}`)).data;
export const assignTeamProject = async (teamId, projectId) => (await api.put(`/teams/${teamId}/project`, { projectId })).data;
export const deleteTeam = async (teamId) => (await api.delete(`/teams/${teamId}`)).data;
