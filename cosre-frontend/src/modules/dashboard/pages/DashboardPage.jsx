import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Activity, ArrowRight, BarChart3, Bell, BookOpen, Building2, CircleAlert,
  ClipboardCheck, FileText, FileUp, FolderKanban, GraduationCap, LayoutDashboard,
  LogOut, MessageSquare, Search, Settings, ShieldCheck, Sparkles, UserCheck,
  UserMinus, Users, Video,
} from 'lucide-react';
import BrandLogo from '../../../components/BrandLogo';
import useAuthStore from '../../../store/useAuthStore';
import { getUsers } from '../../account/accountService';
import './DashboardPage.css';
import './DashboardRoleExtensions.css';

const ROLE_META = {
  ADMIN: { label: 'Quản trị viên', sub: 'System Administrator', icon: ShieldCheck, tone: 'red' },
  HEAD_DEPT: { label: 'Trưởng bộ môn', sub: 'Department Workspace', icon: Building2, tone: 'violet' },
  STAFF: { label: 'Nhân viên đào tạo', sub: 'Academic Staff Workspace', icon: ClipboardCheck, tone: 'green' },
  LECTURER: { label: 'Giảng viên', sub: 'Lecturer Workspace', icon: GraduationCap, tone: 'violet' },
  STUDENT: { label: 'Sinh viên', sub: 'Student Workspace', icon: BookOpen, tone: 'blue' },
};

const ROLE_NAV = {
  ADMIN: [
    ['Tổng quan', LayoutDashboard], ['Tài khoản', Users, '/admin/users'], ['Báo cáo sự cố', CircleAlert, '/admin/reports'],
  ],
  HEAD_DEPT: [
    ['Tổng quan', LayoutDashboard], ['Dự án chờ duyệt', ClipboardCheck], ['Dự án đã duyệt', FolderKanban],
    ['Lớp học', BookOpen], ['Phân công dự án', Users], ['Báo cáo', BarChart3],
  ],
  STAFF: [
    ['Tổng quan', LayoutDashboard], ['Môn học & đề cương', BookOpen], ['Lớp học', Building2],
    ['Tài khoản', Users], ['Nhập dữ liệu', FileUp], ['Báo cáo nhập liệu', FileText],
  ],
  LECTURER: [
    ['Tổng quan', LayoutDashboard], ['Lớp học', BookOpen], ['Dự án', FolderKanban], ['Nhóm sinh viên', Users],
    ['Đánh giá', ClipboardCheck], ['Lịch họp', Video],
  ],
  STUDENT: [
    ['Tổng quan', LayoutDashboard], ['Lớp học của tôi', BookOpen], ['Workspace nhóm', FolderKanban],
    ['Nhiệm vụ', ClipboardCheck], ['Tài nguyên', GraduationCap], ['Tin nhắn', MessageSquare],
  ],
};

const ROLE_MODULES = {
  HEAD_DEPT: [
    ['Dự án chờ duyệt', 'Các đề xuất của giảng viên sẽ xuất hiện tại đây.', ClipboardCheck],
    ['Phân công dự án', 'Phân công dự án đã duyệt cho các lớp thuộc bộ môn.', FolderKanban],
    ['Lớp học', 'Theo dõi danh sách lớp và thông tin giảng dạy.', BookOpen],
  ],
  STAFF: [
    ['Môn học & đề cương', 'Quản lý dữ liệu môn học và đề cương đào tạo.', BookOpen],
    ['Lớp học & thành viên', 'Tạo lớp và phân công giảng viên, sinh viên.', Building2],
    ['Nhập dữ liệu', 'Nhập tài khoản và dữ liệu học vụ từ tệp chuẩn.', FileUp],
  ],
  LECTURER: [
    ['Lớp học phụ trách', 'Các lớp được phân công sẽ xuất hiện tại đây.', BookOpen],
    ['Dự án & nhóm', 'Quản lý dự án, nhóm sinh viên và cột mốc.', FolderKanban],
    ['Đánh giá', 'Bài nộp và yêu cầu đánh giá sẽ xuất hiện tại đây.', ClipboardCheck],
  ],
  STUDENT: [
    ['Lớp học của tôi', 'Các lớp được phân công sẽ xuất hiện tại đây.', BookOpen],
    ['Workspace nhóm', 'Không gian dự án sẽ sẵn sàng khi bạn được xếp nhóm.', FolderKanban],
    ['Nhiệm vụ', 'Nhiệm vụ được giao sẽ xuất hiện tại đây.', ClipboardCheck],
  ],
};

const ROLE_DESCRIPTIONS = {
  ADMIN: 'Theo dõi sức khỏe và hoạt động của toàn bộ nền tảng.',
  HEAD_DEPT: 'Phê duyệt đề xuất và điều phối dự án trong bộ môn.',
  STAFF: 'Quản lý dữ liệu học vụ, lớp học và tài khoản đào tạo.',
  LECTURER: 'Theo dõi lớp học và hỗ trợ các nhóm sinh viên.',
  STUDENT: 'Cộng tác cùng đội nhóm và theo dõi công việc học tập.',
};

function initials(value = '') {
  return value.trim().split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase() || 'CS';
}

