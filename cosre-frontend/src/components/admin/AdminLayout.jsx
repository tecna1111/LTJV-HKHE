import { Bell, ChevronRight, LayoutDashboard, LogOut, Search, Settings, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import BrandLogo from '../BrandLogo';
import useAuthStore from '../../store/useAuthStore';
import './AdminLayout.css';

const adminTabs = [
  { id: 'overview', label: 'Tổng quan', path: '/dashboard' },
  { id: 'accounts', label: 'Tài khoản', path: '/admin/users' },
  { id: 'reports', label: 'Báo cáo sự cố', path: '/admin/reports' },
];

function initials(value = '') {
  return value.trim().split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase() || 'CS';
}

function AdminLayout({ activeTab, title, children }) {
  const username = useAuthStore((state) => state.username);
  const fullName = useAuthStore((state) => state.fullName);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();
  const displayName = fullName || username || 'Quản trị viên';

  const logout = () => {
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <main className="cosre-admin-shell">
      <aside className="cosre-admin-sidebar">
        <div className="cosre-admin-brand"><BrandLogo /></div>
        <div className="cosre-admin-role">
          <span><ShieldCheck size={17} /></span>
          <div><strong>Quản trị viên</strong><small>System Administrator</small></div>
        </div>
        <nav className="cosre-admin-side-nav" aria-label="Điều hướng quản trị">
          <small>MENU</small>
          <button className="active" onClick={() => navigate('/dashboard')}>
            <LayoutDashboard size={18} />
            <span>Admin Dashboard</span>
          </button>
        </nav>
        <div className="cosre-admin-sidebar-footer">
          <button type="button"><Settings size={17} /><span>Cài đặt</span></button>
          <button type="button" onClick={logout}><LogOut size={17} /><span>Đăng xuất</span></button>
        </div>
      </aside>

      <section className="cosre-admin-main">
        <header className="cosre-admin-topbar">
          <div className="cosre-admin-breadcrumb">
            <ShieldCheck size={15} />
            <span>Quản trị viên</span>
            <ChevronRight size={14} />
            <strong>{title}</strong>
          </div>
          <div className="cosre-admin-actions">
            <button type="button" aria-label="Tìm kiếm"><Search size={18} /></button>
            <button type="button" className="cosre-admin-notification" aria-label="Thông báo"><Bell size={18} /><i /></button>
            <div className="cosre-admin-profile">
              <span>{initials(displayName)}</span>
              <div><strong>{displayName}</strong><small>Quản trị viên</small></div>
            </div>
          </div>
        </header>

        <div className="cosre-admin-tabs-header">
          <h1>Admin</h1>
          <nav aria-label="Chức năng quản trị">
            {adminTabs.map((tab) => (
              <button
                type="button"
                className={activeTab === tab.id ? 'active' : ''}
                aria-current={activeTab === tab.id ? 'page' : undefined}
                key={tab.id}
                onClick={() => navigate(tab.path)}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="cosre-admin-page">{children}</div>
      </section>
    </main>
  );
}

export default AdminLayout;
