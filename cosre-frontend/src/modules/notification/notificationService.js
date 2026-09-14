import api from '../../config/axios';

export const getNotifications = async () => (await api.get('/notifications')).data.data;
export const markNotificationRead = async (id) => (await api.put(`/notifications/${id}/read`)).data.data;
export const markAllNotificationsRead = async () => api.put('/notifications/read-all');
