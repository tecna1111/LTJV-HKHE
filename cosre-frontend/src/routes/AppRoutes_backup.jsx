import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../modules/authentication/pages/LoginPage';
import UserManagementPage from '../modules/account/pages/UserManagementPage';
import ProtectedRoute from './ProtectedRoute';
import DashboardPage from '../modules/dashboard/pages/DashboardPage';
import SystemReportsPage from '../modules/report/pages/SystemReportsPage';
import SubjectListPage from '../modules/subject/pages/SubjectListPage';
import ClassroomListPage from '../modules/classroom/pages/ClassroomListPage';
import ClassroomDetailPage from '../modules/classroom/pages/ClassroomDetailPage';

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
        <Route path="/staff/subjects" element={<ProtectedRoute requiredRole="STAFF"><SubjectListPage /></ProtectedRoute>} />
        <Route path="/staff/classrooms" element={<ProtectedRoute requiredRole="STAFF"><ClassroomListPage /></ProtectedRoute>} />
        <Route path="/staff/classrooms/:id" element={<ProtectedRoute requiredRole="STAFF"><ClassroomDetailPage /></ProtectedRoute>} />
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
