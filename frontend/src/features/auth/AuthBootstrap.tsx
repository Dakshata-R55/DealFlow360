import { useEffect, type ReactNode } from 'react'
import { useGetCurrentUserQuery } from '../../stores/api/authApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'
import { logout, markHydrated, setCredentials } from './authSlice'

export function AuthBootstrap({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch()
  const token = useAppSelector((state) => state.auth.accessToken)
  const hydrated = useAppSelector((state) => state.auth.hydrated)
  const { data, isError, isSuccess } = useGetCurrentUserQuery(undefined, { skip: !token })

  useEffect(() => {
    if (!token) {
      dispatch(markHydrated())
    }
  }, [token, dispatch])

  useEffect(() => {
    if (!token) {
      return
    }
    if (isSuccess && data) {
      dispatch(setCredentials({ user: data, accessToken: token }))
      dispatch(markHydrated())
    }
    if (isError) {
      dispatch(logout())
      dispatch(markHydrated())
    }
  }, [token, isSuccess, isError, data, dispatch])

  if (!hydrated) {
    return (
      <div className="auth-page">
        <p className="muted">Restoring session…</p>
      </div>
    )
  }

  return children
}
