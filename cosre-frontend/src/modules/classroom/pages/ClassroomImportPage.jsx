import { useRef, useState } from 'react';
import { Download, FileSpreadsheet, Upload, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AcademicLayout from '../../../components/academic/AcademicLayout';
import { getApiError } from '../../../config/axios';
import { downloadBlob } from '../../../config/download';
import { downloadClassroomTemplate, importClassrooms } from '../classroomService';
import '../../../components/academic/AcademicLayout.css';
import '../../account/pages/AccountImportPage.css';
import '../../account/pages/AccountImportWorkspace.css';

function getImportError(error) {
  if (!error.response) return 'Không thể kết nối backend. Vui lòng kiểm tra backend đang chạy tại cổng 8080.';
  if (error.response.data?.message) return error.response.data.message;
  if (error.response.status === 403) return 'Tài khoản hiện tại không có quyền import. Vui lòng đăng nhập bằng vai trò STAFF.';
  if (error.response.status === 413) return 'Tệp vượt quá dung lượng tối đa 5 MB.';
  if (error.response.status === 415) return 'Backend không nhận được tệp CSV/XLSX. Vui lòng chọn lại tệp.';
  return `Import thất bại (HTTP ${error.response.status}). Vui lòng thử lại.`;
}

// Trang import hàng loạt lớp học từ tệp CSV/XLSX.
function ClassroomImportPage() {
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [result, setResult] = useState(null);

  const chooseFile = (event) => {
    setFile(event.target.files?.[0] || null);
    setResult(null);
    setFeedback('');
  };

  const downloadTemplate = async () => {
    setDownloading(true);
    try {
      const blob = await downloadClassroomTemplate();
      downloadBlob(blob, 'mau-import-lop-hoc.xlsx');
    } catch (error) {
      setFeedback(getApiError(error, 'Không thể tải tệp mẫu.'));
    } finally {
      setDownloading(false);
    }
  };

  const submit = async () => {
    if (!file) return setFeedback('Vui lòng chọn tệp CSV hoặc XLSX trước khi import.');
    setLoading(true);
    setFeedback('');
    setResult(null);
    try {
      const response = await importClassrooms(file);
      setResult(response.data);
      setFeedback(response.message);
    } catch (error) {
      setFeedback(getImportError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AcademicLayout activeTab="classrooms" title="Import danh sách lớp học">
      <div className="import-workspace-page">
        <div className="academic-toolbar">
          <div style={{ flex: 1 }} />
          <button type="button" className="academic-button secondary" onClick={() => navigate('/staff/classrooms')}>
            ← Danh sách lớp
          </button>
        </div>
        <section className="import-guide">
          <div>
            <h2>Import danh sách lớp học</h2>
            <p>
              Tệp cần có đúng 5 cột: <code>code</code>, <code>name</code>, <code>subjectCode</code>,{' '}
              <code>semester</code>, <code>academicYear</code>.
            </p>
            <p>
              <code>subjectCode</code> phải là mã môn học đã tồn tại trong hệ thống. Năm học theo định dạng{' '}
              <strong>YYYY</strong> hoặc <strong>YYYY-YYYY</strong>. Dung lượng tối đa 5 MB.
            </p>
          </div>
          <button type="button" className="academic-button secondary" disabled={downloading} onClick={downloadTemplate}>
            <Download size={17} /> {downloading ? 'Đang tải…' : 'Tải Excel mẫu'}
          </button>
        </section>
        <section className="academic-panel import-panel">
          <input ref={inputRef} type="file" accept=".csv,.xlsx" onChange={chooseFile} hidden />
          <button type="button" className="import-dropzone" onClick={() => inputRef.current?.click()}>
            <FileSpreadsheet size={42} />
            <strong>{file ? file.name : 'Chọn tệp danh sách lớp học'}</strong>
            <span>Hỗ trợ định dạng .csv và .xlsx</span>
          </button>
          <div className="import-actions">
            <button type="button" className="academic-button" disabled={loading || !file} onClick={submit}>
              <Upload size={17} /> {loading ? 'Đang import…' : 'Bắt đầu import'}
            </button>
          </div>
        </section>
        {feedback && <div className={`academic-feedback ${!result ? 'error' : ''}`}>{feedback}</div>}
        {result && (
          <section className="import-result">
            <div className="import-summary">
              <article>
                <span>Tổng số dòng</span>
                <strong>{result.totalRows}</strong>
              </article>
              <article className="success">
                <span>Đã tạo</span>
                <strong>{result.importedCount}</strong>
              </article>
              <article className="failed">
                <span>Không hợp lệ</span>
                <strong>{result.failedCount}</strong>
              </article>
            </div>
            {result.errors?.length > 0 && (
              <div className="academic-panel">
                <table className="academic-table">
                  <thead>
                    <tr>
                      <th>Dòng</th>
                      <th>Mã lớp</th>
                      <th>Lý do</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.errors.map((item) => (
                      <tr key={`${item.row}-${item.code}`}>
                        <td>{item.row}</td>
                        <td>{item.code || '—'}</td>
                        <td className="import-error">
                          <XCircle size={16} />
                          {item.message}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}
      </div>
    </AcademicLayout>
  );
}

export default ClassroomImportPage;
