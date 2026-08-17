import { useEffect, useMemo, useState } from 'react';
import { BookOpenCheck, CheckCircle2, Crown, FolderKanban, GraduationCap, LogOut, Plus, RefreshCw, Trash2, UserPlus, Users, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import BrandLogo from '../../../components/BrandLogo';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { addTeamMember, assignTeamProject, createTeam, deleteTeam, getAvailableStudents, getTeams, removeTeamMember, getLecturerClassrooms, getClassroomProjects } from '../teamService';
import './TeamManagementPage.css';

const emptyForm = { name: '', description: '', classroomId: '', projectId: '', leaderId: '', memberIds: [] };

function TeamManagementPage() {
  const navigate = useNavigate();
  const { fullName, username, clearAuth } = useAuthStore();
  const [teams, setTeams] = useState([]);
  const [students, setStudents] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [projects, setProjects] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState('');
  const [feedback, setFeedback] = useState({ type: '', text: '' });

  const loadTeams = async () => {
    setLoading(true);
    try { const result = await getTeams(); setTeams(result.data || []); }
    catch (error) { setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách nhóm.') }); }
    finally { setLoading(false); }
  };
  useEffect(() => {
    getLecturerClassrooms().then(setClassrooms).catch(() => setClassrooms([]));
    getTeams()
      .then((result) => setTeams(result.data || []))
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách nhóm.') }))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const id = Number(form.classroomId);
    if (!id) return undefined;
    const timer = setTimeout(() => getAvailableStudents(id)
      .then((result) => setStudents(result.data || []))
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải sinh viên khả dụng.') })), 300);
    return () => clearTimeout(timer);
  }, [form.classroomId]);

  useEffect(() => {
    const id = Number(form.classroomId);
    if (!id) return;
    getClassroomProjects(id).then(setProjects).catch(() => setProjects([]));
  }, [form.classroomId]);

  const counts = useMemo(() => ({ teams: teams.length, members: teams.reduce((sum, team) => sum + team.members.length, 0), projects: teams.filter((team) => team.projectId).length }), [teams]);
  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const toggleStudent = (id) => setForm((current) => ({ ...current, memberIds: current.memberIds.includes(id) ? current.memberIds.filter((value) => value !== id) : [...current.memberIds, id] }));

  const submit = async (event) => {
    event.preventDefault();
    if (form.memberIds.length === 0) {
      setFeedback({ type: 'error', text: 'Vui lòng chọn ít nhất một thành viên.' });
      return;
    }
    if (form.leaderId && !form.memberIds.includes(Number(form.leaderId))) {
      setFeedback({ type: 'error', text: 'Trưởng nhóm phải thuộc danh sách thành viên đã chọn.' });
      return;
    }
    setSaving(true); setFeedback({ type: '', text: '' });
    try {
      await createTeam({ ...form, classroomId: Number(form.classroomId), projectId: form.projectId ? Number(form.projectId) : null, leaderId: form.leaderId ? Number(form.leaderId) : null });
      setForm(emptyForm); setShowForm(false); setFeedback({ type: 'success', text: 'Tạo nhóm thành công.' }); await loadTeams();
    } catch (error) { setFeedback({ type: 'error', text: getApiError(error, 'Không thể tạo nhóm.') }); }
    finally { setSaving(false); }
  };

  const run = async (key, action, message) => {
    setBusy(key); setFeedback({ type: '', text: '' });
    try { await action(); setFeedback({ type: 'success', text: message }); await loadTeams(); }
    catch (error) { setFeedback({ type: 'error', text: getApiError(error) }); }
    finally { setBusy(''); }
  };

  const addMember = async (team) => {
    const raw = window.prompt('Nhập ID sinh viên cần thêm:');
    if (!raw) return;
    await run(`add-${team.id}`, () => addTeamMember(team.id, Number(raw)), 'Đã thêm thành viên.');
  };
  const chooseProject = async (team) => {
    const available = await getClassroomProjects(team.classroomId);
    const raw = window.prompt(`Chọn ID đề tài của lớp (${available.map((item) => `${item.id}: ${item.title}`).join(', ')}):`, team.projectId || '');
    if (!raw) return;
    await run(`project-${team.id}`, () => assignTeamProject(team.id, Number(raw)), 'Đã chọn đề tài cho nhóm.');
  };

  return (
    <main className="team-shell">
      <aside className="team-sidebar">
        <BrandLogo />
        <nav><button onClick={() => navigate('/dashboard')}><FolderKanban size={19} /> Tổng quan</button><button className="active"><Users size={19} /> Nhóm học tập</button></nav>
        <button className="team-logout" onClick={() => { clearAuth(); navigate('/login'); }}><LogOut size={18} /> Đăng xuất</button>
      </aside>
      <section className="team-main">
        <header className="team-topbar"><div><small>WORKSPACE / TEAMS</small><strong>Xin chào, {fullName || username}</strong></div><button onClick={loadTeams} aria-label="Làm mới"><RefreshCw size={18} /></button></header>
        <div className="team-content">
          <div className="team-heading"><div><span>TEAM MANAGEMENT</span><h1>Nhóm học tập</h1><p>Tạo nhóm, phân công thành viên và chọn đề tài cho từng lớp.</p></div><button className="team-primary" onClick={() => setShowForm(true)}><Plus size={18} /> Tạo nhóm mới</button></div>
          <div className="team-stats"><article><Users /><div><strong>{counts.teams}</strong><span>Nhóm đang quản lý</span></div></article><article><GraduationCap /><div><strong>{counts.members}</strong><span>Sinh viên</span></div></article><article><BookOpenCheck /><div><strong>{counts.projects}</strong><span>Nhóm đã chọn đề tài</span></div></article></div>
          {feedback.text && <div className={`team-feedback ${feedback.type}`}>{feedback.type === 'success' ? <CheckCircle2 size={17} /> : <X size={17} />}{feedback.text}</div>}
          {loading ? <div className="team-empty">Đang tải danh sách nhóm…</div> : teams.length === 0 ? <div className="team-empty"><Users size={36} /><h3>Chưa có nhóm nào</h3><p>Tạo nhóm đầu tiên để bắt đầu phân công sinh viên.</p></div> : <div className="team-grid">{teams.map((team) => (
            <article className="team-card" key={team.id}>
              <div className="team-card-head"><span>LỚP #{team.classroomId}</span><button disabled={busy === `delete-${team.id}`} onClick={() => { if (window.confirm(`Xóa nhóm ${team.name}?`)) run(`delete-${team.id}`, () => deleteTeam(team.id), 'Đã xóa nhóm.'); }}><Trash2 size={16} /></button></div>
              <h2>{team.name}</h2><p>{team.description || 'Chưa có mô tả cho nhóm.'}</p>
              <button className="team-project" onClick={() => navigate(`/teams/${team.id}/workspace`)}><FolderKanban size={17} /> Mở workspace</button>
              <button className={`team-project ${team.projectId ? 'selected' : ''}`} onClick={() => chooseProject(team)}><BookOpenCheck size={17} />{team.projectId ? `Đề tài #${team.projectId}` : 'Chọn đề tài'} </button>
              <div className="team-members-title"><strong>Thành viên ({team.members.length})</strong><button onClick={() => addMember(team)}><UserPlus size={15} /> Thêm</button></div>
              <div className="team-members">{team.members.map((member) => <div key={member.id}><span>{member.fullName.slice(0, 1).toUpperCase()}</span><div><strong>{member.fullName}</strong><small>@{member.username}</small></div>{team.leader?.id === member.id && <Crown size={15} className="leader" />}<button aria-label={`Xóa ${member.fullName}`} onClick={() => run(`remove-${team.id}-${member.id}`, () => removeTeamMember(team.id, member.id), 'Đã xóa thành viên.')}><X size={14} /></button></div>)}</div>
            </article>
          ))}</div>}
        </div>
      </section>
      {showForm && <div className="team-modal-backdrop" onMouseDown={() => setShowForm(false)}><form className="team-modal" onSubmit={submit} onMouseDown={(event) => event.stopPropagation()}><div className="team-modal-title"><div><span>TẠO NHÓM</span><h2>Thông tin nhóm mới</h2></div><button type="button" onClick={() => setShowForm(false)}><X /></button></div>
        {feedback.text && <div className={`team-feedback ${feedback.type}`}>{feedback.type === 'success' ? <CheckCircle2 size={17} /> : <X size={17} />}{feedback.text}</div>}
        <label>Tên nhóm<input required maxLength="100" value={form.name} onChange={(event) => update('name', event.target.value)} placeholder="Ví dụ: Innovation Squad" /></label>
        <label>Mô tả<textarea maxLength="500" value={form.description} onChange={(event) => update('description', event.target.value)} placeholder="Mục tiêu hoặc ghi chú ngắn…" /></label>
        <div className="team-form-row"><label>Lớp học<select required value={form.classroomId} onChange={(event) => { setStudents([]); setProjects([]); update('classroomId', event.target.value); update('projectId', ''); }}><option value="">Chọn lớp phụ trách</option>{classrooms.map((item) => <option key={item.id} value={item.id}>{item.code} — {item.name}</option>)}</select></label><label>Đề tài đã gán cho lớp<select value={form.projectId} onChange={(event) => update('projectId', event.target.value)}><option value="">Chọn sau</option>{projects.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}</select></label></div>
        <fieldset><legend>Chọn thành viên khả dụng</legend>{!form.classroomId ? <p>Nhập ID lớp để tải sinh viên.</p> : students.length === 0 ? <p>Không có sinh viên khả dụng.</p> : <div className="student-picker">{students.map((student) => <label key={student.id}><input type="checkbox" checked={form.memberIds.includes(student.id)} onChange={() => toggleStudent(student.id)} /><span>{student.fullName}<small>@{student.username}</small></span></label>)}</div>}</fieldset>
        {form.memberIds.length > 0 && <label>Trưởng nhóm<select value={form.leaderId} onChange={(event) => update('leaderId', event.target.value)}><option value="">Chưa chỉ định</option>{students.filter((student) => form.memberIds.includes(student.id)).map((student) => <option value={student.id} key={student.id}>{student.fullName}</option>)}</select></label>}
        <div className="team-modal-actions"><button type="button" onClick={() => setShowForm(false)}>Hủy</button><button type="submit" className="team-primary" disabled={saving}>{saving ? 'Đang tạo…' : 'Tạo nhóm'}</button></div>
      </form></div>}
    </main>
  );
}

export default TeamManagementPage;
