import { BookOpen, GraduationCap, LayoutDashboard, LogOut, School } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import BrandLogo from '../BrandLogo';
import useAuthStore from '../../store/useAuthStore';
import './AcademicLayout.css';

const tabs = [
  { id: 'subjects', label: 'Môn học', path: '/staff/subjects', icon: BookOpen },
  { id: 'classrooms', label: 'Lớp học', path: '/staff/classrooms', icon: School },
];

function AcademicLayout({ activeTab, title, children }) {
  const navigate = useNavigate();
  const fullName = useAuthStore((state) => state.fullName);
  const username = useAuthStore((state) => state.username);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const logout = () => { clearAuth(); navigate('/login', { replace: true }); };

  return (
    <main className="academic-shell">
      <aside className="academic-sidebar">
        <BrandLogo />
        <div className="academic-role"><GraduationCap size={20} /><div><strong>Academic</strong><small>Cán bộ đào tạo</small></div></div>
        <nav>
          <button type="button" onClick={() => navigate('/dashboard')}><LayoutDashboard size={18} /> Tổng quan</button>
          {tabs.map(({ id, label, path, icon: Icon }) => (
            <button type="button" className={activeTab === id ? 'active' : ''} onClick={() => navigate(path)} key={id}>
              <Icon size={18} /> {label}
            </button>
          ))}
        </nav>
        <button type="button" className="academic-logout" onClick={logout}><LogOut size={18} /> Đăng xuất</button>
      </aside>
      <section className="academic-main">
        <header><div><small>COLLABSPHERE / ACADEMIC</small><h1>{title}</h1></div><span>{fullName || username}</span></header>
        <div className="academic-page">{children}</div>
      </section>
    </main>
  );
}

export default AcademicLayout;
