import { Link } from 'react-router-dom'
import { useAppSelector } from '../../stores/hooks'
import { useGetCustomerRequestsQuery } from '../../stores/api/quoteRequestApi'
import { homeBucket } from './types'

export function CustomerHomePage() {
  const user = useAppSelector((state) => state.auth.user)
  const requests = useGetCustomerRequestsQuery()
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
