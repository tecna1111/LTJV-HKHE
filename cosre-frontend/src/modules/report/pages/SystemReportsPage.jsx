import { useState } from 'react';
import ModuleLayout from '../../../components/workspace/ModuleLayout';
import Notice from '../../../components/workspace/Notice';
import useRemote from '../../../components/workspace/useRemote';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { listIncidents, createIncident, updateIncident } from '../incidentService';
const labels = { OPEN: 'Mới tiếp nhận', IN_PROGRESS: 'Đang xử lý', RESOLVED: 'Đã giải quyết', CLOSED: 'Đã đóng' };
const transitions = { OPEN: ['OPEN','IN_PROGRESS'], IN_PROGRESS: ['IN_PROGRESS','RESOLVED'], RESOLVED: ['RESOLVED','IN_PROGRESS','CLOSED'], CLOSED: ['CLOSED'] };
function IncidentEditor({ report, onSaved }) {
  const [status, setStatus] = useState(report.status);
  const [resolution, setResolution] = useState(report.resolution || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function save(e) {
    e.preventDefault();setBusy(true);setError('');
    try { await updateIncident(report.id, { status, resolution: resolution.trim() || null }); onSaved(); }
    catch (err) { setError(getApiError(err, 'Không thể cập nhật. Báo cáo có thể đã được người khác xử lý.')); } finally { setBusy(false); }
  }
  return <form onSubmit={save}><Notice error={error} /><fieldset disabled={busy}><label>Trạng thái<select value={status} onChange={e => setStatus(e.target.value)}>{transitions[report.status].map(s => <option key={s} value={s}>{labels[s]}</option>)}</select></label><label>Kết quả xử lý<textarea maxLength={2000} required={['RESOLVED','CLOSED'].includes(status)} value={resolution} onChange={e => setResolution(e.target.value)} /></label><button>{busy ? 'Đang lưu…' : 'Cập nhật trạng thái'}</button></fieldset></form>;
}
export default function SystemReportsPage() {
  const admin = useAuthStore(s => s.role === 'ADMIN');
  const { data, loading, error, reload } = useRemote(listIncidents);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [filter, setFilter] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  async function submit(e) {
    e.preventDefault();setBusy(true);setNotice({});
    try { const report = await createIncident({ title: title.trim(), description: description.trim() }); setTitle('');setDescription('');setNotice({ message: `Đã tiếp nhận báo cáo #${report.id}.` });reload(); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể gửi báo cáo.') }); } finally { setBusy(false); }
  }
  const reports = (data || []).filter(r => !filter || r.status === filter);
  return <ModuleLayout title={admin ? 'Quản lý báo cáo sự cố' : 'Báo cáo sự cố'} description={admin ? 'Tiếp nhận, theo dõi và cập nhật kết quả xử lý.' : 'Gửi sự cố và theo dõi tiến độ xử lý báo cáo của bạn.'}>
    <Notice error={error || notice.error} message={notice.message} /><div className="dm-grid"><form className="dm-panel" onSubmit={submit}><h2>Gửi báo cáo mới</h2><fieldset disabled={busy}><label>Tiêu đề<input required maxLength={200} value={title} onChange={e => setTitle(e.target.value)} /></label><label>Mô tả sự cố<textarea required maxLength={5000} placeholder="Bạn đang thực hiện thao tác gì? Kết quả mong đợi và lỗi gặp phải…" value={description} onChange={e => setDescription(e.target.value)} /></label><button className="primary" disabled={!title.trim() || !description.trim()}>{busy ? 'Đang gửi…' : 'Gửi báo cáo'}</button></fieldset></form>
    <section className="dm-panel"><h2>{admin ? 'Tất cả báo cáo' : 'Báo cáo của tôi'}</h2><div className="dm-actions"><label>Lọc trạng thái<select value={filter} onChange={e => setFilter(e.target.value)}><option value="">Tất cả</option>{Object.entries(labels).map(([s,l]) => <option key={s} value={s}>{l}</option>)}</select></label><button disabled={loading} onClick={reload}>Làm mới</button></div>
    {loading ? <p>Đang tải báo cáo…</p> : !error && reports.length === 0 ? <p>Chưa có báo cáo phù hợp.</p> : reports.map(r => <article className="dm-card" key={r.id}><h3>#{r.id} · {r.title}</h3><span className="dm-badge">{labels[r.status]}</span><p>{r.description}</p><small>{new Date(r.createdAt).toLocaleString('vi-VN')}</small>{admin ? <IncidentEditor key={`${r.id}:${r.version}`} report={r} onSaved={() => { setNotice({ message: 'Đã cập nhật báo cáo.' });reload(); }} /> : <p>Kết quả xử lý: {r.resolution || 'Đang chờ cập nhật.'}</p>}</article>)}</section></div></ModuleLayout>;
}
