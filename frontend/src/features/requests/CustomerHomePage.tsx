import { Link } from 'react-router-dom'
import { useAppSelector } from '../../stores/hooks'
import { useGetCustomerRequestsQuery, useGetCustomerStandingQuery } from '../../stores/api/quoteRequestApi'
import { homeBucket, rupee } from './types'

export function CustomerHomePage() {
  const user = useAppSelector((state) => state.auth.user)
  const requests = useGetCustomerRequestsQuery()
  const standing = useGetCustomerStandingQuery()
  const rows = requests.data ?? []
  const activeCount = rows.filter((row) => homeBucket(row) === 'active').length
  const awaitingCount = rows.filter((row) => homeBucket(row) === 'awaiting').length
  const ordersCount = rows.filter((row) => homeBucket(row) === 'orders').length

  return (
    <div className="stack">
      <section className="panel">
        <h2>Welcome, {user?.name ?? 'customer'}</h2>
        <p className="muted">Browse seller companies and send a request for a quotation.</p>
        <p>
          <Link className="button" to="/customer/companies">
            Browse Companies
          </Link>
        </p>
      </section>
      {(standing.data ?? []).length > 0 ? (
        <section className="panel">
          <div className="panel-head">
            <h2>Your standing</h2>
          </div>
          <ul>
            {(standing.data ?? []).map((row) => (
              <li key={row.sellerCompanyId}>
                <strong>{row.sellerCompanyName}</strong>
                {' · '}
                {row.standingName}
                {' · '}
                {rupee(row.spend)} in {row.windowMonths} months
                {row.nextStanding && row.amountToNext != null
                  ? ` · ${rupee(row.amountToNext)} more to ${row.nextStanding}`
                  : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      <div className="metric-grid">
        <Link className="metric-card" to="/customer/requests">
          <p className="metric-label">Active requests</p>
          <p className="metric-value">{requests.isLoading ? '…' : activeCount}</p>
        </Link>
        <Link className="metric-card" to="/customer/quotations">
          <p className="metric-label">Awaiting response</p>
          <p className="metric-value">{requests.isLoading ? '…' : awaitingCount}</p>
        </Link>
        <Link className="metric-card" to="/customer/orders">
          <p className="metric-label">Orders in progress</p>
          <p className="metric-value">{requests.isLoading ? '…' : ordersCount}</p>
        </Link>
        <div className="metric-card">
          <p className="metric-label">Outstanding invoices</p>
          <p className="metric-value">0</p>
        </div>
      </div>
    </div>
  )
}
