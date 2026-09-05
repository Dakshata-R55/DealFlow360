import { Link } from 'react-router-dom'
import { Panel } from '../components/common/Panel'
import { canAccessQuotations } from '../features/auth/types'
import { HomePage } from './HomePage'
import { useAppSelector } from '../stores/hooks'

export function DashboardPage() {
  const user = useAppSelector((state) => state.auth.user)
  const showQuotations = canAccessQuotations(user?.role)

  return (
    <div className="stack">
      {showQuotations ? (
        <Panel title="Sales workspace">
          <p className="muted">Create and edit quotations for your customers.</p>
          <p>
            <Link className="link" to="/quotations">
              Open quotations
            </Link>
          </p>
        </Panel>
      ) : null}
      <Panel title="Signed in">
        {user ? (
          <dl className="facts">
            <div>
              <dt>User ID</dt>
              <dd>{user.id}</dd>
            </div>
            <div>
              <dt>Name</dt>
              <dd>{user.name}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Role</dt>
              <dd>{user.role}</dd>
            </div>
            <div>
              <dt>Company ID</dt>
              <dd>{user.companyId}</dd>
            </div>
            <div>
              <dt>Company</dt>
              <dd>{user.companyName}</dd>
            </div>
          </dl>
        ) : (
          <p className="muted">Identity is loading…</p>
        )}
      </Panel>
      <HomePage />
    </div>
  )
}
