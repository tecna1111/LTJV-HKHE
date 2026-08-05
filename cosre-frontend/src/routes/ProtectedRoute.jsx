import { Navigate } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';

function ProtectedRoute({ children, requiredRole }) {
  const token = useAuthStore((state) => state.token);
  const role = useAuthStore((state) => state.role);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && role !== requiredRole) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default ProtectedRoute;
