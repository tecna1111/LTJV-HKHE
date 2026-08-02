import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../modules/authentication/pages/LoginPage';
import UserManagementPage from '../modules/account/pages/UserManagementPage';
import ProtectedRoute from './ProtectedRoute';
import DashboardPage from '../modules/dashboard/pages/DashboardPage';

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <UserManagementPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;
