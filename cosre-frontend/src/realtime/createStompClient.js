import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAuthToken } from '../store/useAuthStore';

const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const backendUrl = apiUrl.replace(/\/api\/v1\/?$/, '');

export function createStompClient({ onConnect, onDisconnect, onError }) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${backendUrl}/ws`),
    connectHeaders: { Authorization: `Bearer ${getAuthToken()}` },
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect,
    onDisconnect,
    onWebSocketClose: onDisconnect,
    onStompError: (frame) => onError?.(frame.headers.message || 'Kết nối realtime gặp lỗi.'),
    onWebSocketError: () => onError?.('Không thể kết nối máy chủ realtime.'),
  });
  return client;
}
