import api from '../../config/axios';

export const getSubjects = async () => (await api.get('/subjects')).data.data || [];
export const getSyllabi = async (subjectId) => (await api.get('/syllabi', { params: { subjectId } })).data.data || [];
export const createSyllabus = async (payload) => (await api.post('/syllabi', payload)).data.data;
export const getReviewProjects = async () => (await api.get('/projects/review')).data.data || [];
export const getApprovedProjects = async () => (await api.get('/projects/approved')).data.data || [];
export const reviewProject = async (id, approved, note) => (await api.put(`/projects/${id}/review`, { approved, note })).data.data;
export const getClassrooms = async () => (await api.get('/classrooms')).data.data || [];
export const assignProjectToClassroom = async (projectId, classroomId) => (await api.put(`/projects/${projectId}/classrooms/${classroomId}`)).data;
export const getClassroomProjects = async (classroomId) => (await api.get(`/projects/classroom/${classroomId}`)).data.data || [];
export const getMyProjects = async () => (await api.get('/projects/mine')).data.data || [];
