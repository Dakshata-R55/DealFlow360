import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../stores/hooks'

export function AdminRoute() {
  const user = useAppSelector((state) => state.auth.user)

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
