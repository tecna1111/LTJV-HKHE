import api from '../../config/axios';

// Gọi API để lấy danh sách môn học, có thể kèm từ khóa tìm kiếm.
export const getSubjects = async (query = '') => (await api.get('/subjects', { params: query ? { query } : {} })).data;
// Gọi API để tạo mới môn học.
export const createSubject = async (payload) => (await api.post('/subjects', payload)).data;
// Gọi API để cập nhật môn học hiện có.
export const updateSubject = async (id, payload) => (await api.put(`/subjects/${id}`, payload)).data;
// Gọi API để đổi trạng thái hoạt động của môn học.
export const setSubjectStatus = async (id, active) => (await api.put(`/subjects/${id}/status`, null, { params: { active } })).data;
