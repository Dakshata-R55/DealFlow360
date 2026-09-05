import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useLoginMutation } from '../../stores/api/authApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { setCredentials } from './authSlice'
import { AuthShell } from './AuthShell'
import { homePath, isCompanyUser, isCustomerUser } from './types'

export function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  const [mode, setMode] = useState<'company' | 'customer'>('company')
  const [email, setEmail] = useState('sales@acme.demo')
  const [password, setPassword] = useState('')
  const [login, { isLoading }] = useLoginMutation()
  const [error, setError] = useState<string | null>(null)

  if (isAuthenticated) {
    return <Navigate to={homePath(user?.role)} replace />
  }

  function switchMode(next: 'company' | 'customer') {
    setMode(next)
    setEmail(next === 'customer' ? 'customer@example.com' : 'sales@acme.demo')
    setError(null)
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const session = await login({ email, password }).unwrap()
      if (mode === 'customer' && !isCustomerUser(session.user.role)) {
        setError('This account belongs to a company. Switch to Company to log in.')
        return
      }
      if (mode === 'company' && !isCompanyUser(session.user.role)) {
        setError('This account is a customer. Switch to Customer to log in.')
        return
      }
      dispatch(setCredentials({ user: session.user, accessToken: session.accessToken }))
      navigate(homePath(session.user.role), { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Login failed'))
    }
  }

  return (
    <AuthShell title="Log in">
      <div className="view-toggle auth-toggle" role="group" aria-label="Log in as">
        <button className={mode === 'company' ? 'active' : ''} type="button" onClick={() => switchMode('company')}>
          Company
        </button>
        <button className={mode === 'customer' ? 'active' : ''} type="button" onClick={() => switchMode('customer')}>
          Customer
        </button>
      </div>
      <form className="form" onSubmit={onSubmit}>
        <label className="field">
          Email
          <input
            className="input"
            type="email"
            name="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>
        <label className="field">
          Password
          <input
            className="input"
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
        <div className="form-actions">
          <button className="button" type="submit" disabled={isLoading}>
            {isLoading ? 'Signing in…' : 'Log in'}
          </button>
          {mode === 'customer' ? (
            <Link className="link" to="/signup/customer">
              Create a customer account
            </Link>
          ) : (
            <Link className="link" to="/signup">
              Create a company
            </Link>
          )}
        </div>
      </form>
    </AuthShell>
  )
}
