import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, ChevronRight, CircleUserRound, FolderKanban, GraduationCap, Layers3, LogOut, ShieldCheck, Users } from 'lucide-react';
import useAuthStore from '../../../store/useAuthStore';
import { getCurrentUser } from '../dashboardService';
import './DashboardPage.css';

function DashboardPage() {
  const storedName = useAuthStore((state) => state.fullName);
  const storedUsername = useAuthStore((state) => state.username);
  const storedRole = useAuthStore((state) => state.role);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    getCurrentUser()
      .then((result) => {
        if (result.success) setProfile(result.data);
        else setError(result.message || 'Không thể tải thông tin tài khoản.');
      })
      .catch(() => setError('Không thể kết nối đến máy chủ.'))
      .finally(() => setLoading(false));
  }, []);

  const logout = () => {
    clearAuth();
    navigate('/login', { replace: true });
  };

  const name = profile?.fullName || storedName || storedUsername;
  const role = profile?.role || storedRole;

  if (loading) return <main className="dashboard-state"><span className="dashboard-spinner" /><p>Đang tải workspace…</p></main>;

  return (
    <main className="dashboard-page">
      <aside className="dashboard-sidebar">
        <div className="dashboard-brand"><span><Layers3 size={21} /></span><div><strong>CollabSphere</strong><small>COSRE</small></div></div>
        <nav>
          <button className="active"><FolderKanban size={18} /> Tổng quan</button>
          {role === 'ADMIN' && <button onClick={() => navigate('/admin/users')}><Users size={18} /> Quản lý người dùng</button>}
          <button><GraduationCap size={18} /> Không gian học tập</button>
          <button><ShieldCheck size={18} /> Bảo mật tài khoản</button>
        </nav>
        <button className="sidebar-logout" onClick={logout}><LogOut size={17} /> Đăng xuất</button>
      </aside>

      <section className="dashboard-main">
        <header className="dashboard-topbar">
          <div><span>Workspace</span><ChevronRight size={13} /><strong>Dashboard</strong></div>
          <div className="dashboard-account"><button aria-label="Thông báo"><Bell size={18} /></button><span><CircleUserRound size={20} /></span><div><strong>{name}</strong><small>{role}</small></div></div>
        </header>

        <div className="dashboard-content">
          {error && <div className="dashboard-error">{error}</div>}
          <div className="welcome-card">
            <div><span className="welcome-kicker">COSRE WORKSPACE</span><h1>Chào mừng trở lại, {name}</h1><p>Bạn đã đăng nhập thành công bằng tài khoản <strong>{role}</strong>. Theo dõi hoạt động và truy cập nhanh các chức năng của hệ thống tại đây.</p></div>
            <div className="welcome-art"><Layers3 size={66} /></div>
          </div>

          <div className="dashboard-grid">
            <article><span className="metric-icon blue"><Users size={20} /></span><div><small>Vai trò hiện tại</small><strong>{role}</strong><p>Quyền truy cập đã được xác thực</p></div></article>
            <article><span className="metric-icon green"><ShieldCheck size={20} /></span><div><small>Trạng thái tài khoản</small><strong>Đang hoạt động</strong><p>JWT authentication active</p></div></article>
            <article><span className="metric-icon violet"><FolderKanban size={20} /></span><div><small>Workspace</small><strong>Sẵn sàng</strong><p>Các module tiếp theo đang phát triển</p></div></article>
          </div>

          {role === 'ADMIN' && (
            <section className="admin-action">
              <div><span><ShieldCheck size={20} /></span><div><h2>Admin Control Center</h2><p>Tạo, cập nhật, khóa và quản lý tài khoản trong hệ thống COSRE.</p></div></div>
              <button onClick={() => navigate('/admin/users')}>Mở quản lý người dùng <ChevronRight size={16} /></button>
            </section>
          )}
        </div>
      </section>
    </main>
  );
}

export default DashboardPage;
