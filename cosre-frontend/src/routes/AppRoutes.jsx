import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../modules/authentication/pages/LoginPage';
import UserManagementPage from '../modules/account/pages/UserManagementPage';
import ProtectedRoute from './ProtectedRoute';
import DashboardPage from '../modules/dashboard/pages/DashboardPage';
import SystemReportsPage from '../modules/report/pages/SystemReportsPage';
import TeamManagementPage from '../modules/team/pages/TeamManagementPage';
import CreateProjectPage from '../features/lecturer/pages/CreateProjectPage';
import AccountImportPage from '../modules/account/pages/AccountImportPage';
import SubjectListPage from '../modules/subject/pages/SubjectListPage';
import ClassroomListPage from '../modules/classroom/pages/ClassroomListPage';
import ClassroomDetailPage from '../modules/classroom/pages/ClassroomDetailPage';
import ProjectWorkflowPage from '../modules/workflow/pages/ProjectWorkflowPage';
import TeamWorkspacePage from '../modules/team/pages/TeamWorkspacePage';
import StudentTeamsPage from '../modules/team/pages/StudentTeamsPage';

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="/workflow" element={<ProtectedRoute><ProjectWorkflowPage /></ProtectedRoute>} />
        <Route path="/teams/:id/workspace" element={<ProtectedRoute><TeamWorkspacePage /></ProtectedRoute>} />
        <Route path="/student/teams" element={<ProtectedRoute requiredRole="STUDENT"><StudentTeamsPage /></ProtectedRoute>} />
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
          path="/staff/subjects"
          element={<ProtectedRoute requiredRole="STAFF"><SubjectListPage /></ProtectedRoute>}
        />
        <Route
          path="/staff/classrooms"
          element={<ProtectedRoute requiredRole="STAFF"><ClassroomListPage /></ProtectedRoute>}
        />
        <Route
          path="/staff/classrooms/:id"
          element={<ProtectedRoute requiredRole="STAFF"><ClassroomDetailPage /></ProtectedRoute>}
        />
        <Route
          path="/staff/accounts/import"
          element={
            <ProtectedRoute requiredRole="STAFF">
              <AccountImportPage />
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
