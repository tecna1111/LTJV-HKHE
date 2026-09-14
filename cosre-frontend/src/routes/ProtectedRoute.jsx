import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';
import { getCurrentUser } from '../modules/dashboard/dashboardService';

function ProtectedRoute({ children, requiredRole }) {
  const token = useAuthStore((state) => state.token);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const setIdentity = useAuthStore((state) => state.setIdentity);
  const [status, setStatus] = useState(token ? 'checking' : 'anonymous');
  const [verifiedRole, setVerifiedRole] = useState(null);

  useEffect(() => {
    let active = true;

    if (!token) {
      return () => { active = false; };
    }

    getCurrentUser()
      .then((result) => {
        if (!active || !result.success || !result.data) throw new Error('Invalid session');
        setIdentity(result.data.username, result.data.fullName, result.data.role);
        setVerifiedRole(result.data.role);
        setStatus('authenticated');
      })
      .catch(() => {
        if (!active) return;
        clearAuth();
        setStatus('anonymous');
      });

    return () => { active = false; };
  }, [token, clearAuth, setIdentity]);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (status === 'checking') {
    return <main className="route-loading" aria-live="polite">Đang xác minh phiên đăng nhập…</main>;
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && verifiedRole !== requiredRole) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default ProtectedRoute;
