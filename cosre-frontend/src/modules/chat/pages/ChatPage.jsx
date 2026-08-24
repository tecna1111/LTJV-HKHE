import { useEffect, useMemo, useRef, useState } from 'react';
import { BookOpen, MessageCircle, RefreshCw, Send, Users } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { createStompClient } from '../../../realtime/createStompClient';
import { chatDestination, chatTopic, getChatHistory, getClassroomRooms, getTeamRooms } from '../chatService';
import './ChatPage.css';

const roomKey = (room) => `${room.type}-${room.id}`;
const time = (value) => value ? new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '';

export default function ChatPage() {
  const [searchParams] = useSearchParams();
  const role = useAuthStore((state) => state.role);
  const username = useAuthStore((state) => state.username);
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const clientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const endRef = useRef(null);
  const [rooms, setRooms] = useState([]);
  const [active, setActive] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const requested = useMemo(() => ({ type: searchParams.get('type'), id: Number(searchParams.get('id')) }), [searchParams]);

  useEffect(() => {
    Promise.all([getTeamRooms(), getClassroomRooms()]).then(([teams, classrooms]) => {
      const values = [
        ...teams.map((item) => ({ type: 'TEAM', id: item.id, name: item.name, subtitle: `Nhóm · Lớp #${item.classroomId}` })),
        ...classrooms.map((item) => ({ type: 'CLASSROOM', id: item.id, name: item.name, subtitle: `Lớp · ${item.code}` })),
      ];
      setRooms(values);
      setActive(values.find((item) => item.type === requested.type && item.id === requested.id) || values[0] || null);
    }).catch((exception) => setError(getApiError(exception, 'Không thể tải danh sách phòng chat.')))
      .finally(() => setLoading(false));
  }, [requested]);

  useEffect(() => {
    const client = createStompClient({
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onError: setError,
    });
    clientRef.current = client;
    client.activate();
    return () => { subscriptionRef.current?.unsubscribe(); client.deactivate(); };
  }, []);

  useEffect(() => {
    if (!active) return undefined;
    let cancelled = false;
    getChatHistory(active.type, active.id).then((values) => { if (!cancelled) setMessages(values); })
      .catch((exception) => setError(getApiError(exception, 'Không thể tải lịch sử tin nhắn.')));

    const subscribe = () => {
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = clientRef.current.subscribe(chatTopic(active.type, active.id), (frame) => {
        const value = JSON.parse(frame.body);
        setMessages((current) => current.some((item) => item.id === value.id) ? current : [...current, value]);
      });
    };
    if (clientRef.current?.connected) subscribe();
    else {
      const timer = window.setInterval(() => {
        if (clientRef.current?.connected) { window.clearInterval(timer); subscribe(); }
      }, 200);
      return () => { cancelled = true; window.clearInterval(timer); subscriptionRef.current?.unsubscribe(); };
    }
    return () => { cancelled = true; subscriptionRef.current?.unsubscribe(); };
  }, [active, connected]);

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  const send = (event) => {
    event.preventDefault();
    const content = draft.trim();
    if (!content || !active || !clientRef.current?.connected) return;
    clientRef.current.publish({ destination: chatDestination(active.type, active.id), body: JSON.stringify({ content }) });
    setDraft('');
  };

  return <DashboardShell role={role} displayName={displayName} activePath="/messages" pageTitle="Tin nhắn">
    <div className="chat-page">
      <aside className="chat-rooms"><header><span><MessageCircle size={19}/></span><div><strong>Tin nhắn</strong><small>{rooms.length} phòng trò chuyện</small></div></header>
        <div className="chat-connection"><i className={connected ? 'online' : ''}/>{connected ? 'Đã kết nối realtime' : 'Đang kết nối lại…'}</div>
        <div className="chat-room-list">{loading ? <p>Đang tải phòng chat…</p> : rooms.length === 0 ? <p>Bạn chưa có lớp hoặc nhóm để trò chuyện.</p> : rooms.map((room) => <button className={active && roomKey(active) === roomKey(room) ? 'active' : ''} key={roomKey(room)} onClick={() => setActive(room)}>{room.type === 'TEAM' ? <Users size={18}/> : <BookOpen size={18}/>}<span><strong>{room.name}</strong><small>{room.subtitle}</small></span></button>)}</div>
      </aside>
      <section className="chat-conversation">{!active ? <div className="chat-empty"><MessageCircle size={42}/><h2>Chưa có phòng chat</h2><p>Hãy tham gia một lớp hoặc nhóm trước.</p></div> : <><header><div><strong>{active.name}</strong><small>{active.subtitle}</small></div><button onClick={() => getChatHistory(active.type, active.id).then(setMessages)} title="Tải lại"><RefreshCw size={17}/></button></header>
        {error && <div className="chat-error">{error}</div>}
        <div className="chat-messages">{messages.length === 0 ? <div className="chat-empty"><MessageCircle size={34}/><p>Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện.</p></div> : messages.map((message) => <article className={message.senderUsername === username ? 'mine' : ''} key={message.id || `${message.senderId}-${message.createdAt}`}><div><strong>{message.senderName}</strong><time>{time(message.createdAt)}</time></div><p>{message.content}</p></article>)}<div ref={endRef}/></div>
        <form onSubmit={send}><textarea maxLength={2000} rows={2} value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) send(event); }} placeholder={connected ? 'Nhập tin nhắn…' : 'Đang chờ kết nối realtime…'} disabled={!connected}/><button disabled={!connected || !draft.trim()}><Send size={18}/></button></form></>}
      </section>
    </div>
  </DashboardShell>;
}
