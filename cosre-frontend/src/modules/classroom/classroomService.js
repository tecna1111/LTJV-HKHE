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

// Gọi API để import danh sách lớp học từ tệp CSV/XLSX.
export const importClassrooms = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post('/classrooms/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

// Gọi API để import danh sách thành viên (sinh viên/giảng viên) vào một lớp học từ tệp CSV/XLSX.
// type là 'students' hoặc 'lecturers', đồng bộ với addClassMember/removeClassMember.
export const importClassMembers = async (id, type, file) => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post(`/classrooms/${id}/${type}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

// Gọi API để tải tệp Excel mẫu (.xlsx) dùng khi import danh sách lớp học.
export const downloadClassroomTemplate = async () =>
  (await api.get('/classrooms/import/template', { responseType: 'blob' })).data;

// Gọi API để tải tệp Excel mẫu (.xlsx) dùng khi import danh sách thành viên vào lớp.
export const downloadMemberTemplate = async () =>
  (await api.get('/classrooms/members-import/template', { responseType: 'blob' })).data;
