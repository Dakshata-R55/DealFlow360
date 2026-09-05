import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useLoginMutation } from '../../stores/api/authApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { APP_SUBTITLE, APP_TITLE } from '../../constants/app'
import { setCredentials } from './authSlice'

export function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  const [email, setEmail] = useState('sales@acme.demo')
  const [password, setPassword] = useState('')
  const [login, { isLoading }] = useLoginMutation()
  const [error, setError] = useState<string | null>(null)

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const session = await login({ email, password }).unwrap()
      dispatch(setCredentials({ user: session.user, accessToken: session.accessToken }))
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Login failed'))
    }
  }

  return (
    <div className="shell">
      <header className="header">
        <div>
          <p className="eyebrow">Dealflow360</p>
          <h1>{APP_TITLE}</h1>
          <p className="subtitle">{APP_SUBTITLE}</p>
        </div>
      </header>
      <main className="main">
        <section className="panel">
          <h2>Log in</h2>
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
              <Link className="link" to="/signup">
                Create a company
              </Link>
            </div>
          </form>
        </section>
      </main>
    </div>
  )
}
