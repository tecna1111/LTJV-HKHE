import { useEffect, useMemo, useState } from 'react';
import {
  BookOpenCheck, Download, FileText, FolderKanban, LogOut, Trash2, Upload, Users,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import BrandLogo from '../../../components/BrandLogo';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { getClassrooms } from '../../classroom/classroomService';
import { getTeams } from '../../team/teamService';
import {
  getClassroomResources, getTeamResources, uploadClassroomResource, uploadTeamResource,
  deleteResource, downloadResource,
} from '../resourceService';
import './ResourceLibraryPage.css';

const TABS = [
  { id: 'classroom', label: 'Tài liệu môn học', icon: BookOpenCheck },
  { id: 'team', label: 'File bài nộp nhóm', icon: Users },
];

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function ResourceLibraryPage() {
  const navigate = useNavigate();
  const { fullName, username, clearAuth } = useAuthStore();

  const [tab, setTab] = useState('classroom');
  const [classrooms, setClassrooms] = useState([]);
  const [teams, setTeams] = useState([]);
  const [selectedClassroomId, setSelectedClassroomId] = useState('');
  const [selectedTeamId, setSelectedTeamId] = useState('');

  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', text: '' });

  const [file, setFile] = useState(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  // Tải danh sách lớp học và nhóm (của giảng viên hiện tại) khi vào trang.
  useEffect(() => {
    getClassrooms()
      .then((result) => setClassrooms(result.data || []))
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách lớp học.') }));
    getTeams()
      .then((result) => setTeams(result.data || []))
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách nhóm.') }));
  }, []);

  const selectedId = tab === 'classroom' ? selectedClassroomId : selectedTeamId;

  const loadResources = () => {
    if (!selectedId) { setResources([]); return; }
    setLoading(true);
    const request = tab === 'classroom' ? getClassroomResources(selectedId) : getTeamResources(selectedId);
    request
      .then((result) => setResources(result.data || []))
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách tài nguyên.') }))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadResources(); }, [tab, selectedClassroomId, selectedTeamId]); // eslint-disable-line react-hooks/exhaustive-deps

  const resetUploadForm = () => { setFile(null); setTitle(''); setDescription(''); };

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!selectedId) { setFeedback({ type: 'error', text: tab === 'classroom' ? 'Vui lòng chọn lớp học.' : 'Vui lòng chọn nhóm.' }); return; }
    if (!file) { setFeedback({ type: 'error', text: 'Vui lòng chọn file để tải lên.' }); return; }
    setUploading(true);
    setFeedback({ type: '', text: '' });
    try {
      if (tab === 'classroom') await uploadClassroomResource(selectedId, file, title, description);
      else await uploadTeamResource(selectedId, file, title, description);
      resetUploadForm();
      setFeedback({ type: 'success', text: 'Tải file lên thành công.' });
      loadResources();
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Tải file lên thất bại.') });
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (resource) => {
    try {
      await downloadResource(resource.id, resource.originalFileName);
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải file này.') });
    }
  };

  const handleDelete = async (resource) => {
    if (!window.confirm(`Xóa "${resource.title}"? Hành động này không thể hoàn tác.`)) return;
    try {
      await deleteResource(resource.id);
      setFeedback({ type: 'success', text: 'Đã xóa resource.' });
      loadResources();
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể xóa resource này.') });
    }
  };

  const logout = () => { clearAuth(); navigate('/login', { replace: true }); };

  const teamOptions = useMemo(() => teams, [teams]);

  return (
    <main className="resource-shell">
      <aside className="resource-sidebar">
        <BrandLogo />
        <nav>
          {TABS.map(({ id, label, icon: Icon }) => (
            <button type="button" key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}>
              <Icon size={18} /> {label}
            </button>
          ))}
        </nav>
        <button type="button" className="resource-logout" onClick={logout}><LogOut size={18} /> Đăng xuất</button>
      </aside>

      <section className="resource-main">
        <header className="resource-topbar">
          <div><small>COLLABSPHERE / RESOURCES</small><strong>{fullName || username}</strong></div>
        </header>

        <div className="resource-content">
          <div className="resource-heading">
            <h1>{tab === 'classroom' ? 'Tài liệu môn học' : 'File bài nộp nhóm'}</h1>
            <p>{tab === 'classroom'
              ? 'Tải lên và quản lý tài liệu học tập cho từng lớp học.'
              : 'Xem và quản lý file bài nộp của các nhóm sinh viên.'}</p>
          </div>

          {feedback.text && <div className={`resource-feedback ${feedback.type}`}>{feedback.text}</div>}

          <div className="resource-selector">
            {tab === 'classroom' ? (
              <select value={selectedClassroomId} onChange={(e) => setSelectedClassroomId(e.target.value)}>
                <option value="">-- Chọn lớp học --</option>
                {classrooms.map((c) => <option key={c.id} value={c.id}>{c.code} - {c.name}</option>)}
              </select>
            ) : (
              <select value={selectedTeamId} onChange={(e) => setSelectedTeamId(e.target.value)}>
                <option value="">-- Chọn nhóm --</option>
                {teamOptions.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            )}
          </div>

          {selectedId && (
            <form className="resource-upload-form" onSubmit={handleUpload}>
              <input
                type="file"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />
              <input
                type="text"
                placeholder="Tiêu đề (tùy chọn, mặc định lấy tên file)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
              <input
                type="text"
                placeholder="Mô tả ngắn (tùy chọn)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
              <button type="submit" disabled={uploading}>
                <Upload size={16} /> {uploading ? 'Đang tải lên...' : 'Tải lên'}
              </button>
            </form>
          )}

          {!selectedId && (
            <div className="resource-empty">
              <FolderKanban size={32} />
              <p>{tab === 'classroom' ? 'Chọn một lớp học để xem tài liệu.' : 'Chọn một nhóm để xem file bài nộp.'}</p>
            </div>
          )}

          {selectedId && loading && <p className="resource-loading">Đang tải danh sách...</p>}

          {selectedId && !loading && resources.length === 0 && (
            <div className="resource-empty">
              <FileText size={32} />
              <p>Chưa có file nào.</p>
            </div>
          )}

          {selectedId && !loading && resources.length > 0 && (
            <ul className="resource-list">
              {resources.map((resource) => (
                <li key={resource.id} className="resource-item">
                  <FileText size={20} className="resource-item-icon" />
                  <div className="resource-item-info">
                    <strong>{resource.title}</strong>
                    <small>
                      {resource.originalFileName} · {formatSize(resource.fileSize)} · {resource.uploadedBy?.fullName || 'Không rõ'}
                    </small>
                    {resource.description && <p>{resource.description}</p>}
                  </div>
                  <div className="resource-item-actions">
                    <button type="button" onClick={() => handleDownload(resource)} title="Tải về">
                      <Download size={16} />
                    </button>
                    <button type="button" onClick={() => handleDelete(resource)} title="Xóa" className="danger">
                      <Trash2 size={16} />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </main>
  );
}

export default ResourceLibraryPage;
