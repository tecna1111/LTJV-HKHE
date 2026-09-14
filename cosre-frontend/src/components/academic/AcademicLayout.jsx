import useAuthStore from '../../store/useAuthStore';
import { DashboardShell } from '../../modules/dashboard/pages/DashboardPage';
import './AcademicLayout.css';
import './AcademicWorkspace.css';

const TAB_PATHS = {
  subjects: '/staff/subjects',
  classrooms: '/staff/classrooms',
  accounts: '/staff/accounts/import',
};

function AcademicLayout({ activeTab, title, children }) {
  const displayName = useAuthStore((state) => state.fullName || state.username) || 'Nhân viên đào tạo';

  return <DashboardShell
    role="STAFF"
    displayName={displayName}
    activePath={TAB_PATHS[activeTab] || '/dashboard'}
    pageTitle={title}
  >
    <div className="workspace-page academic-workspace-page">
      <section className="workspace-heading academic-workspace-heading">
        <div><span>QUẢN LÝ HỌC VỤ</span><h1>{title}</h1></div>
      </section>
      {children}
    </div>
  </DashboardShell>;
}

export default AcademicLayout;
