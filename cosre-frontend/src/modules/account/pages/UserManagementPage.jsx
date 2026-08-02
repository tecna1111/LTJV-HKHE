import { useEffect, useState } from 'react';
import { getUsers, createUser, updateUser, deleteUser, setUserStatus } from '../accountService';
import useAuthStore from '../../../store/useAuthStore';
import { useNavigate } from 'react-router-dom';
import { getApiError } from '../../../config/axios';

const defaultForm = {
  username: '',
  email: '',
  password: '',
  fullName: '',
  role: 'STUDENT',
};

function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editingId, setEditingId] = useState(null);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();

  async function loadUsers() {
    try {
      const result = await getUsers();
      if (result.success) {
        setUsers(result.data || []);
      }
    } catch (err) {
      setError(getApiError(err, 'Không thể tải danh sách người dùng'));
    }
  }

  useEffect(() => {
    getUsers()
      .then((result) => {
        if (result.success) setUsers(result.data || []);
      })
      .catch((err) => setError(getApiError(err, 'Không thể tải danh sách người dùng')));
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    try {
      if (editingId) {
        await updateUser(editingId, {
          fullName: form.fullName,
          role: form.role,
          isActive: form.isActive,
        });
        setSuccess('User updated');
      } else {
        await createUser(form);
        setSuccess('User created');
      }
      setForm(defaultForm);
      setEditingId(null);
      loadUsers();
    } catch (err) {
      setError(getApiError(err, 'Không thể lưu người dùng'));
    }
  };

  const handleEdit = (user) => {
    setEditingId(user.id);
    setForm({
      username: user.username,
      email: user.email,
      password: '',
      fullName: user.fullName,
      role: user.role,
      isActive: user.active,
    });
  };

  const handleDelete = async (id) => {
    try {
      await deleteUser(id);
      setSuccess('User deleted');
      loadUsers();
    } catch (err) {
      setError(getApiError(err, 'Không thể xóa người dùng'));
    }
  };

  const handleStatus = async (user) => {
    try {
      await setUserStatus(user.id, !user.active);
      setSuccess('User status updated');
      loadUsers();
    } catch (err) {
      setError(getApiError(err, 'Không thể cập nhật trạng thái'));
    }
  };

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <div style={{ padding: 16 }}>
      <h1>Quản lý người dùng</h1>
      <button onClick={handleLogout}>Logout</button>
      <section style={{ marginTop: 24 }}>
        <h2>{editingId ? 'Chỉnh sửa tài khoản' : 'Tạo tài khoản mới'}</h2>
        <form onSubmit={handleSubmit}>
          {!editingId && (
            <>
              <div>
                <label>Username</label>
                <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
              </div>
              <div>
                <label>Email</label>
                <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
              </div>
              <div>
                <label>Password</label>
                <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
              </div>
            </>
          )}
          <div>
            <label>Full name</label>
            <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          </div>
          <div>
            <label>Role</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="ADMIN">ADMIN</option>
              <option value="HEAD_DEPT">HEAD_DEPT</option>
              <option value="STAFF">STAFF</option>
              <option value="LECTURER">LECTURER</option>
              <option value="STUDENT">STUDENT</option>
            </select>
          </div>
          <button type="submit">{editingId ? 'Cập nhật' : 'Tạo mới'}</button>
        </form>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        {success && <p style={{ color: 'green' }}>{success}</p>}
      </section>

      <section style={{ marginTop: 32 }}>
        <h2>Danh sách người dùng</h2>
        <table border="1" cellPadding="8" cellSpacing="0">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Full name</th>
              <th>Role</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>{user.fullName}</td>
                <td>{user.role}</td>
                <td>{user.active ? 'Yes' : 'No'}</td>
                <td>
                  <button onClick={() => handleEdit(user)}>Edit</button>
                  <button onClick={() => handleDelete(user.id)}>Delete</button>
                  <button onClick={() => handleStatus(user)}>
                    {user.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

export default UserManagementPage;
