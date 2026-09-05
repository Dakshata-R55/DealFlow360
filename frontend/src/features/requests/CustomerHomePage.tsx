import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { useAppSelector } from '../../stores/hooks'
import { useGetCustomerRequestsQuery } from '../../stores/api/quoteRequestApi'

export function CustomerHomePage() {
  const user = useAppSelector((state) => state.auth.user)
  const requests = useGetCustomerRequestsQuery()
  const activeCount = (requests.data ?? []).filter(
    (row) => row.status === 'DRAFT' || row.status === 'SUBMITTED' || row.status === 'UNDER_REVIEW',
  ).length

  return (
    <div className="stack">
      <Panel title={`Welcome, ${user?.name ?? 'customer'}`}>
        <p className="muted">Browse seller companies and send a request for a quotation.</p>
        <p>
          <Link className="button" to="/customer/companies">
            Browse Companies
          </Link>
        </p>
      </Panel>
      <Panel title="Active Requests">
        {requests.isLoading ? <p className="muted">Loading…</p> : <p>{activeCount}</p>}
        <p>
          <Link className="link" to="/customer/requests">
            My Requests
          </Link>
        </p>
      </Panel>
      <Panel title="Quotations Awaiting Response">
        <p>0</p>
      </Panel>
      <Panel title="Orders In Progress">
        <p>0</p>
      </Panel>
      <Panel title="Outstanding Invoices">
        <p>0</p>
      </Panel>
    </div>
  )
}
