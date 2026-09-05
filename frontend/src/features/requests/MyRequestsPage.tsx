import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { useGetCustomerRequestsQuery } from '../../stores/api/quoteRequestApi'
import { rupee } from './types'

export function MyRequestsPage() {
  const requests = useGetCustomerRequestsQuery()

  return (
    <div className="stack">
      <Panel title="My Requests">
        {requests.isLoading ? <p className="muted">Loading requests…</p> : null}
        {requests.isError ? <p className="error">Could not load requests.</p> : null}
        {(requests.data ?? []).length === 0 ? <p className="muted">No requests yet.</p> : null}
        {(requests.data ?? []).map((request) => (
          <Link className="quote-card" key={request.id} to={`/customer/requests/${request.id}`}>
            <p className="quote-card-status">{request.statusLabel}</p>
            <p className="quote-card-customer">{request.requestNumber}</p>
            <p className="quote-card-meta">{request.sellerCompanyName}</p>
            <p className="quote-card-meta">
              {request.lines.map((line) => `${line.quantity} ${line.productName}`).join(' · ')}
            </p>
            <p className="quote-card-amount">MRP {rupee(request.catalogMrpTotal)}</p>
          </Link>
        ))}
      </Panel>
    </div>
  )
}
