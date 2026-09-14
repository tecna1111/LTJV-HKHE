import { useCallback, useState } from 'react';
import ModuleLayout from '../../../components/workspace/ModuleLayout';
import Notice from '../../../components/workspace/Notice';
import useRemote from '../../../components/workspace/useRemote';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getClassrooms } from '../../classroom/classroomService';
import { getTeams, getTeamWorkspace } from '../../team/teamService';
import { getClassroomResources, getTeamResources, uploadClassroomResource, uploadTeamResource, deleteResource, downloadResource, updateResourceMetadata, getResourceCheckpoints } from '../resourceService';
function Metadata({ resource, milestones, checkpoints, onSaved, onCancel }) {
  const [title, setTitle] = useState(resource.title);
  const [description, setDescription] = useState(resource.description || '');
  const [milestoneId, setMilestone] = useState(String(resource.milestoneId || ''));
  const [checkpointId, setCheckpoint] = useState(String(resource.checkpointId || ''));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function save(e) {
    e.preventDefault();setBusy(true);setError('');
    try { await updateResourceMetadata(resource.id, { title: title.trim(), description, milestoneId: milestoneId ? Number(milestoneId) : null, checkpointId: checkpointId ? Number(checkpointId) : null });onSaved(); }
    catch (err) { setError(getApiError(err, 'Không thể cập nhật thông tin file.')); } finally { setBusy(false); }
  }
  return <form className="dm-panel" onSubmit={save}><h2>Sửa thông tin: {resource.originalFileName}</h2><Notice error={error} /><fieldset disabled={busy}><label>Tiêu đề<input required maxLength={255} value={title} onChange={e => setTitle(e.target.value)} /></label><label>Mô tả<textarea maxLength={1000} value={description} onChange={e => setDescription(e.target.value)} /></label>
    {resource.teamId && <><label>Milestone<select value={milestoneId} disabled={!!checkpointId} onChange={e => setMilestone(e.target.value)}><option value="">Không liên kết</option>{milestones.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}</select></label><label>Checkpoint<select value={checkpointId} onChange={e => { setCheckpoint(e.target.value); if (e.target.value) setMilestone(String(checkpoints.find(c => String(c.id) === e.target.value)?.milestoneId || '')); }}><option value="">Không liên kết</option>{checkpoints.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}</select></label></>}
    <div className="dm-actions"><button className="primary" disabled={!title.trim()}>{busy ? 'Đang lưu…' : 'Lưu thông tin'}</button><button type="button" onClick={onCancel}>Hủy</button></div></fieldset></form>;
}
function Files({ scope, type, canUpload }) {
  const { role, username } = useAuthStore();
  const loader = useCallback(() => (type === 'team' ? getTeamResources(scope.id) : getClassroomResources(scope.id)).then(r => r.data), [type, scope.id]);
  const { data, loading, error, reload } = useRemote(loader);
  const linksLoader = useCallback(async () => {
    if (type !== 'team') return { milestones: [], checkpoints: [], warning: '' };
    const [workspace, checkpoints] = await Promise.allSettled([getTeamWorkspace(scope.id), getResourceCheckpoints(scope.id)]);
    return { milestones: workspace.status === 'fulfilled' ? workspace.value.project?.milestones || [] : [], checkpoints: checkpoints.status === 'fulfilled' ? checkpoints.value : [], warning: workspace.status === 'rejected' || checkpoints.status === 'rejected' ? 'Không tải đủ milestone/checkpoint. Thử tải lại trước khi sửa liên kết.' : '' };
  }, [type, scope.id]);
  const links = useRemote(linksLoader);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  const [editing, setEditing] = useState(null);
  const canManage = r => ['ADMIN','STAFF'].includes(role) || r.uploadedBy?.username === username || (role === 'LECTURER' && (type === 'team' ? scope.lecturer?.username === username : scope.lecturers?.some(l => l.username === username)));
  async function upload(e) {
    e.preventDefault(); const form = e.currentTarget; const fields = new FormData(form); const file = fields.get('file');
    setBusy(true);setNotice({});
    try { await (type === 'team' ? uploadTeamResource : uploadClassroomResource)(scope.id, file, fields.get('title'), fields.get('description'));form.reset();reload();setNotice({ message: 'Đã tải file lên. Chọn Sửa thông tin để gắn milestone/checkpoint.' }); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể tải file lên.') }); } finally { setBusy(false); }
  }
  async function action(file, remove) {
    if (remove && !window.confirm(`Xóa “${file.title}”?`)) return;
    setBusy(true);setNotice({});
    try { if (remove) { await deleteResource(file.id);reload();setNotice({ message: 'Đã xóa file.' }); } else await downloadResource(file.id, file.originalFileName); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể thực hiện thao tác.') }); } finally { setBusy(false); }
  }
  return <><Notice error={error || notice.error} message={notice.message} />{canUpload && <form className="dm-panel" onSubmit={upload}><h2>Tải file lên</h2><fieldset disabled={busy}><label>File<input required name="file" type="file" /></label><label>Tiêu đề<input name="title" maxLength={255} placeholder="Mặc định lấy tên file" /></label><label>Mô tả<textarea name="description" maxLength={1000} /></label><button className="primary">{busy ? 'Đang xử lý…' : 'Tải lên'}</button></fieldset></form>}
    {(links.error || links.data?.warning) && <div className="dm-notice error" role="alert">{links.error || links.data.warning}<button onClick={links.reload}>Tải lại liên kết</button></div>}
    {editing && <Metadata key={editing.id} resource={editing} milestones={links.data?.milestones || []} checkpoints={links.data?.checkpoints || []} onCancel={() => setEditing(null)} onSaved={() => { setEditing(null);reload();setNotice({ message: 'Đã lưu thông tin.' }); }} />}
    <section className="dm-panel"><div className="dm-actions"><h2>Danh sách file</h2><button disabled={loading || busy} onClick={reload}>Làm mới</button></div>{loading ? <p>Đang tải file…</p> : !error && data?.length === 0 ? <p>Chưa có file nào.</p> : data?.map(r => <article key={r.id} className="dm-card"><h3>{r.title}</h3><p className="dm-muted">{r.originalFileName} · {(r.fileSize / 1024).toFixed(1)} KB · {r.uploadedBy?.fullName}</p><p>{r.description}</p>{r.milestoneId && <p>Milestone: {links.data?.milestones.find(m => m.id === r.milestoneId)?.title || `#${r.milestoneId}`}</p>}{r.checkpointId && <p>Checkpoint: {links.data?.checkpoints.find(c => c.id === r.checkpointId)?.title || `#${r.checkpointId}`}</p>}<div className="dm-actions"><button disabled={busy} onClick={() => action(r, false)}>Tải về</button>{canManage(r) && <><button disabled={busy || links.loading || !!links.error || !!links.data?.warning} onClick={() => setEditing(r)}>Sửa thông tin</button><button className="danger" disabled={busy} onClick={() => action(r, true)}>Xóa</button></>}</div></article>)}</section></>;
}
export default function ResourceLibraryPage() {
  const { role, username } = useAuthStore();
  const [type, setType] = useState('classroom');
  const [id, setId] = useState('');
  const loader = useCallback(() => (type === 'team' ? getTeams() : getClassrooms()).then(r => r.data), [type]);
  const { data, loading, error, reload } = useRemote(loader);
  const scope = data?.find(s => String(s.id) === id);
  const canUpload = scope && (type === 'team' ? ['LECTURER','STUDENT'].includes(role) : ['ADMIN','STAFF'].includes(role) || role === 'LECTURER' && scope.lecturers?.some(l => l.username === username));
  return <ModuleLayout title="Thư viện tài nguyên" description="Quản lý tài liệu lớp học và file bài nộp của nhóm."><Notice error={error} />{error && <button onClick={reload}>Thử lại</button>}<div className="dm-grid"><label>Phạm vi<select value={type} onChange={e => { setType(e.target.value);setId(''); }}><option value="classroom">Lớp học</option>{['STUDENT','LECTURER'].includes(role) && <option value="team">Nhóm</option>}</select></label><label>{type === 'team' ? 'Nhóm' : 'Lớp học'}<select disabled={loading} value={id} onChange={e => setId(e.target.value)}><option value="">{loading ? 'Đang tải…' : 'Chọn phạm vi'}</option>{(data || []).map(s => <option key={s.id} value={s.id}>{s.code ? `${s.code} — ` : ''}{s.name}</option>)}</select></label></div>{scope && <Files key={`${type}:${id}`} type={type} scope={scope} canUpload={canUpload} />}</ModuleLayout>;
}
