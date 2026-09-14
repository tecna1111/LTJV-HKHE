import { useCallback, useState } from 'react';
import ModuleLayout from '../../../components/workspace/ModuleLayout';
import Notice from '../../../components/workspace/Notice';
import useRemote from '../../../components/workspace/useRemote';
import { fetchMyProjects } from '../../../features/lecturer/api/projectApi';
import { getApiError } from '../../../config/axios';
import { getCriteria, createCriteria, updateCriteria, deleteCriteria } from '../evaluationService';
const empty = { title: '', description: '', maxScore: '10', weight: '' };
function Rubric({ projectId }) {
  const loader = useCallback(() => getCriteria(projectId).then(r => r.data), [projectId]);
  const { data, loading, error, reload } = useRemote(loader);
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  const items = data || [];
  const total = items.reduce((sum, item) => sum + Math.round(Number(item.weight) * 100), 0);
  const frozen = items.some(item => item.locked);
  const field = name => ({ value: form[name], onChange: e => setForm({ ...form, [name]: e.target.value }) });
  async function save(e) {
    e.preventDefault(); setNotice({});
    const next = total - (editing ? Math.round(editing.weight * 100) : 0) + Math.round(Number(form.weight) * 100);
    if (next > 100) { setNotice({ error: 'Tổng trọng số không được vượt 100%.' }); return; }
    setBusy(true);
    try {
      const payload = { ...form, title: form.title.trim(), projectId, maxScore: Number(form.maxScore), weight: Number(form.weight) };
      if (editing) await updateCriteria(editing.id, payload); else await createCriteria(payload);
      setEditing(null); setForm(empty); setNotice({ message: 'Đã lưu tiêu chí.' }); reload();
    } catch (err) { setNotice({ error: getApiError(err, 'Không thể lưu tiêu chí.') }); } finally { setBusy(false); }
  }
  async function remove(item) {
    if (!window.confirm(`Xóa tiêu chí “${item.title}”?`)) return;
    setBusy(true); setNotice({});
    try { await deleteCriteria(item.id); if (editing?.id === item.id) { setEditing(null); setForm(empty); } reload(); setNotice({ message: 'Đã xóa tiêu chí.' }); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể xóa tiêu chí.') }); } finally { setBusy(false); }
  }
  return <><Notice error={error || notice.error} message={notice.message} />{error && <button onClick={reload}>Thử lại</button>}
    {loading ? <p role="status">Đang tải tiêu chí…</p> : !error && <div className="dm-grid"><section className="dm-panel"><h2>Bộ tiêu chí <span className="dm-badge">{total}% / 100%</span></h2>
      <p className="dm-muted">Tổng trọng số phải đủ 100% trước khi sinh viên chấm. Trọng số 0,25 tương ứng 25%.</p>
      {items.length === 0 && <p>Chưa có tiêu chí. Thêm tiêu chí đầu tiên ở biểu mẫu bên cạnh.</p>}
      {items.map(item => <article className="dm-card" key={item.id}><h3>{item.title}</h3><p>{item.description}</p><p>Thang điểm: {item.maxScore} · Trọng số: {Math.round(item.weight * 100)}% {item.locked && '· Đã sử dụng'}</p>
        <div className="dm-actions"><button disabled={busy} onClick={() => { setEditing(item); setForm({ title: item.title, description: item.description || '', maxScore: String(item.maxScore), weight: String(item.weight) }); }}>Sửa</button><button className="danger" disabled={busy || item.locked} onClick={() => remove(item)}>Xóa</button></div></article>)}
    </section><form className="dm-panel" onSubmit={save}><h2>{editing ? 'Sửa tiêu chí' : 'Thêm tiêu chí'}</h2>
      {frozen && <p className="dm-muted">Đã có bài chấm: không thêm tiêu chí. Tiêu chí đã dùng chỉ được sửa tiêu đề/mô tả.</p>}
      <fieldset disabled={busy || (!editing && frozen)}><label>Tiêu đề<input required maxLength={150} {...field('title')} /></label><label>Mô tả<textarea maxLength={500} {...field('description')} /></label>
      <label>Điểm tối đa<input required type="number" min="0.01" max="100" step="0.01" disabled={editing?.locked} {...field('maxScore')} /></label><label>Trọng số (0–1)<input required type="number" min="0.01" max="1" step="0.01" disabled={editing?.locked} {...field('weight')} /></label>
      <button className="primary" disabled={!form.title.trim()}>{busy ? 'Đang lưu…' : 'Lưu tiêu chí'}</button></fieldset>
      {editing && <button type="button" disabled={busy} onClick={() => { setEditing(null); setForm(empty); }}>Hủy sửa</button>}</form></div>}</>;
}
export default function CriteriaManagementPage() {
  const { data, loading, error, reload } = useRemote(fetchMyProjects);
  const [selected, setSelected] = useState('');
  return <ModuleLayout title="Quản lý tiêu chí" description="Thiết lập bộ tiêu chí cho các dự án do bạn tạo."><Notice error={error} />
    {error && <button onClick={reload}>Thử lại</button>}{loading ? <p>Đang tải dự án…</p> : <label>Dự án<select value={selected} onChange={e => setSelected(e.target.value)}><option value="">Chọn dự án</option>{(data || []).map(p => <option key={p.id} value={p.id}>{p.title}</option>)}</select></label>}
    {!loading && data?.length === 0 && <p>Bạn chưa có dự án nào.</p>}{selected && <Rubric key={selected} projectId={Number(selected)} />}</ModuleLayout>;
}
