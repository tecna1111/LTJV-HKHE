import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardCheck, RefreshCw } from 'lucide-react';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getTeams } from '../teamService';
import { loadBoard } from '../kanbanService';
import './StudentTasksPage.css';

const statuses = { TODO: 'Chưa bắt đầu', IN_PROGRESS: 'Đang thực hiện', DONE: 'Hoàn thành' };

export default function StudentTasksPage() {
  const { username, fullName } = useAuthStore();
  const [teams, setTeams] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [teamId, setTeamId] = useState('');
  const [status, setStatus] = useState('');
  const [query, setQuery] = useState('');
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true); setError(''); setTasks([]);
      try {
        const result = await getTeams();
        const groups = result.data || [];
        const boards = await Promise.allSettled(groups.map(team => loadBoard(team.id)));
        if (!active) return;
        setTeams(groups);
        const failed = [];
        const assigned = boards.flatMap((result, index) => {
          const team = groups[index];
          if (result.status === 'rejected') { failed.push(team.name); return []; }
          const member = team.members.find(member => member.username === username);
          return result.value.tasks.filter(task => member && task.assigneeIds.includes(member.id))
            .map(task => ({ ...task, teamId: team.id, teamName: team.name }));
        });
        setTasks(assigned);
        if (failed.length) setError(`Không tải được nhiệm vụ của: ${failed.join(', ')}. Hãy tải lại; số liệu hiện chưa đầy đủ.`);
      } catch (exception) { if (active) setError(getApiError(exception)); }
      finally { if (active) setLoading(false); }
    }
    load();
    return () => { active = false; };
  }, [username, refresh]);

  const visible = tasks.filter(task => (!teamId || String(task.teamId) === teamId)
    && (!status || task.status === status) && task.title.toLocaleLowerCase('vi').includes(query.trim().toLocaleLowerCase('vi')))
    .sort((a, b) => (a.dueOn || '9999').localeCompare(b.dueOn || '9999'));
  return <DashboardShell role="STUDENT" displayName={fullName || username} activePath="/student/tasks" pageTitle="Nhiệm vụ">
    <div className="student-tasks-page">
      <header><div><span>CÔNG VIỆC CỦA TÔI</span><h1>Nhiệm vụ được giao</h1><p>Theo dõi công việc trong các nhóm và mở Kanban để cập nhật tiến độ.</p></div><button disabled={loading} onClick={() => setRefresh(value => value + 1)}><RefreshCw size={18}/> Tải lại</button></header>
      <section className="student-task-stats" aria-label="Thống kê nhiệm vụ">{Object.entries(statuses).map(([key, label]) => <article key={key}><ClipboardCheck size={22}/><strong>{loading ? '—' : tasks.filter(task => task.status === key).length}</strong><span>{label}</span></article>)}</section>
      <section className="student-task-filters" aria-label="Lọc nhiệm vụ">
        <label>Nhóm<select value={teamId} onChange={event => setTeamId(event.target.value)}><option value="">Tất cả nhóm</option>{teams.map(team => <option value={team.id} key={team.id}>{team.name}</option>)}</select></label>
        <label>Trạng thái<select value={status} onChange={event => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option>{Object.entries(statuses).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select></label>
        <label>Tìm nhiệm vụ<input value={query} onChange={event => setQuery(event.target.value)} placeholder="Nhập tên công việc…"/></label>
      </section>
      {error && <p className="student-task-error" role="alert">{error}</p>}
      {loading ? <p role="status">Đang tải nhiệm vụ…</p> : visible.length === 0 ? <section className="student-task-empty"><ClipboardCheck size={32}/><h2>{error ? 'Chưa có nhiệm vụ để hiển thị' : tasks.length ? 'Không có nhiệm vụ phù hợp' : 'Bạn chưa được giao nhiệm vụ'}</h2><p>{error ? 'Vui lòng tải lại dữ liệu.' : tasks.length ? 'Thử thay đổi bộ lọc hoặc từ khóa.' : 'Công việc được phân công trên Kanban của nhóm sẽ xuất hiện ở đây.'}</p><Link to="/student/teams">Xem nhóm của tôi →</Link></section> : <section className="student-task-list">{visible.map(task => <article key={`${task.teamId}-${task.id}`}><div className="student-task-title"><span>{task.teamName}</span><span className={`student-task-status ${task.status.toLowerCase()}`}>{statuses[task.status]}</span></div><h2>{task.title}</h2><p>{task.description || 'Chưa có mô tả.'}</p><small>Hạn: {task.dueOn ? new Date(`${task.dueOn}T00:00:00`).toLocaleDateString('vi-VN') : 'Chưa đặt'} · Checklist: {task.subtasks.filter(item => item.done).length}/{task.subtasks.length}</small><Link to={`/teams/${task.teamId}/kanban#task-${task.id}`}>Mở nhiệm vụ trên Kanban →</Link></article>)}</section>}
    </div>
  </DashboardShell>;
}
