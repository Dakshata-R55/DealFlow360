import { Navigate, Outlet } from 'react-router-dom'
import { canAccessQuotations } from '../features/auth/types'
import { useAppSelector } from '../stores/hooks'

export function SalesWorkspaceRoute() {
  const user = useAppSelector((state) => state.auth.user)

  if (!canAccessQuotations(user?.role)) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
