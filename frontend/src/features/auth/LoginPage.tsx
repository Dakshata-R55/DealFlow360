import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useLoginMutation } from '../../stores/api/authApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { setCredentials } from './authSlice'
import { AuthShell } from './AuthShell'
import { homePath, isCompanyUser, isCustomerUser } from './types'

function EnvelopeIcon() {
  return (
    <svg className="auth-field-icon" viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden>
      <rect x="2.5" y="4.5" width="15" height="11" rx="1.5" stroke="currentColor" strokeWidth="1.4" />
      <path d="M3 5.5 10 11l7-5.5" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
    </svg>
  )
}

function LockIcon() {
  return (
    <svg className="auth-field-icon" viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden>
      <rect x="4.5" y="9" width="11" height="7.5" rx="1.5" stroke="currentColor" strokeWidth="1.4" />
      <path d="M7 9V7.2a3 3 0 0 1 6 0V9" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  )
}

function EyeIcon({ open }: { open: boolean }) {
  return open ? (
    <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden>
      <path
        d="M2.5 10s2.8-5.5 7.5-5.5S17.5 10 17.5 10 14.7 15.5 10 15.5 2.5 10 2.5 10Z"
        stroke="currentColor"
        strokeWidth="1.4"
      />
      <circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  ) : (
    <svg viewBox="0 0 20 20" width="18" height="18" fill="none" aria-hidden>
      <path d="M3 3.5 17 16.5" stroke="currentColor" strokeWidth="1.4" />
      <path
        d="M8.2 5.1A8.6 8.6 0 0 1 10 4.5c4.7 0 7.5 5.5 7.5 5.5a13 13 0 0 1-2.4 3.1M5.3 6.8A13 13 0 0 0 2.5 10S5.3 15.5 10 15.5c1 0 1.9-.2 2.7-.6"
        stroke="currentColor"
        strokeWidth="1.4"
      />
    </svg>
  )
}

export function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  const [mode, setMode] = useState<'company' | 'customer'>('company')
  const [email, setEmail] = useState('sales@acme.demo')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
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
    <AuthShell title="Log in to your account" lede="Welcome back">
      <div className="auth-tabs" role="group" aria-label="Log in as">
        <button
          className="auth-tab"
          type="button"
          aria-pressed={mode === 'company'}
          onClick={() => switchMode('company')}
        >
          Company
        </button>
        <button
          className="auth-tab"
          type="button"
          aria-pressed={mode === 'customer'}
          onClick={() => switchMode('customer')}
        >
          Customer
        </button>
      </div>
      <form className="form auth-form" onSubmit={onSubmit}>
        <label className="field" htmlFor="login-email">
          Email Address
          <span className="auth-input-wrap">
            <EnvelopeIcon />
            <input
              id="login-email"
              className="input"
              type="email"
              name="email"
              autoComplete="username"
              placeholder="sales@acme.demo"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </span>
        </label>
        <label className="field" htmlFor="login-password">
          Password
          <span className="auth-input-wrap">
            <LockIcon />
            <input
              id="login-password"
              className="input"
              type={showPassword ? 'text' : 'password'}
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
            <button
              className="password-toggle"
              type="button"
              onClick={() => setShowPassword((open) => !open)}
              aria-pressed={showPassword}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              <EyeIcon open={showPassword} />
            </button>
          </span>
        </label>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
        <button className="button auth-submit" type="submit" disabled={isLoading}>
          {isLoading ? 'Signing in…' : 'Log in'}
        </button>
        <p className="auth-signup-line">
          Don&apos;t have an account?{' '}
          {mode === 'customer' ? (
            <Link className="link" to="/signup/customer">
              Sign Up
            </Link>
          ) : (
            <Link className="link" to="/signup">
              Sign Up
            </Link>
          )}
        </p>
      </form>
    </AuthShell>
  )
}
