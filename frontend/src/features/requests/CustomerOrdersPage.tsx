import { useSearchParams } from 'react-router-dom'
import { Drawer } from '../../components/common/Drawer'
import { Panel } from '../../components/common/Panel'
import { StatusBadge, toneForQuotationStatus } from '../../components/ui/StatusBadge'
import { useGetCustomerQuotationsQuery } from '../../stores/api/quoteRequestApi'
import { CustomerQuotationPanel } from './CustomerQuotationDetailPage'
import { rupee } from './types'

export function CustomerOrdersPage() {
  const query = useGetCustomerQuotationsQuery()
  const [params, setParams] = useSearchParams()
  const quoteId = Number(params.get('quote'))
  const openQuote = Number.isFinite(quoteId) && quoteId > 0 ? quoteId : null
  const rows = (query.data ?? []).filter((row) => row.status === 'CONFIRMED')

  function closeDrawer() {
    const next = new URLSearchParams(params)
    next.delete('quote')
    setParams(next)
  }

  function openCard(id: number) {
    const next = new URLSearchParams(params)
    next.set('quote', String(id))
    setParams(next)
  }

  return (
    <div className="stack">
      <Panel title="Orders and Billing">
        <p className="muted">Open a row to see the confirmed offer. You stay on this page.</p>
        {query.isLoading ? <p className="muted">Loading orders…</p> : null}
        {query.isError ? <p className="error">Could not load orders.</p> : null}
        {rows.length === 0 && !query.isLoading ? <p className="muted">No confirmed orders yet.</p> : null}
        {rows.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Status</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((quote) => (
                <tr
                  key={quote.id}
                  className={openQuote === quote.id ? 'board-row board-row-selected' : 'board-row'}
                  onClick={() => openCard(quote.id)}
                >
                  <td>
                    <div className="table-stack">
                      <span className="table-primary">{quote.sellerCompanyName}</span>
                      <span className="table-secondary">{quote.quoteNumber}</span>
                    </div>
                  </td>
                  <td>
                    <StatusBadge label={quote.statusLabel} tone={toneForQuotationStatus(quote.status)} />
                  </td>
                  <td>{rupee(quote.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Panel>
      {openQuote != null ? (
        <Drawer onClose={closeDrawer}>
          <CustomerQuotationPanel quotationId={openQuote} />
        </Drawer>
      ) : null}
    </div>
  )
}
