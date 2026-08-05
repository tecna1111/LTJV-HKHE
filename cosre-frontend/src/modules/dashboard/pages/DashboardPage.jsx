import { useEffect, useMemo, useState } from 'react';
import { Activity, ShieldCheck, UserCheck, UserMinus, Users } from 'lucide-react';
import AdminLayout from '../../../components/admin/AdminLayout';
import useAuthStore from '../../../store/useAuthStore';
import { getUsers } from '../../account/accountService';
import { getCurrentUser } from '../dashboardService';
import './DashboardPage.css';

const roleLabels = {
  ADMIN: 'Quản trị viên',
  HEAD_DEPT: 'Trưởng bộ môn',
  STAFF: 'Cán bộ đào tạo',
  LECTURER: 'Giảng viên',
  STUDENT: 'Sinh viên',
};

const roleOrder = ['HEAD_DEPT', 'STAFF', 'LECTURER', 'STUDENT', 'ADMIN'];

function DashboardPage() {
  const storedRole = useAuthStore((state) => state.role);
  const [profile, setProfile] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getCurrentUser()
      .then(async (result) => {
        if (!result.success) throw new Error(result.message);
        setProfile(result.data);
        if (result.data.role === 'ADMIN') {
          const accountResult = await getUsers();
          if (accountResult.success) setUsers(accountResult.data || []);
        }
      })
      .catch(() => setError('Không thể tải dữ liệu quản trị. Vui lòng kiểm tra kết nối và thử lại.'))
      .finally(() => setLoading(false));
  }, []);

  const role = profile?.role || storedRole;
  const activeUsers = users.filter((user) => user.active).length;
  const inactiveUsers = users.length - activeUsers;
  const managedUsers = users.filter((user) => user.role !== 'ADMIN').length;
  const roleStats = useMemo(() => roleOrder.map((item) => ({
    role: item,
    label: roleLabels[item],
    count: users.filter((user) => user.role === item).length,
  })), [users]);

  const metrics = [
    { label: 'Tổng tài khoản', value: users.length, note: 'Tất cả tài khoản trong hệ thống', icon: Users, tone: 'blue' },
    { label: 'Đang hoạt động', value: activeUsers, note: 'Có thể đăng nhập và sử dụng COSRE', icon: UserCheck, tone: 'violet' },
    { label: 'Đã vô hiệu hóa', value: inactiveUsers, note: 'Không thể đăng nhập hệ thống', icon: UserMinus, tone: 'red' },
    { label: 'Tài khoản được quản lý', value: managedUsers, note: 'Head Department, Staff, Lecturer, Student', icon: ShieldCheck, tone: 'green' },
  ];

  if (role && role !== 'ADMIN') {
    return <main className="dashboard-state"><p>Dashboard dành cho vai trò {roleLabels[role] || role} đang được phát triển.</p></main>;
  }

  return (
    <AdminLayout activeTab="overview" title="Admin Dashboard">
      <section className="admin-overview-heading">
        <div><span>COSRE SYSTEM</span><h2>Tổng quan hệ thống</h2><p>Theo dõi tài khoản và trạng thái vận hành của nền tảng CollabSphere.</p></div>
        <div className="admin-health"><i /><span><strong>System operational</strong><small>JWT authentication active</small></span></div>
      </section>

      {error && <div className="dashboard-error">{error}</div>}

      <section className="admin-metric-grid" aria-label="Thống kê tài khoản">
        {metrics.map(({ label, value, note, icon: Icon, tone }) => (
          <article className={`admin-metric-card admin-metric-card--${tone}`} key={label}>
            <div className="admin-metric-icon"><Icon size={20} /></div>
            <Activity size={14} className="admin-metric-trend" />
            <strong>{loading ? '—' : value}</strong>
            <h3>{label}</h3>
            <p>{note}</p>
          </article>
        ))}
      </section>

      <section className="admin-dashboard-grid">
        <article className="admin-panel role-distribution">
          <header><div><h3>Phân bổ tài khoản theo vai trò</h3><p>Dữ liệu đồng bộ trực tiếp từ Account API</p></div></header>
          <div className="role-stat-list">
            {roleStats.map((item) => {
              const percent = users.length ? Math.round((item.count / users.length) * 100) : 0;
              return (
                <div className="role-stat" key={item.role}>
                  <div><strong>{item.label}</strong><span>{item.count} tài khoản</span></div>
                  <div className="role-stat-track"><i style={{ width: `${percent}%` }} /></div>
                  <b>{percent}%</b>
                </div>
              );
            })}
          </div>
        </article>

      </section>
    </AdminLayout>
  );
}

export default DashboardPage;
