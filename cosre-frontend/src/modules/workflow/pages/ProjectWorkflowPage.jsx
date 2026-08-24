import { useEffect, useState } from 'react';
import { Check, FileText, FolderKanban, Link2, Plus, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { assignProjectToClassroom, createSyllabus, getApprovedProjects, getClassroomProjects, getClassrooms, getMyProjects, getReviewProjects, getSubjects, getSyllabi, reviewProject } from '../workflowService';
import './ProjectWorkflowPage.css';

const statusLabel = { DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', DENIED: 'Từ chối' };

export default function ProjectWorkflowPage() {
  const role = useAuthStore((state) => state.role);
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const [searchParams] = useSearchParams();
  const selectedStatus = searchParams.get('status') || 'PENDING';
  const [projects, setProjects] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [syllabi, setSyllabi] = useState([]);
  const [assignedClasses, setAssignedClasses] = useState({});
  const [subjectId, setSubjectId] = useState('');
  const [form, setForm] = useState({ title: '', version: '1.0', content: '', objectives: '' });
  const [feedback, setFeedback] = useState('');

  const load = async () => {
    try {
      const [subjectData, classroomData] = await Promise.all([getSubjects(), getClassrooms()]);
      setSubjects(subjectData); setClassrooms(classroomData);
      if (role === 'LECTURER' || role === 'HEAD_DEPT') {
        const assignments = await Promise.all(classroomData.map(async (classroom) => {
          try { return [classroom, await getClassroomProjects(classroom.id)]; }
          catch { return [classroom, []]; }
        }));
        const byProject = {};
        assignments.forEach(([classroom, classroomProjects]) => classroomProjects.forEach((project) => {
          byProject[project.id] = [...(byProject[project.id] || []), classroom.code];
        }));
        setAssignedClasses(byProject);
      }
      if (role === 'HEAD_DEPT') setProjects(await getReviewProjects());
      if (role === 'LECTURER') {
        const [mine, approved] = await Promise.all([getMyProjects(), getApprovedProjects()]);
        setProjects([...new Map([...mine, ...approved].map((item) => [item.id, item])).values()]);
      }
    } catch (error) { setFeedback(getApiError(error, 'Không thể tải dữ liệu luồng dự án.')); }
  };
  useEffect(() => { // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (subjectId) getSyllabi(subjectId).then(setSyllabi).catch(() => setSyllabi([])); }, [subjectId]);

  const submitSyllabus = async (event) => {
    event.preventDefault();
    try { await createSyllabus({ ...form, subjectId: Number(subjectId) }); setFeedback('Đã tạo đề cương.'); setSyllabi(await getSyllabi(subjectId)); setForm({ title: '', version: '1.0', content: '', objectives: '' }); }
    catch (error) { setFeedback(getApiError(error)); }
  };
  const decide = async (project, approved) => {
    const note = window.prompt(approved ? 'Ghi chú phê duyệt (có thể để trống):' : 'Nhập lý do từ chối:') ?? '';
    if (!approved && !note.trim()) return;
    try { await reviewProject(project.id, approved, note); setFeedback(approved ? 'Đã phê duyệt dự án.' : 'Đã từ chối dự án.'); await load(); }
    catch (error) { setFeedback(getApiError(error)); }
  };
  const assign = async (project) => {
    const eligible = classrooms.filter((item) => item.subject.id === project.subjectId);
    const raw = window.prompt(`Nhập ID lớp (${eligible.map((item) => `${item.id}: ${item.code}`).join(', ')}):`);
    if (!raw) return;
    try { await assignProjectToClassroom(project.id, Number(raw)); setFeedback('Đã gán dự án cho lớp.'); await load(); }
    catch (error) { setFeedback(getApiError(error)); }
  };

  const visibleProjects = role === 'HEAD_DEPT' ? projects.filter((project) => project.status === selectedStatus) : projects;
  const activePath = role === 'HEAD_DEPT' ? `/workflow?status=${selectedStatus}` : '/workflow';

  return <DashboardShell role={role} displayName={displayName} activePath={activePath} pageTitle="Luồng dự án">
    <div className="workflow-page"><header><div><span>CORE WORKFLOW</span><h1>{role === 'STAFF' ? 'Đề cương môn học' : 'Phê duyệt & phân công dự án'}</h1><p>Luồng liên kết dữ liệu từ đề cương đến lớp và nhóm sinh viên.</p></div></header>
      {feedback && <div className="workflow-feedback">{feedback}</div>}
      {role === 'STAFF' ? <div className="workflow-columns"><form className="workflow-panel" onSubmit={submitSyllabus}><h2><Plus size={18}/> Tạo đề cương</h2>
        <label>Môn học<select required value={subjectId} onChange={(e) => setSubjectId(e.target.value)}><option value="">Chọn môn học</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.code} — {s.name}</option>)}</select></label>
        <label>Tiêu đề<input required value={form.title} onChange={(e) => setForm({...form, title:e.target.value})}/></label><label>Phiên bản<input required value={form.version} onChange={(e) => setForm({...form, version:e.target.value})}/></label>
        <label>Nội dung<textarea value={form.content} onChange={(e) => setForm({...form, content:e.target.value})}/></label><label>Mục tiêu (mỗi dòng một mục tiêu)<textarea required value={form.objectives} onChange={(e) => setForm({...form, objectives:e.target.value})}/></label><button>Tạo đề cương</button></form>
        <section className="workflow-panel"><h2><FileText size={18}/> Danh sách đề cương</h2>{syllabi.map((s) => <article className="workflow-card" key={s.id}><b>{s.title}</b><small>{s.subjectCode} · v{s.version}</small><p>{s.objectives.join(' · ')}</p></article>)}</section></div>
      : <section className="workflow-panel"><h2><FolderKanban size={18}/> Danh sách dự án</h2>{visibleProjects.length === 0 ? <p>Chưa có dự án phù hợp.</p> : visibleProjects.map((p) => <article className="workflow-card" key={p.id}><div><b>{p.title}</b><span className={`status ${p.status.toLowerCase()}`}>{statusLabel[p.status]}</span></div><small>Subject #{p.subjectId} · Syllabus #{p.syllabusId || '—'} · Lecturer #{p.createdBy}</small><p>{p.description}</p><ul>{p.milestones.map((m) => <li key={m.id || m.title}>{m.title} ({m.dueOffsetDays} ngày)</li>)}</ul>{assignedClasses[p.id]?.length > 0 && <p className="assigned-classrooms"><Check size={15}/> Đã gán: {assignedClasses[p.id].join(', ')}</p>}<footer>{role === 'HEAD_DEPT' && p.status === 'PENDING' && <><button onClick={() => decide(p, true)}><Check size={15}/> Duyệt</button><button className="danger" onClick={() => decide(p, false)}><X size={15}/> Từ chối</button></>}{p.status === 'APPROVED' && <button onClick={() => assign(p)}><Link2 size={15}/> {assignedClasses[p.id]?.length ? 'Gán thêm lớp' : 'Gán cho lớp'}</button>}</footer></article>)}</section>}
    </div></DashboardShell>;
}
