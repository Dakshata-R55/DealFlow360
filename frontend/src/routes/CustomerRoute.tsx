import { Navigate, Outlet } from 'react-router-dom'
import { isCustomerUser } from '../features/auth/types'
import { useAppSelector } from '../stores/hooks'

export function CustomerRoute() {
  const user = useAppSelector((state) => state.auth.user)

  if (!isCustomerUser(user?.role)) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