function DashboardShell({ role, displayName, children }) {
  const navigate = useNavigate();
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const meta = ROLE_META[role] || ROLE_META.STUDENT;
  const RoleIcon = meta.icon;
  const logout = () => { clearAuth(); navigate('/login', { replace: true }); };

  return <main className={`workspace-shell workspace-shell--${meta.tone}`}>
    <aside className="workspace-sidebar">
      <div className="workspace-brand"><BrandLogo /></div>
      <div className="workspace-role"><span><RoleIcon size={18} /></span><div><strong>{meta.label}</strong><small>{meta.sub}</small></div></div>
      <nav className="workspace-nav" aria-label={`Menu ${meta.label}`}><small>KHÔNG GIAN LÀM VIỆC</small>
        {(ROLE_NAV[role] || ROLE_NAV.STUDENT).map(([label, Icon, path], index) => <button type="button" className={index === 0 ? 'active' : ''} key={label} onClick={() => path && navigate(path)}><Icon size={18} /><span>{label}</span></button>)}
      </nav>
      <div className="workspace-side-foot"><button type="button"><Settings size={18} /><span>Cài đặt</span></button><button type="button" onClick={logout}><LogOut size={18} /><span>Đăng xuất</span></button></div>
    </aside>
    <section className="workspace-main">
      <header className="workspace-topbar"><div className="workspace-crumb"><LayoutDashboard size={15} /><span>Workspace</span><ArrowRight size={14} /><strong>Tổng quan</strong></div>
        <div className="workspace-actions"><label><Search size={17} /><input aria-label="Tìm kiếm" placeholder="Tìm kiếm nhanh..." /></label><button type="button" aria-label="Thông báo"><Bell size={18} /></button><div className="workspace-profile"><span>{initials(displayName)}</span><div><strong>{displayName}</strong><small>{meta.label}</small></div></div></div>
      </header>{children}
    </section>
  </main>;
}

function MetricGrid({ items, loading }) {
  return <section className="role-metric-grid">{items.map(([value, label, note, Icon, tone]) => <article className={`role-metric role-metric--${tone}`} key={label}><span><Icon size={20} /></span><small><Activity size={13} /> API</small><strong>{loading ? '—' : value}</strong><h3>{label}</h3><p>{note}</p></article>)}</section>;
}

function AdminDashboard({ users, loading, error }) {
  const active = users.filter((user) => user.active).length;
  const metrics = [
    [users.length, 'Tổng tài khoản', 'Tài khoản hiện có trong hệ thống', Users, 'blue'],
    [active, 'Đang hoạt động', 'Tài khoản có quyền truy cập', UserCheck, 'green'],
    [users.length - active, 'Đã vô hiệu hóa', 'Tài khoản bị hạn chế', UserMinus, 'red'],
    [users.filter((user) => user.role !== 'ADMIN').length, 'Tài khoản được quản lý', 'Không bao gồm quản trị viên', ShieldCheck, 'violet'],
  ];
  const labels = { HEAD_DEPT: 'Trưởng bộ môn', STAFF: 'Nhân viên đào tạo', LECTURER: 'Giảng viên', STUDENT: 'Sinh viên' };
  return <><MetricGrid items={metrics} loading={loading} /><article className="role-panel admin-real-data"><header><div><h3>Phân bổ tài khoản theo vai trò</h3><p>Dữ liệu trực tiếp từ Account API</p></div></header>
    {error ? <EmptyState icon={CircleAlert} title="Không thể tải dữ liệu" description={error} /> : users.length === 0 && !loading ? <EmptyState icon={Users} title="Chưa có tài khoản" description="Tài khoản mới sẽ xuất hiện tại đây sau khi được tạo hoặc nhập vào hệ thống." /> : <div className="distribution-list">{Object.entries(labels).map(([role, label]) => { const count = users.filter((user) => user.role === role).length; const percent = users.length ? Math.round(count / users.length * 100) : 0; return <div key={role}><span><strong>{label}</strong><small>{count} tài khoản</small></span><i><b style={{ width: `${percent}%` }} /></i><em>{percent}%</em></div>; })}</div>}
  </article></>;
}

function EmptyState({ icon: Icon, title, description }) {
  return <div className="dashboard-empty"><span><Icon size={22} /></span><h3>{title}</h3><p>{description}</p></div>;
}

function RoleDashboard({ role }) {
  return <><section className="module-card-grid">{(ROLE_MODULES[role] || []).map(([title, description, Icon]) => <article key={title}><span><Icon size={20} /></span><h3>{title}</h3><p>{description}</p><button type="button">Mở module <ArrowRight size={14} /></button></article>)}</section>
    <article className="role-panel role-empty-panel"><header><div><h3>Dữ liệu tổng quan</h3><p>Sẽ được đồng bộ từ module nghiệp vụ</p></div></header><EmptyState icon={BarChart3} title="Chưa có dữ liệu" description="Dashboard sẽ tự động cập nhật khi hệ thống có dữ liệu thực tế. Không sử dụng dữ liệu minh họa." /></article>
  </>;
}

function DashboardPage() {
  const role = useAuthStore((state) => state.role) || 'STUDENT';
  const displayName = useAuthStore((state) => state.fullName || state.username) || ROLE_META[role]?.label || 'COSRE User';
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(role === 'ADMIN');
  const [error, setError] = useState('');

  useEffect(() => {
    if (role !== 'ADMIN') return;
    getUsers().then((result) => { if (!result.success) throw new Error(result.message); setUsers(result.data || []); }).catch(() => setError('Vui lòng kiểm tra kết nối backend và thử lại.')).finally(() => setLoading(false));
  }, [role]);

  const greeting = useMemo(() => new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: 'long' }).format(new Date()), []);

  return <DashboardShell role={role} displayName={displayName}><div className="workspace-page">
    <section className="workspace-heading"><div><span>{greeting}</span><h1>Chào mừng trở lại, {displayName.split(' ').slice(-1)[0]}!</h1><p>{ROLE_DESCRIPTIONS[role] || ROLE_DESCRIPTIONS.STUDENT}</p></div><button><Sparkles size={16} /> Hỏi COSRE AI</button></section>
    {role === 'ADMIN' ? <AdminDashboard users={users} loading={loading} error={error} /> : <RoleDashboard role={role} />}
  </div></DashboardShell>;
}

export default DashboardPage;
