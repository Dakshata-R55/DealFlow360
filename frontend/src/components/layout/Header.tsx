import { useNavigate } from 'react-router-dom'
import { APP_SUBTITLE, APP_TITLE } from '../../constants/app'
import { logout } from '../../features/auth/authSlice'
import { baseApi } from '../../stores/api/baseApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'

export function Header() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)

  function onLogout() {
    dispatch(logout())
    dispatch(baseApi.util.resetApiState())
    navigate('/login', { replace: true })
  }

  return (
    <header className="header">
      <div>
        <p className="eyebrow">Dealflow360</p>
        <h1>{APP_TITLE}</h1>
        <p className="subtitle">{APP_SUBTITLE}</p>
      </div>
      {isAuthenticated ? (
        <div className="header-actions">
          {user ? (
            <p className="auth-meta">
              {user.email}
              <span className="muted"> · {user.role}</span>
            </p>
          ) : null}
          <button className="button" type="button" onClick={onLogout}>
            Logout
          </button>
        </div>
      ) : null}
    </header>
  )
}
