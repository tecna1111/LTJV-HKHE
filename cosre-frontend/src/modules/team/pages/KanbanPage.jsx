import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Plus, RefreshCw } from 'lucide-react';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { loadBoard, changeBoard } from '../kanbanService';
import './KanbanPage.css';

const columns = [['TODO', 'To-do'], ['IN_PROGRESS', 'In Progress'], ['DONE', 'Done']];
const emptyTask = { title: '', description: '', weight: 1, dueOn: '', sprintId: '', milestoneId: '', checkpointId: '', assigneeIds: [] };

export default function KanbanPage() {
  const { id } = useParams();
  const role = useAuthStore(s => s.role);
  const displayName = useAuthStore(s => s.fullName || s.username);
  const [board, setBoard] = useState(null);
  const focusedTask = useRef('');
  useEffect(() => {
    const hash = window.location.hash;
    if (board && /^#task-\d+$/.test(hash) && focusedTask.current !== `${id}${hash}`) {
      document.getElementById(hash.slice(1))?.scrollIntoView({ block: 'center' });
      focusedTask.current = `${id}${hash}`;
    }
  }, [board, id]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [assignee, setAssignee] = useState('');
  const [sprintFilter, setSprintFilter] = useState('');
  const [form, setForm] = useState(null);
  const [sprintForm, setSprintForm] = useState(null);
  const [subtaskForm, setSubtaskForm] = useState(null);
  const refresh = useCallback(async () => {
    try { setBoard(await loadBoard(id)); } catch (e) { setError(getApiError(e)); }
  }, [id]);
  useEffect(() => {
    let active = true;
    loadBoard(id).then(value => { if (active) setBoard(value); })
      .catch(e => { if (active) setError(getApiError(e)); });
    return () => { active = false; };
  }, [id]);
  const mutate = async (path, method, payload = {}) => {
    if (busy) return false;
    setBusy(true); setError('');
    try { setBoard(await changeBoard(id, path, method, { ...payload, revision: board.revision })); return true; }
    catch (e) {
      setError(getApiError(e));
      // A rejected stale write must never silently overwrite another user's work.
      if (e.response?.status === 409) await refresh();
      return false;
    } finally { setBusy(false); }
  };
  const move = (taskId, status, position) => mutate(`/tasks/${taskId}/move`, 'put', { status, position });
  const drop = (event, status, beforeId = null) => {
    event.preventDefault(); event.stopPropagation();
    const taskId = Number(event.dataTransfer.getData('text/plain'));
    if (!board.tasks.some(t => t.id === taskId) || busy || taskId === beforeId) return;
    const target = board.tasks.filter(t => t.status === status && t.id !== taskId);
    const position = beforeId === null ? target.length : target.findIndex(t => t.id === beforeId);
    if (position >= 0) move(taskId, status, position);
  };
  const saveTask = async event => {
    event.preventDefault();
    const payload = { ...form, weight: Number(form.weight), dueOn: form.dueOn || null,
      sprintId: form.sprintId ? Number(form.sprintId) : null,
      milestoneId: form.milestoneId ? Number(form.milestoneId) : null,
      checkpointId: form.checkpointId ? Number(form.checkpointId) : null };
    if (await mutate(form.id ? `/tasks/${form.id}` : '/tasks', form.id ? 'put' : 'post', payload)) setForm(null);
  };
  const filtered = task => (!assignee || task.assigneeIds.includes(Number(assignee)))
    && (!sprintFilter || (sprintFilter === 'backlog' ? !task.sprintId : task.sprintId === Number(sprintFilter)));

  return <DashboardShell role={role} displayName={displayName} pageTitle="Kanban nhóm">
    <div className="kanban-page">
      <header className="kanban-heading"><div><Link to={`/teams/${id}/workspace`}><ArrowLeft size={16} /> Workspace nhóm</Link><h1>{board?.teamName || 'Kanban'}</h1><p>Quản lý công việc, theo dõi tiến độ và phối hợp cùng nhóm.</p></div>
        <button disabled={busy || !board} onClick={() => setForm({ ...emptyTask })}><Plus size={16} /> Công việc</button></header>
      {error && <div role="alert" className="kanban-error">{error}</div>}
      {!board ? <p>Đang tải board…</p> : <>
        <div className="kanban-filters">
          <label>Người phụ trách<select value={assignee} onChange={e => setAssignee(e.target.value)}><option value="">Tất cả</option>{board.members.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}</select></label>
          <label>Sprint<select value={sprintFilter} onChange={e => setSprintFilter(e.target.value)}><option value="">Tất cả</option><option value="backlog">Chưa vào sprint</option>{board.sprints.map(s => <option key={s.id} value={s.id}>{s.title}</option>)}</select></label>
          <button disabled={busy} onClick={refresh}><RefreshCw size={16} /> Tải lại</button>
          {board.canManage && <button disabled={busy} onClick={() => setSprintForm({ title: '', startsOn: '', endsOn: '' })}><Plus size={16} /> Sprint</button>}
        </div>
        <section className="kanban-columns" aria-label="Bảng công việc" aria-busy={busy}>
          {columns.map(([status, label]) => <section key={status} className={`kanban-column kanban-column--${status.toLowerCase()}`} onDragOver={e => e.preventDefault()} onDrop={e => drop(e, status)}>
            <h2>{label} <small>{board.tasks.filter(t => t.status === status && filtered(t)).length}</small></h2>
            {board.tasks.filter(t => t.status === status && filtered(t)).map(task => <article key={task.id} id={`task-${task.id}`} className="kanban-card" draggable={!busy}
              onDragStart={e => e.dataTransfer.setData('text/plain', String(task.id))} onDragOver={e => e.preventDefault()} onDrop={e => drop(e, status, task.id)}>
              <h3>{task.title}</h3><p>{task.description}</p>
              <div className="kanban-meta">Trọng số {task.weight} · {task.dueOn ? `Hạn ${task.dueOn}` : 'Chưa đặt hạn'}</div>
              <p>{task.assigneeIds.map(uid => board.members.find(m => m.id === uid)?.name || `#${uid}`).join(', ')}</p>
              {task.sprintId && <small>Sprint: {board.sprints.find(s => s.id === task.sprintId)?.title}</small>}
              {task.milestoneId && <small>Milestone: {board.milestones.find(m => m.id === task.milestoneId)?.title}</small>}
              {task.checkpointId && <small>Checkpoint: {board.checkpoints.find(c => c.id === task.checkpointId)?.title}</small>}
              <ul className="kanban-subtasks">{task.subtasks.map(sub => <li key={sub.id}>
                <label><input type="checkbox" checked={sub.done} disabled={busy} onChange={e => mutate(`/tasks/${task.id}/subtasks/${sub.id}`, 'put', { title: sub.title, done: e.target.checked })}/>{sub.title}</label>
                <button disabled={busy} aria-label={`Sửa ${sub.title}`} onClick={() => setSubtaskForm({ ...sub, taskId: task.id })}>Sửa</button>
                <button disabled={busy} aria-label={`Xóa ${sub.title}`} onClick={() => mutate(`/tasks/${task.id}/subtasks/${sub.id}`, 'delete')}>×</button>
              </li>)}</ul>
              <button disabled={busy} onClick={() => setSubtaskForm({ taskId: task.id, title: '', done: false })}>+ Subtask</button>
              <label>Trạng thái<select disabled={busy} value={task.status} onChange={e => move(task.id, e.target.value, board.tasks.filter(t => t.status === e.target.value && t.id !== task.id).length)}>{columns.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></label>
              <div className="kanban-actions"><button disabled={busy} onClick={() => setForm({ ...task, dueOn: task.dueOn || '', sprintId: task.sprintId || '', milestoneId: task.milestoneId || '', checkpointId: task.checkpointId || '' })}>Sửa</button>
                <button disabled={busy} onClick={() => { if (window.confirm(`Xóa task “${task.title}” và các subtask?`)) mutate(`/tasks/${task.id}`, 'delete'); }}>Xóa</button></div>
            </article>)}
            <p className="kanban-drop-hint">Kéo công việc vào đây để cập nhật tiến độ</p>
          </section>)}
        </section>
        <section className="kanban-panel"><h2>Sprint</h2>{board.sprints.length === 0 && <p>Chưa có sprint.</p>}{board.sprints.map(s => <div className="kanban-sprint" key={s.id}><span><b>{s.title}</b> · {s.startsOn} → {s.endsOn}</span>{board.canManage && <div><button disabled={busy} onClick={() => setSprintForm(s)}>Sửa</button><button disabled={busy} onClick={() => { if (window.confirm('Xóa sprint? Các task sẽ được chuyển về chưa vào sprint.')) mutate(`/sprints/${s.id}`, 'delete'); }}>Xóa</button></div>}</div>)}</section>
        <section className="kanban-panel"><h2>Tiến độ và đóng góp theo task</h2><p role="note">Bản xem trước — chờ nhóm duyệt công thức. Không phải điểm học tập.</p><p>Tiến độ theo trọng số: <b>{board.metrics.progressPercent}%</b> · Done {board.metrics.doneWeight}/{board.metrics.totalWeight}</p>
          <table><thead><tr><th>Thành viên</th><th>Trọng số Done được chia</th><th>Đóng góp</th></tr></thead><tbody>{board.metrics.members.map(m => <tr key={m.userId}><td>{m.name}</td><td>{m.doneWeight}</td><td>{m.percent}%</td></tr>)}</tbody></table>
          <p>Task nhiều người được chia đều trọng số. Task mở lại không được tính Done; subtask không tính thêm trọng số.</p></section>
        <details className="kanban-panel"><summary>Nhật ký thay đổi (100 sự kiện gần nhất)</summary>{board.events.map(e => <p key={e.id}><b>{e.action}</b> · {e.occurredAt} · Người thao tác #{e.actorId}<br/>{e.detail}</p>)}</details>
      </>}
      {form && <div className="kanban-modal"><form onSubmit={saveTask} aria-label="Thông tin task"><h2>{form.id ? 'Sửa công việc' : 'Tạo công việc'}</h2>
        <label>Tiêu đề<input required maxLength={200} value={form.title} onChange={e => setForm({ ...form, title: e.target.value })}/></label>
        <label>Mô tả<textarea maxLength={4000} value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })}/></label>
        <label>Trọng số<input type="number" required min="0.01" max="99999999.99" step="0.01" value={form.weight} onChange={e => setForm({ ...form, weight: e.target.value })}/></label>
        <label>Hạn<input type="date" value={form.dueOn} onChange={e => setForm({ ...form, dueOn: e.target.value })}/></label>
        <label>Sprint<select value={form.sprintId} onChange={e => setForm({ ...form, sprintId: e.target.value })}><option value="">Chưa vào sprint</option>{board.sprints.map(s => <option key={s.id} value={s.id}>{s.title}</option>)}</select></label>
        <label>Milestone<select disabled={!!form.checkpointId} value={form.milestoneId} onChange={e => setForm({ ...form, milestoneId: e.target.value })}><option value="">Không liên kết</option>{board.milestones.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}</select></label>
        <label>Checkpoint<select value={form.checkpointId} onChange={e => { const cp = board.checkpoints.find(c => c.id === Number(e.target.value)); setForm({ ...form, checkpointId: e.target.value, milestoneId: cp?.milestoneId || '' }); }}><option value="">Không liên kết</option>{board.checkpoints.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}</select></label>
        <fieldset><legend>Người được giao (chọn ít nhất một)</legend>{board.members.map(m => <label key={m.id}><input type="checkbox" checked={form.assigneeIds.includes(m.id)} onChange={e => setForm({ ...form, assigneeIds: e.target.checked ? [...form.assigneeIds, m.id] : form.assigneeIds.filter(uid => uid !== m.id) })}/>{m.name}</label>)}</fieldset>
        {error && <p role="alert" className="kanban-error">{error}</p>}<div className="kanban-actions"><button disabled={busy || !form.assigneeIds.length}>Lưu</button><button type="button" disabled={busy} onClick={() => setForm(null)}>Hủy</button></div>
      </form></div>}
      {sprintForm && <div className="kanban-modal"><form aria-label="Thông tin sprint" onSubmit={async e => { e.preventDefault(); if (await mutate(sprintForm.id ? `/sprints/${sprintForm.id}` : '/sprints', sprintForm.id ? 'put' : 'post', sprintForm)) setSprintForm(null); }}><h2>Sprint</h2>
        <label>Tên sprint<input required maxLength={200} value={sprintForm.title} onChange={e => setSprintForm({ ...sprintForm, title: e.target.value })}/></label>
        <label>Bắt đầu<input type="date" required value={sprintForm.startsOn} onChange={e => setSprintForm({ ...sprintForm, startsOn: e.target.value })}/></label>
        <label>Kết thúc<input type="date" required min={sprintForm.startsOn} value={sprintForm.endsOn} onChange={e => setSprintForm({ ...sprintForm, endsOn: e.target.value })}/></label>
        {error && <p role="alert">{error}</p>}<div className="kanban-actions"><button disabled={busy}>Lưu</button><button type="button" disabled={busy} onClick={() => setSprintForm(null)}>Hủy</button></div>
      </form></div>}
      {subtaskForm && <div className="kanban-modal"><form aria-label="Thông tin subtask" onSubmit={async e => { e.preventDefault(); if (await mutate(`/tasks/${subtaskForm.taskId}/subtasks${subtaskForm.id ? `/${subtaskForm.id}` : ''}`, subtaskForm.id ? 'put' : 'post', subtaskForm)) setSubtaskForm(null); }}><h2>Subtask</h2>
        <label>Tiêu đề<input required maxLength={200} value={subtaskForm.title} onChange={e => setSubtaskForm({ ...subtaskForm, title: e.target.value })}/></label>
        <label><input type="checkbox" checked={subtaskForm.done} onChange={e => setSubtaskForm({ ...subtaskForm, done: e.target.checked })}/>Hoàn thành</label>
        {error && <p role="alert">{error}</p>}<div className="kanban-actions"><button disabled={busy}>Lưu</button><button type="button" disabled={busy} onClick={() => setSubtaskForm(null)}>Hủy</button></div>
      </form></div>}
    </div>
  </DashboardShell>;
}
