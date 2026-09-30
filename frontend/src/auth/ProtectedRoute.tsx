import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './authContext';

/** Layout route: renders its children when logged in, otherwise redirects to /login and remembers where to return. */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
