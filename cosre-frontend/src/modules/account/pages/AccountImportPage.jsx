import { useRef, useState } from 'react';
import { Download, FileSpreadsheet, Upload, XCircle } from 'lucide-react';
import useAuthStore from '../../../store/useAuthStore';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import { importUsers } from '../accountService';
import '../../../components/academic/AcademicLayout.css';
import './AccountImportPage.css';
import './AccountImportWorkspace.css';

const TEMPLATE = [
  'sep=,',
  'username,email,fullName,password,role',
  'sv001,sv001@example.edu.vn,Nguy\u1EC5n V\u0103n An,Student@123,STUDENT',
  'gv001,gv001@example.edu.vn,Tr\u1EA7n Th\u1ECB B\u00ECnh,Lecturer@123,LECTURER',
].join('\n');

function getImportError(error) {
  if (!error.response) return 'Không thể kết nối backend. Vui lòng kiểm tra backend đang chạy tại cổng 8080.';
  if (error.response.data?.message) return error.response.data.message;
  if (error.response.status === 403) return 'Tài khoản hiện tại không có quyền import. Vui lòng đăng nhập bằng vai trò STAFF.';
  if (error.response.status === 413) return 'Tệp vượt quá dung lượng tối đa 5 MB.';
  if (error.response.status === 415) return 'Backend không nhận được tệp CSV/XLSX. Vui lòng chọn lại tệp.';
  return `Import thất bại (HTTP ${error.response.status}). Vui lòng thử lại.`;
}

function AccountImportPage() {
  const inputRef = useRef(null);
  const displayName = useAuthStore((state) => state.fullName || state.username) || 'Nhân viên đào tạo';
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [result, setResult] = useState(null);

  const chooseFile = (event) => {
    setFile(event.target.files?.[0] || null);
    setResult(null);
    setFeedback('');
  };

  const downloadTemplate = () => {
    const blob = new Blob(['\uFEFF', TEMPLATE], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'mau-import-tai-khoan-utf8.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const submit = async () => {
    if (!file) return setFeedback('Vui lòng chọn tệp CSV hoặc XLSX trước khi import.');
    setLoading(true);
    setFeedback('');
    setResult(null);
    try {
      const response = await importUsers(file);
      setResult(response.data);
      setFeedback(response.message);
    } catch (error) {
      setFeedback(getImportError(error));
    } finally {
      setLoading(false);
    }
  };

  return <DashboardShell role="STAFF" displayName={displayName} activePath="/staff/accounts/import" pageTitle="Tài khoản">
    <div className="workspace-page import-workspace-page">
    <section className="workspace-heading import-heading"><div><span>QUẢN LÝ TÀI KHOẢN</span><h1>Tài khoản giảng viên và sinh viên</h1><p>Nhập danh sách tài khoản đào tạo từ tệp dữ liệu chuẩn.</p></div></section>
    <section className="import-guide"><div><h2>Import danh sách tài khoản</h2>
      <p>Tệp cần có đúng 5 cột: <code>username</code>, <code>email</code>, <code>fullName</code>, <code>password</code>, <code>role</code>.</p>
      <p>Vai trò hợp lệ là <strong>STUDENT</strong> hoặc <strong>LECTURER</strong>. Dung lượng tối đa 5 MB.</p></div>
      <button type="button" className="academic-button secondary" onClick={downloadTemplate}><Download size={17} /> Tải CSV mẫu</button>
    </section>
    <section className="academic-panel import-panel">
      <input ref={inputRef} type="file" accept=".csv,.xlsx" onChange={chooseFile} hidden />
      <button type="button" className="import-dropzone" onClick={() => inputRef.current?.click()}>
        <FileSpreadsheet size={42} /><strong>{file ? file.name : 'Chọn tệp danh sách tài khoản'}</strong><span>Hỗ trợ định dạng .csv và .xlsx</span>
      </button>
      <div className="import-actions"><button type="button" className="academic-button" disabled={loading || !file} onClick={submit}>
        <Upload size={17} /> {loading ? 'Đang import…' : 'Bắt đầu import'}
      </button></div>
    </section>
    {feedback && <div className={`academic-feedback ${!result ? 'error' : ''}`}>{feedback}</div>}
    {result && <section className="import-result">
      <div className="import-summary"><article><span>Tổng số dòng</span><strong>{result.totalRows}</strong></article>
        <article className="success"><span>Đã tạo</span><strong>{result.importedCount}</strong></article>
        <article className="failed"><span>Không hợp lệ</span><strong>{result.failedCount}</strong></article></div>
      {result.errors?.length > 0 && <div className="academic-panel"><table className="academic-table">
        <thead><tr><th>Dòng</th><th>Username</th><th>Lý do</th></tr></thead><tbody>{result.errors.map((item) =>
          <tr key={`${item.row}-${item.username}`}><td>{item.row}</td><td>{item.username || '—'}</td>
            <td className="import-error"><XCircle size={16} />{item.message}</td></tr>)}</tbody>
      </table></div>}
    </section>}
    </div>
  </DashboardShell>;
}

export default AccountImportPage;
