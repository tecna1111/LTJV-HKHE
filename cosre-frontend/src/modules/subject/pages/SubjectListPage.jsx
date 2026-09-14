import { useEffect, useState } from 'react';
import AcademicLayout from '../../../components/academic/AcademicLayout';
import { getApiError } from '../../../config/axios';
import { createSubject, getSubjects, setSubjectStatus, updateSubject } from '../subjectService';

// Giá trị mặc định cho form tạo/chỉnh sửa môn học.
const emptyForm = { code: '', name: '', description: '', credits: 3 };

// Trang quản lý danh sách môn học ở giao diện người dùng.
function SubjectListPage() {
  // State lưu danh sách môn học, từ khóa tìm kiếm và dữ liệu form.
  const [subjects, setSubjects] = useState([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [feedback, setFeedback] = useState({ text: '', error: false });

  // Tải lại danh sách môn học, có thể gửi từ khóa tìm kiếm.
  const load = async (value = '') => { try { setSubjects((await getSubjects(value)).data || []); } catch (e) { setFeedback({ text: getApiError(e), error: true }); } };

  // Khi mở trang, gọi API để tải danh sách môn học ban đầu.
  useEffect(() => { getSubjects().then((r) => setSubjects(r.data || [])).catch((e) => setFeedback({ text: getApiError(e), error: true })); }, []);

  // Gửi dữ liệu form để tạo mới hoặc cập nhật môn học.
  const save = async (event) => {
    event.preventDefault();
    try {
      const payload = { ...form, credits: Number(form.credits) };
      if (editingId) await updateSubject(editingId, payload); else await createSubject(payload);
      setFeedback({ text: editingId ? 'Đã cập nhật môn học.' : 'Đã tạo môn học.', error: false });
      setForm(emptyForm); setEditingId(null); setShowForm(false); await load(query);
    } catch (e) { setFeedback({ text: getApiError(e), error: true }); }
  };
  const edit = (item) => { setForm({ code: item.code, name: item.name, description: item.description || '', credits: item.credits }); setEditingId(item.id); setShowForm(true); };
  const toggle = async (item) => { try { await setSubjectStatus(item.id, !item.active); await load(query); } catch (e) { setFeedback({ text: getApiError(e), error: true }); } };
  return <AcademicLayout activeTab="subjects" title="Quản lý môn học">
    {feedback.text && <div className={`academic-feedback ${feedback.error ? 'error' : ''}`}>{feedback.text}</div>}
    <div className="academic-toolbar"><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo mã hoặc tên môn học"/><button className="academic-button secondary" onClick={() => load(query)}>Tìm</button><button className="academic-button" onClick={() => { setShowForm(true); setEditingId(null); setForm(emptyForm); }}>Thêm môn học</button></div>
    {showForm && <form className="academic-panel academic-form" onSubmit={save}>
      <label>Mã môn<input required maxLength="30" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })}/></label>
      <label>Tên môn<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}/></label>
      <label>Số tín chỉ<input required type="number" min="1" max="30" value={form.credits} onChange={(e) => setForm({ ...form, credits: e.target.value })}/></label>
      <label className="wide">Mô tả<textarea rows="3" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}/></label>
      <div className="academic-form-actions"><button type="button" className="academic-button secondary" onClick={() => setShowForm(false)}>Hủy</button><button className="academic-button">{editingId ? 'Lưu thay đổi' : 'Tạo môn học'}</button></div>
    </form>}
    <div className="academic-panel"><table className="academic-table"><thead><tr><th>Mã</th><th>Tên môn học</th><th>Tín chỉ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{subjects.map((item) => <tr key={item.id}><td><strong>{item.code}</strong></td><td>{item.name}</td><td>{item.credits}</td><td><span className={`academic-badge ${item.active ? '' : 'off'}`}>{item.active ? 'Hoạt động' : 'Đã khóa'}</span></td><td className="actions"><button className="academic-button secondary" onClick={() => edit(item)}>Sửa</button><button className={`academic-button ${item.active ? 'danger' : 'secondary'}`} onClick={() => toggle(item)}>{item.active ? 'Khóa' : 'Mở'}</button></td></tr>)}</tbody></table>{subjects.length === 0 && <div className="academic-empty">Chưa có môn học.</div>}</div>
  </AcademicLayout>;
}
export default SubjectListPage;
