import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Filter, Search, ShieldAlert, UserRoundCheck, Users } from 'lucide-react';
import AdminLayout from '../../../components/admin/AdminLayout';
import { getApiError } from '../../../config/axios';
import { getUsers, setUserStatus } from '../accountService';
import './UserManagementPage.css';

const roleLabels = {
  ADMIN: 'Quản trị viên',
  HEAD_DEPT: 'Trưởng bộ môn',
  STAFF: 'Cán bộ đào tạo',
  LECTURER: 'Giảng viên',
  STUDENT: 'Sinh viên',
};

const filterRoles = ['ALL', 'HEAD_DEPT', 'STAFF', 'LECTURER', 'STUDENT', 'ADMIN'];

function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [feedback, setFeedback] = useState({ type: '', text: '' });

  const loadUsers = async () => {
    try {
      const result = await getUsers();
      if (result.success) setUsers(result.data || []);
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách tài khoản.') });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getUsers()
      .then((result) => { if (result.success) setUsers(result.data || []); })
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách tài khoản.') }))
      .finally(() => setLoading(false));
  }, []);

  const shownUsers = useMemo(() => users.filter((user) => {
    const keyword = query.trim().toLowerCase();
    const matchesRole = roleFilter === 'ALL' || user.role === roleFilter;
    const matchesText = !keyword || `${user.fullName} ${user.username} ${user.email}`.toLowerCase().includes(keyword);
    return matchesRole && matchesText;
  }), [query, roleFilter, users]);

  const toggleStatus = async (user) => {
    if (user.role === 'ADMIN') {
      setFeedback({ type: 'error', text: 'Không thể vô hiệu hóa tài khoản quản trị từ màn hình này.' });
      return;
    }
    setUpdatingId(user.id);
    setFeedback({ type: '', text: '' });
    try {
      await setUserStatus(user.id, !user.active);
      setFeedback({ type: 'success', text: `Đã ${user.active ? 'vô hiệu hóa' : 'kích hoạt lại'} tài khoản ${user.username}.` });
      await loadUsers();
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể cập nhật trạng thái tài khoản.') });
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <AdminLayout activeTab="accounts" title="Quản lý tài khoản">
      <section className="accounts-page-heading">
        <div><span>ACCOUNT MANAGEMENT</span><h2>Tài khoản hệ thống</h2><p>Xem và kiểm soát quyền truy cập của người dùng CollabSphere.</p></div>
        <div className="accounts-summary"><Users size={17} /><span><strong>{users.length}</strong><small>Tổng tài khoản</small></span></div>
      </section>

      {feedback.text && (
        <div className={`accounts-feedback ${feedback.type}`} role="status">
          {feedback.type === 'success' ? <CheckCircle2 size={16} /> : <ShieldAlert size={16} />}
          {feedback.text}
        </div>
      )}

      <section className="accounts-panel">
        <div className="accounts-toolbar">
          <label className="accounts-search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm tên, username hoặc email…" /></label>
          <div className="accounts-filter-label"><Filter size={14} /> Lọc vai trò</div>
          <div className="role-filters">
            {filterRoles.map((role) => (
              <button type="button" className={roleFilter === role ? 'active' : ''} onClick={() => setRoleFilter(role)} key={role}>
                {role === 'ALL' ? 'Tất cả' : roleLabels[role]}
              </button>
            ))}
          </div>
        </div>

        <div className="accounts-table-wrap">
          <table>
            <thead><tr><th>Người dùng</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Quyền truy cập</th></tr></thead>
            <tbody>
              {shownUsers.map((user) => (
                <tr key={user.id}>
                  <td><div className="account-user"><span>{(user.fullName || user.username).slice(0, 1).toUpperCase()}</span><div><strong>{user.fullName}</strong><small>@{user.username}</small></div></div></td>
                  <td><span className="account-email">{user.email}</span></td>
                  <td><span className={`account-role role-${user.role.toLowerCase()}`}>{roleLabels[user.role]}</span></td>
                  <td><span className={`account-status ${user.active ? 'on' : 'off'}`}><i />{user.active ? 'Hoạt động' : 'Đã vô hiệu hóa'}</span></td>
                  <td>
                    <button
                      type="button"
                      className={`account-access-button ${user.active ? 'deactivate' : 'activate'}`}
                      disabled={updatingId === user.id || user.role === 'ADMIN'}
                      title={user.role === 'ADMIN' ? 'Không thể thay đổi tài khoản quản trị tại đây' : undefined}
                      onClick={() => toggleStatus(user)}
                    >
                      {updatingId === user.id ? 'Đang cập nhật…' : user.active ? 'Vô hiệu hóa' : 'Kích hoạt lại'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!loading && shownUsers.length === 0 && <div className="accounts-empty"><UserRoundCheck size={28} /><p>Không tìm thấy tài khoản phù hợp.</p></div>}
          {loading && <div className="accounts-empty"><span className="accounts-spinner" /><p>Đang tải tài khoản…</p></div>}
        </div>
      </section>

      <p className="accounts-permission-note"><ShieldAlert size={14} /> Theo đặc tả COSRE, Admin xem và vô hiệu hóa tài khoản; chức năng tạo/import và phân lớp thuộc vai trò Staff.</p>
    </AdminLayout>
  );
}

export default UserManagementPage;
