import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../modules/authentication/pages/LoginPage';
import UserManagementPage from '../modules/account/pages/UserManagementPage';
import ProtectedRoute from './ProtectedRoute';
import DashboardPage from '../modules/dashboard/pages/DashboardPage';
import SystemReportsPage from '../modules/report/pages/SystemReportsPage';
import TeamManagementPage from '../modules/team/pages/TeamManagementPage';
import CreateProjectPage from '../features/lecturer/pages/CreateProjectPage';

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route
          path="/lecturer/projects/new"
          element={
            <ProtectedRoute requiredRole="LECTURER">
              <CreateProjectPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/teams"
          element={
            <ProtectedRoute requiredRole="LECTURER">
              <TeamManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <UserManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/reports"
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <SystemReportsPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;
