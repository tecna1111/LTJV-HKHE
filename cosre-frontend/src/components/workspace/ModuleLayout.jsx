import { useLocation } from 'react-router-dom';
import useAuthStore from '../../store/useAuthStore';
import { DashboardShell } from '../../modules/dashboard/pages/DashboardPage';
import './modules.css';

export default function ModuleLayout({ title, description, children }) {
  const { role, fullName, username } = useAuthStore();
  const { pathname } = useLocation();
  const activePath = pathname === '/incidents' ? '/admin/reports' : pathname;
  return <DashboardShell role={role} displayName={fullName || username} activePath={activePath} pageTitle={title}>
    <div className="dm-content"><h1>{title}</h1><p className="dm-intro">{description}</p>{children}</div>
  </DashboardShell>;
}
