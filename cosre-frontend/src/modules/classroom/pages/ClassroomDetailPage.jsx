import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AcademicLayout from '../../../components/academic/AcademicLayout';
import { getApiError } from '../../../config/axios';
import { addClassMember, getClassroom, getEligibleMembers, removeClassMember } from '../classroomService';

function MemberSection({ title, type, members, options, classroomId, onChanged }) {
  const [selected, setSelected] = useState('');
  const available = options.filter((candidate) => !members.some((member) => member.id === candidate.id));
  const add = async () => { if (!selected) return; await addClassMember(classroomId, type, selected); setSelected(''); await onChanged(); };
  const remove = async (id) => { await removeClassMember(classroomId, type, id); await onChanged(); };
  return <section className="member-card"><h3>{title} ({members.length})</h3><div className="member-add"><select value={selected} onChange={(e) => setSelected(e.target.value)}><option value="">Chọn tài khoản</option>{available.map((user) => <option value={user.id} key={user.id}>{user.fullName} — {user.username}</option>)}</select><button className="academic-button" onClick={add}>Thêm</button></div><div className="member-list">{members.map((user) => <div className="member-row" key={user.id}><div><strong>{user.fullName}</strong><small>{user.email}</small></div><button className="academic-button danger" onClick={() => remove(user.id)}>Xóa</button></div>)}{members.length === 0 && <div className="academic-empty">Chưa có thành viên.</div>}</div></section>;
}

function ClassroomDetailPage() {
  const { id } = useParams(); const navigate = useNavigate();
  const [item, setItem] = useState(null); const [lecturers, setLecturers] = useState([]); const [students, setStudents] = useState([]); const [error, setError] = useState('');
  const load = async () => { try { setItem((await getClassroom(id)).data); } catch (e) { setError(getApiError(e)); } };
  useEffect(() => { Promise.all([getClassroom(id), getEligibleMembers('LECTURER'), getEligibleMembers('STUDENT')]).then(([c, l, s]) => { setItem(c.data); setLecturers(l.data || []); setStudents(s.data || []); }).catch((e) => setError(getApiError(e))); }, [id]);
  const changed = async () => { try { await load(); } catch (e) { setError(getApiError(e)); } };
  return <AcademicLayout activeTab="classrooms" title={item ? item.name : 'Chi tiết lớp học'}>{error && <div className="academic-feedback error">{error}</div>}<div className="academic-toolbar"><button className="academic-button secondary" onClick={() => navigate('/staff/classrooms')}>← Danh sách lớp</button>{item && <div><strong>{item.code}</strong> · {item.subject.code} · {item.semester} {item.academicYear}</div>}</div>{item && <div className="academic-panel member-grid"><MemberSection title="Giảng viên" type="lecturers" members={item.lecturers} options={lecturers} classroomId={id} onChanged={changed}/><MemberSection title="Sinh viên" type="students" members={item.students} options={students} classroomId={id} onChanged={changed}/></div>}</AcademicLayout>;
}
export default ClassroomDetailPage;
