import { Navigate } from 'react-router-dom'
import { homePath } from '../features/auth/types'
import { useAppSelector } from '../stores/hooks'

export function HomeRedirect() {
  const user = useAppSelector((state) => state.auth.user)
  return <Navigate to={homePath(user?.role)} replace />
}
