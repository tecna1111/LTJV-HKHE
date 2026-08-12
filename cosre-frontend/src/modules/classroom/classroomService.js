import api from '../../config/axios';

// Gọi API để lấy danh sách lớp học.
export const getClassrooms = async () => (await api.get('/classrooms')).data;
// Gọi API để lấy thông tin chi tiết một lớp học theo id.
export const getClassroom = async (id) => (await api.get(`/classrooms/${id}`)).data;
// Gọi API để tạo mới lớp học.
export const createClassroom = async (payload) => (await api.post('/classrooms', payload)).data;
// Gọi API để cập nhật lớp học hiện có.
export const updateClassroom = async (id, payload) => (await api.put(`/classrooms/${id}`, payload)).data;
// Gọi API để đổi trạng thái hoạt động của lớp học.
export const setClassroomStatus = async (id, active) => (await api.put(`/classrooms/${id}/status`, null, { params: { active } })).data;
// Gọi API để lấy danh sách người dùng đủ điều kiện làm giảng viên/sinh viên.
export const getEligibleMembers = async (role) => (await api.get('/classrooms/eligible-members', { params: { role } })).data;
// Gọi API để thêm thành viên vào lớp học.
export const addClassMember = async (id, type, userId) => (await api.post(`/classrooms/${id}/${type}/${userId}`)).data;
// Gọi API để xóa thành viên khỏi lớp học.
export const removeClassMember = async (id, type, userId) => (await api.delete(`/classrooms/${id}/${type}/${userId}`)).data;
