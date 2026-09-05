import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { APP_SUBTITLE, APP_TITLE } from '../../constants/app'
import { useSignupCustomerMutation } from '../../stores/api/authApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { setCredentials } from './authSlice'
import { homePath } from './types'

export function CustomerSignupPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const isAuthenticated = useAppSelector((state) => state.auth.isAuthenticated)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [signup, { isLoading }] = useSignupCustomerMutation()
  const [error, setError] = useState<string | null>(null)

  if (isAuthenticated) {
    return <Navigate to={homePath(user?.role)} replace />
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    try {
      const session = await signup({ name, email, password }).unwrap()
      dispatch(setCredentials({ user: session.user, accessToken: session.accessToken }))
      navigate(homePath(session.user.role), { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Signup failed'))
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
          <h2>Customer sign up</h2>
          <form className="form" onSubmit={onSubmit}>
            <label className="field">
              Name
              <input
                className="input"
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
              />
            </label>
            <label className="field">
              Email
              <input
                className="input"
                type="email"
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
                autoComplete="new-password"
                minLength={8}
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
                {isLoading ? 'Creating…' : 'Create account'}
              </button>
              <Link className="link" to="/login">
                Already have an account
              </Link>
            </div>
          </form>
        </section>
      </main>
    </div>
  )
}
