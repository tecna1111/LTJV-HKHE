import { useEffect, useState } from 'react';
import { CalendarPlus, ExternalLink, PhoneCall, RefreshCw, Video, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getTeamRooms } from '../../chat/chatService';
import { cancelMeeting, createMeeting, getMeetings, joinMeeting, startInstantMeeting, updateMeeting } from '../meetingService';
import './MeetingPage.css';

const emptyForm = { title: '', description: '', startsAt: '', endsAt: '' };
const format = (value) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));

export default function MeetingPage() {
  const role = useAuthStore((state) => state.role);
  const username = useAuthStore((state) => state.username);
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const [params] = useSearchParams();
  const [teams, setTeams] = useState([]); const [teamId, setTeamId] = useState(params.get('teamId') || '');
  const [meetings, setMeetings] = useState([]); const [form, setForm] = useState(emptyForm); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); const [room, setRoom] = useState(null);
  const load = async (id = teamId) => { if (!id) { setMeetings([]); setLoading(false); return; } setLoading(true); try { setMeetings(await getMeetings(id)); } catch (e) { setError(getApiError(e, 'Không thể tải lịch họp.')); } finally { setLoading(false); } };
  useEffect(() => { getTeamRooms().then((data) => { setTeams(data); const selected = teamId || String(data[0]?.id || ''); setTeamId(selected); load(selected); }).catch((e) => { setError(getApiError(e, 'Không thể tải nhóm.')); setLoading(false); }); }, []);
  const submit = async (event) => { event.preventDefault(); setError(''); try { await createMeeting(teamId, { ...form, startsAt: `${form.startsAt}:00`, endsAt: `${form.endsAt}:00` }); setForm(emptyForm); await load(); } catch (e) { setError(getApiError(e, 'Không thể tạo lịch họp.')); } };
  const join = async (id) => { try { setRoom(await joinMeeting(id)); } catch (e) { setError(getApiError(e, 'Bạn không có quyền tham gia cuộc họp này.')); } };
  const startNow = async () => { setError(''); try { setRoom(await startInstantMeeting(teamId)); await load(); } catch (e) { setError(getApiError(e, 'Không thể bắt đầu cuộc họp.')); } };
  const edit = async (event) => { event.preventDefault(); try { await updateMeeting(editing.id, { ...form, startsAt: `${form.startsAt}:00`, endsAt: `${form.endsAt}:00` }); setEditing(null); setForm(emptyForm); await load(); } catch (e) { setError(getApiError(e, 'Không thể cập nhật lịch họp.')); } };
  const cancel = async (id) => { if (!window.confirm('Hủy cuộc họp này?')) return; try { await cancelMeeting(id); await load(); } catch (e) { setError(getApiError(e, 'Không thể hủy cuộc họp.')); } };
  const selectedTeam = teams.find((team) => String(team.id) === String(teamId));
  const canManage = role === 'LECTURER' || selectedTeam?.leader?.username === username;
  return <DashboardShell role={role} displayName={displayName} activePath="/meetings" pageTitle="Lịch họp"><div className="workspace-page meeting-page"><section className="workspace-heading"><div><span>Meeting</span><h1>Lịch họp nhóm</h1><p>Tạo lịch, nhận thông báo và tham gia phòng họp theo quyền nhóm.</p></div><button onClick={() => load()}><RefreshCw size={16}/> Tải lại</button></section>
    {error && <p className="meeting-error">{error}</p>}<label className="meeting-team">Nhóm <select value={teamId} onChange={(e) => { setTeamId(e.target.value); load(e.target.value); }}><option value="">Chọn nhóm</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
    {canManage && <><button className="meeting-start-now" disabled={!teamId} onClick={startNow}><PhoneCall size={16}/> Họp ngay</button><form className="meeting-form" onSubmit={editing ? edit : submit}><h2><CalendarPlus size={20}/>{editing ? ' Sửa lịch họp' : ' Tạo lịch họp'}</h2><input required maxLength="150" placeholder="Tên cuộc họp" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}/><textarea maxLength="1000" placeholder="Mô tả (không bắt buộc)" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}/><label>Bắt đầu<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })}/></label><label>Kết thúc<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })}/></label><button disabled={!teamId}><CalendarPlus size={16}/>{editing ? ' Lưu thay đổi' : ' Tạo lịch'}</button>{editing && <button type="button" onClick={() => { setEditing(null); setForm(emptyForm); }}>Hủy sửa</button>}</form></>}
    <section className="meeting-list"><h2><Video size={20}/> Cuộc họp của nhóm</h2>{loading ? <p>Đang tải…</p> : meetings.length === 0 ? <p>Chưa có lịch họp cho nhóm này.</p> : meetings.map((meeting) => <article key={meeting.id} className={meeting.status === 'CANCELLED' ? 'cancelled' : ''}><div><strong>{meeting.title}</strong><p>{format(meeting.startsAt)} - {format(meeting.endsAt)}</p>{meeting.description && <small>{meeting.description}</small>}</div><span>{meeting.status === 'CANCELLED' ? 'Đã hủy' : 'Đã lên lịch'}</span>{meeting.status !== 'CANCELLED' && <button onClick={() => join(meeting.id)}><ExternalLink size={16}/> Tham gia</button>}{canManage && meeting.status !== 'CANCELLED' && <button onClick={() => { setEditing(meeting); setForm({ title: meeting.title, description: meeting.description || '', startsAt: meeting.startsAt.slice(0, 16), endsAt: meeting.endsAt.slice(0, 16) }); }}>Sửa</button>}{canManage && meeting.status !== 'CANCELLED' && <button className="danger" onClick={() => cancel(meeting.id)} title="Hủy cuộc họp"><X size={16}/></button>}</article>)}</section>{room && <section className="meeting-room"><header><strong>Đang họp</strong><button onClick={() => setRoom(null)}>Rời phòng</button></header><iframe title="Phòng họp" src={room.joinUrl} allow="camera; microphone; display-capture; fullscreen; autoplay" allowFullScreen/><p>Dùng các nút trong phòng họp để bật/tắt microphone, camera và chia sẻ màn hình.</p></section>}</div></DashboardShell>;
}
