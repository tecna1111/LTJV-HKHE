import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, BookOpen, CalendarDays, ChevronDown, CircleAlert, GraduationCap, Users } from 'lucide-react';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getClassrooms } from '../classroomService';
import './MyClassroomsPage.css';

function MemberList({ members, emptyText }) {
  return members.length === 0 ? <p className="classrooms-muted">{emptyText}</p> : <ul className="classrooms-members">{members.map((member) => <li key={member.id}>
    <span className="classrooms-avatar" aria-hidden="true">{(member.fullName || member.username || '?').slice(0, 1).toUpperCase()}</span>
    <div><strong>{member.fullName || member.username}</strong><small>@{member.username}</small></div>
  </li>)}</ul>;
}

export default function MyClassroomsPage() {
  const role = useAuthStore((state) => state.role);
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const title = role === 'LECTURER' ? 'Lớp học phụ trách' : role === 'STUDENT' ? 'Lớp học của tôi' : 'Lớp học';

  useEffect(() => {
    let active = true;
    getClassrooms().then((result) => {
      if (!result.success) throw new Error(result.message || 'Không thể tải lớp học.');
      if (active) setItems(result.data || []);
    }).catch((err) => {
      if (active) setError(getApiError(err, 'Không thể tải lớp học. Vui lòng thử lại.'));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [attempt]);

  return <DashboardShell role={role} displayName={displayName} activePath="/classrooms" pageTitle={title}>
    <div className="workspace-page classrooms-page">
      <Link to="/dashboard" className="classrooms-back"><ArrowLeft size={16} aria-hidden="true"/> Quay lại tổng quan</Link>
      <section className="workspace-heading"><div><span>KHÔNG GIAN LỚP HỌC</span><h1>{title}</h1><p>Theo dõi thông tin môn học và thành viên trong các lớp của bạn.</p></div></section>
      <div className="classrooms-section-heading"><h2>Danh sách lớp học</h2>{!loading && !error && <span>{items.length} lớp học</span>}</div>
      {loading ? <div className="classrooms-empty" role="status"><span className="classrooms-icon"><BookOpen size={26}/></span><h3>Đang tải lớp học...</h3></div>
        : error ? <div className="classrooms-empty" role="alert"><span className="classrooms-icon"><CircleAlert size={26}/></span><h3>Không thể tải danh sách lớp</h3><p>{error}</p><button className="classrooms-retry" onClick={() => { setError(''); setLoading(true); setAttempt((value) => value + 1); }}>Thử lại</button></div>
        : items.length === 0 ? <div className="classrooms-empty"><span className="classrooms-icon"><BookOpen size={26}/></span><h3>Chưa có lớp học</h3><p>Các lớp được phân công cho bạn sẽ xuất hiện tại đây.</p></div>
        : <div className="classrooms-grid">{items.map((item) => <article className="classrooms-card" key={item.id}>
          <div className="classrooms-card-heading"><span className="classrooms-icon"><BookOpen size={23} aria-hidden="true"/></span><span className={`classrooms-status ${item.active ? '' : 'is-locked'}`}>{item.active ? 'Hoạt động' : 'Đã khóa'}</span></div>
          <span className="classrooms-code">{item.code}</span>
          <h2>{item.name}</h2>
          <p className="classrooms-subject">{item.subject.code} — {item.subject.name}</p>
          <p className="classrooms-term"><CalendarDays size={16} aria-hidden="true"/>{item.semester} · {item.academicYear}</p>
          <div className="classrooms-counts"><span><GraduationCap size={17} aria-hidden="true"/>{item.lecturers.length} giảng viên</span><span><Users size={17} aria-hidden="true"/>{item.students.length} sinh viên</span></div>
          <details className="classrooms-details">
            <summary>Xem thành viên lớp<ChevronDown size={17} aria-hidden="true"/></summary>
            <div className="classrooms-roster"><h3>Giảng viên <span>{item.lecturers.length}</span></h3>
              <MemberList members={item.lecturers} emptyText="Chưa có giảng viên."/>
              <h3>Sinh viên <span>{item.students.length}</span></h3>
              <MemberList members={item.students} emptyText="Chưa có sinh viên."/>
            </div>
          </details>
        </article>)}</div>}
    </div>
  </DashboardShell>;
}
