import { useEffect, useRef, useState } from 'react';
import { Bell, CheckCheck, MessageCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { createStompClient } from '../../../realtime/createStompClient';
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../notificationService';
import './NotificationBell.css';

const relativeTime = (value) => {
  const seconds = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 60) return 'Vừa xong';
  if (seconds < 3600) return `${Math.floor(seconds / 60)} phút trước`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} giờ trước`;
  return `${Math.floor(seconds / 86400)} ngày trước`;
};

export default function NotificationBell() {
  const navigate = useNavigate();
  const rootRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const load = () => getNotifications().then((data) => { setItems(data.notifications || []); setUnread(data.unreadCount || 0); }).catch(() => {});

  useEffect(() => {
    load();
    const client = createStompClient({ onConnect: () => client.subscribe('/user/queue/notifications', (frame) => {
      const value = JSON.parse(frame.body);
      setItems((current) => [value, ...current.filter((item) => item.id !== value.id)].slice(0, 50));
      setUnread((count) => count + 1);
    }) });
    client.activate();
    return () => { client.deactivate(); };
  }, []);

  useEffect(() => {
    const close = (event) => { if (!rootRef.current?.contains(event.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const select = async (item) => {
    if (!item.read) {
      await markNotificationRead(item.id).catch(() => null);
      setItems((current) => current.map((value) => value.id === item.id ? { ...value, read: true } : value));
      setUnread((count) => Math.max(0, count - 1));
    }
    setOpen(false);
    if (item.link) navigate(item.link);
  };
  const readAll = async () => {
    await markAllNotificationsRead().catch(() => null);
    setItems((current) => current.map((item) => ({ ...item, read: true })));
    setUnread(0);
  };

  return <div className="notification-bell" ref={rootRef}>
    <button type="button" onClick={() => { setOpen((value) => !value); if (!open) load(); }} aria-label={`Thông báo${unread ? `, ${unread} chưa đọc` : ''}`}><Bell size={18}/>{unread > 0 && <b>{unread > 99 ? '99+' : unread}</b>}</button>
    {open && <section className="notification-panel"><header><div><strong>Thông báo</strong><small>{unread} chưa đọc</small></div>{unread > 0 && <button onClick={readAll}><CheckCheck size={15}/> Đọc tất cả</button>}</header>
      <div>{items.length === 0 ? <p className="notification-empty"><Bell size={25}/>Chưa có thông báo.</p> : items.map((item) => <button className={item.read ? 'read' : ''} key={item.id} onClick={() => select(item)}><span><MessageCircle size={17}/></span><div><strong>{item.title}</strong><p>{item.message}</p><small>{relativeTime(item.createdAt)}</small></div>{!item.read && <i/>}</button>)}</div>
    </section>}
  </div>;
}
