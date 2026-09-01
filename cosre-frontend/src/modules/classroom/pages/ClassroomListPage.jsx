import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AcademicLayout from '../../../components/academic/AcademicLayout';
import { getApiError } from '../../../config/axios';
import { getSubjects } from '../../subject/subjectService';
import { createClassroom, getClassrooms, setClassroomStatus, updateClassroom } from '../classroomService';

// Giá trị mặc định cho form tạo/chỉnh sửa lớp học.
const emptyForm = { code: '', name: '', subjectId: '', semester: 'Học kỳ 1', academicYear: '2026-2027' };

// Trang quản lý danh sách lớp học ở giao diện người dùng.
function ClassroomListPage() {
  const navigate = useNavigate();
  // Danh sách lớp học đang hiển thị và danh sách môn học có thể chọn.
  const [items, setItems] = useState([]); const [subjects, setSubjects] = useState([]);
  // Trạng thái form tạo/chỉnh sửa và thông báo phản hồi từ server.
  const [form, setForm] = useState(emptyForm); const [editingId, setEditingId] = useState(null); const [showForm, setShowForm] = useState(false);
  const [feedback, setFeedback] = useState({ text: '', error: false });

  // Tải lại danh sách lớp học từ backend.
  const load = async () => setItems((await getClassrooms()).data || []);

  // Khi mở trang, gọi cả API lớp học và API môn học để hiển thị dữ liệu ban đầu.
  useEffect(() => { Promise.all([getClassrooms(), getSubjects()]).then(([c, s]) => { setItems(c.data || []); setSubjects((s.data || []).filter((x) => x.active)); }).catch((e) => setFeedback({ text: getApiError(e), error: true })); }, []);

  // Gửi dữ liệu form để tạo mới hoặc cập nhật lớp học.
  const save = async (event) => { event.preventDefault(); try { const payload = { ...form, subjectId: Number(form.subjectId) }; if (editingId) await updateClassroom(editingId, payload); else await createClassroom(payload); setShowForm(false); setEditingId(null); setForm(emptyForm); setFeedback({ text: 'Đã lưu lớp học.', error: false }); await load(); } catch (e) { setFeedback({ text: getApiError(e), error: true }); } };

  // Điền dữ liệu của lớp học vào form để chỉnh sửa.
  const edit = (item) => { setForm({ code: item.code, name: item.name, subjectId: String(item.subject.id), semester: item.semester, academicYear: item.academicYear }); setEditingId(item.id); setShowForm(true); };

  // Bật/tắt trạng thái hoạt động của lớp học.
  const toggle = async (item) => { try { await setClassroomStatus(item.id, !item.active); await load(); } catch (e) { setFeedback({ text: getApiError(e), error: true }); } };
  return <AcademicLayout activeTab="classrooms" title="Quản lý lớp học">
    {feedback.text && <div className={`academic-feedback ${feedback.error ? 'error' : ''}`}>{feedback.text}</div>}
    <div className="academic-toolbar"><div style={{ flex: 1 }}/>
      <button className="academic-button secondary" onClick={() => navigate('/staff/classrooms/import')}>Import từ Excel</button>
      <button className="academic-button" onClick={() => { setForm(emptyForm); setEditingId(null); setShowForm(true); }}>Thêm lớp học</button>
    </div>
    {showForm && <form className="academic-panel academic-form" onSubmit={save}>
      <label>Mã lớp<input required maxLength="50" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })}/></label><label>Tên lớp<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}/></label>
      <label>Môn học<select required value={form.subjectId} onChange={(e) => setForm({ ...form, subjectId: e.target.value })}><option value="">Chọn môn học</option>{subjects.map((s) => <option value={s.id} key={s.id}>{s.code} — {s.name}</option>)}</select></label>
      <label>Học kỳ<input required value={form.semester} onChange={(e) => setForm({ ...form, semester: e.target.value })}/></label><label>Năm học<input required pattern="\d{4}(-\d{4})?" value={form.academicYear} onChange={(e) => setForm({ ...form, academicYear: e.target.value })}/></label>
      <div className="academic-form-actions"><button type="button" className="academic-button secondary" onClick={() => setShowForm(false)}>Hủy</button><button className="academic-button">Lưu lớp học</button></div>
    </form>}
    <div className="academic-panel"><table className="academic-table"><thead><tr><th>Mã lớp</th><th>Tên lớp</th><th>Môn học</th><th>Năm học</th><th>Thành viên</th><th>Thao tác</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td><strong>{item.code}</strong></td><td>{item.name}<br/><span className={`academic-badge ${item.active ? '' : 'off'}`}>{item.active ? 'Hoạt động' : 'Đã khóa'}</span></td><td>{item.subject.code}</td><td>{item.semester}<br/>{item.academicYear}</td><td>{item.lecturers.length} GV · {item.students.length} SV</td><td className="actions"><button className="academic-button secondary" onClick={() => navigate(`/staff/classrooms/${item.id}`)}>Chi tiết</button><button className="academic-button secondary" onClick={() => edit(item)}>Sửa</button><button className={`academic-button ${item.active ? 'danger' : 'secondary'}`} onClick={() => toggle(item)}>{item.active ? 'Khóa' : 'Mở'}</button></td></tr>)}</tbody></table>{items.length === 0 && <div className="academic-empty">Chưa có lớp học.</div>}</div>
  </AcademicLayout>;
}
export default ClassroomListPage;
