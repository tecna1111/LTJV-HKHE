import api from '../../config/axios';

// Danh sách tài liệu môn học của một lớp học.
export const getClassroomResources = async (classroomId) => (await api.get(`/resources/classroom/${classroomId}`)).data;
// Danh sách file bài nộp của một nhóm.
export const getTeamResources = async (teamId) => (await api.get(`/resources/team/${teamId}`)).data;

// Tải tài liệu môn học lên cho một lớp học (multipart/form-data).
export const uploadClassroomResource = async (classroomId, file, title, description) => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (description) formData.append('description', description);
  return (await api.post(`/resources/classroom/${classroomId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })).data;
};

// Tải file bài nộp lên cho một nhóm (multipart/form-data).
export const uploadTeamResource = async (teamId, file, title, description) => {
  const formData = new FormData();
  formData.append('file', file);
  if (title) formData.append('title', title);
  if (description) formData.append('description', description);
  return (await api.post(`/resources/team/${teamId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })).data;
};

// Xóa một resource theo id.
export const deleteResource = async (id) => (await api.delete(`/resources/${id}`)).data;

// Tải file về máy người dùng. Dùng responseType 'blob' + tạo link tạm vì
// endpoint download yêu cầu JWT — mở trực tiếp bằng <a href> sẽ không đính
// kèm được Authorization header.
export const downloadResource = async (id, fileName) => {
  const response = await api.get(`/resources/${id}/download`, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'download';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
