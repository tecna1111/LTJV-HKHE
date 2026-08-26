import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Download, Upload } from 'lucide-react';
import AcademicLayout from '../../../components/academic/AcademicLayout';
import { getApiError } from '../../../config/axios';
import { downloadBlob } from '../../../config/download';
import {
  addClassMember,
  downloadMemberTemplate,
  getClassroom,
  getEligibleMembers,
  importClassMembers,
  removeClassMember,
} from '../classroomService';

// Khối import file Excel/CSV để gán hàng loạt thành viên (SV hoặc GV) vào lớp học.
function MemberImportBox({ type, classroomId, onImported }) {
  const inputRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [result, setResult] = useState(null);

  const downloadTemplate = async () => {
    setDownloading(true);
    setFeedback('');
    try {
      const blob = await downloadMemberTemplate();
      downloadBlob(blob, 'mau-import-thanh-vien-lop.xlsx');
    } catch (error) {
      setFeedback(getApiError(error, 'Không thể tải tệp mẫu.'));
    } finally {
      setDownloading(false);
    }
  };

  const chooseAndImport = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setLoading(true);
    setFeedback('');
    setResult(null);
    try {
      const response = await importClassMembers(classroomId, type, file);
      setResult(response.data);
      setFeedback(response.message);
      await onImported();
    } catch (error) {
      setFeedback(getApiError(error, 'Import thất bại. Vui lòng thử lại.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="member-import">
      <input ref={inputRef} type="file" accept=".csv,.xlsx" onChange={chooseAndImport} hidden />
      <div className="member-import-actions">
        <button type="button" className="academic-button secondary" disabled={downloading} onClick={downloadTemplate}>
          <Download size={15} /> Tải mẫu
        </button>
        <button type="button" className="academic-button secondary" disabled={loading} onClick={() => inputRef.current?.click()}>
          <Upload size={15} /> {loading ? 'Đang import…' : 'Import từ Excel'}
        </button>
      </div>
      {feedback && <p className={`member-import-feedback ${result ? '' : 'error'}`}>{feedback}</p>}
      {result?.errors?.length > 0 && (
        <ul className="member-import-errors">
          {result.errors.map((item) => (
            <li key={`${item.row}-${item.username}`}>
              Dòng {item.row} ({item.username || '—'}): {item.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function MemberSection({ title, type, members, options, classroomId, onChanged }) {
  const [selected, setSelected] = useState('');
  const available = options.filter((candidate) => !members.some((member) => member.id === candidate.id));
  const add = async () => { if (!selected) return; await addClassMember(classroomId, type, selected); setSelected(''); await onChanged(); };
  const remove = async (id) => { await removeClassMember(classroomId, type, id); await onChanged(); };
  return <section className="member-card"><h3>{title} ({members.length})</h3>
    <div className="member-add"><select value={selected} onChange={(e) => setSelected(e.target.value)}><option value="">Chọn tài khoản</option>{available.map((user) => <option value={user.id} key={user.id}>{user.fullName} — {user.username}</option>)}</select><button className="academic-button" onClick={add}>Thêm</button></div>
    <MemberImportBox type={type} classroomId={classroomId} onImported={onChanged} />
    <div className="member-list">{members.map((user) => <div className="member-row" key={user.id}><div><strong>{user.fullName}</strong><small>{user.email}</small></div><button className="academic-button danger" onClick={() => remove(user.id)}>Xóa</button></div>)}{members.length === 0 && <div className="academic-empty">Chưa có thành viên.</div>}</div>
  </section>;
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
