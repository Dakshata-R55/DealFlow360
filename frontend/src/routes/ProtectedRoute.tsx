import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../stores/hooks'

export function ProtectedRoute() {
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  const hydrated = useAppSelector((state) => state.auth.hydrated)

  if (!hydrated) {
    return (
      <div className="shell">
        <p className="muted">Restoring session…</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
