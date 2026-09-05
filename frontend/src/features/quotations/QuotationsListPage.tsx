import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { canWriteQuotations } from '../../features/auth/types'
import { SellerRequestPanel } from '../requests/SellerRequestDetailPage'
import { rupee } from '../requests/types'
import { useGetSellerRequestsQuery } from '../../stores/api/quoteRequestApi'
import {
  useCreateQuotationMutation,
  useGetCustomersQuery,
  useGetQuotationsQuery,
} from '../../stores/api/quotationApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { QuotationPanel } from './QuotationDetailPage'
import { money, PIPELINE_COLUMNS } from './types'

export function QuotationsListPage() {
  const user = useAppSelector((state) => state.auth.user)
  const canCreate = canWriteQuotations(user?.role)
  const quotations = useGetQuotationsQuery()
  const requests = useGetSellerRequestsQuery()
  const customers = useGetCustomersQuery(undefined, { skip: !canCreate })
  const [createQuotation, createState] = useCreateQuotationMutation()
  const [customerId, setCustomerId] = useState('')
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [params, setParams] = useSearchParams()
  const quoteId = Number(params.get('quote'))
  const requestId = Number(params.get('request'))
  const openQuote = Number.isFinite(quoteId) && quoteId > 0 ? quoteId : null
  const openRequest = Number.isFinite(requestId) && requestId > 0 ? requestId : null

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setParams({})
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [setParams])

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const created = await createQuotation({ customerId: Number(customerId) }).unwrap()
      setCustomerId('')
      setParams({ quote: String(created.id) })
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create quotation'))
    }
  }

  const rows = quotations.data ?? []
  const query = search.trim().toLowerCase()
  const filteredQuotes = query
    ? rows.filter(
        (quote) =>
          quote.quoteNumber.toLowerCase().includes(query) ||
          quote.customerName.toLowerCase().includes(query),
      )
    : rows
  const todoRequests = (requests.data ?? []).filter((request) => {
    if (request.quotationId != null) {
      return false
    }
    if (request.status !== 'SUBMITTED' && request.status !== 'UNDER_REVIEW') {
      return false
    }
    if (!query) {
      return true
    }
    return (
      request.requestNumber.toLowerCase().includes(query) ||
      request.customerName.toLowerCase().includes(query)
    )
  })

  return (
    <div className="stack">
      {canCreate ? (
        <Panel title="Board">
          <form className="form" onSubmit={onCreate}>
            <label className="field">
              Customer
              <select
                className="input"
                value={customerId}
                onChange={(event) => setCustomerId(event.target.value)}
                required
              >
                <option value="">Select customer</option>
                {(customers.data ?? [])
                  .filter((customer) => customer.active)
                  .map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {customer.name}
                    </option>
                  ))}
              </select>
            </label>
            {error ? (
              <p className="error" role="alert">
                {error}
              </p>
            ) : null}
            <div className="form-actions">
              <button className="button" type="submit" disabled={createState.isLoading || !customerId}>
                {createState.isLoading ? 'Creating…' : '+ New quotation'}
              </button>
            </div>
          </form>
        </Panel>
      ) : (
        <Panel title="Board">
          <p className="muted">Incoming requests and quotations stay on this board. Open a card to work it.</p>
        </Panel>
      )}

      <Panel title="Pipeline">
        <label className="field">
          Search
          <input
            className="input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Quote number, request, or customer"
          />
        </label>
        {quotations.isLoading || requests.isLoading ? <p className="muted">Loading board…</p> : null}
        {quotations.isError ? <p className="error">Could not load quotations.</p> : null}
        {requests.isError ? <p className="error">Could not load requests.</p> : null}
        <div className="kanban">
          <section className="kanban-column">
            <h3>
              To do
              <span className="muted"> {todoRequests.length}</span>
            </h3>
            {todoRequests.map((request) => (
              <BoardCard
                key={`request-${request.id}`}
                selected={openRequest === request.id}
                status="Incoming request"
                title={request.customerName}
                meta={request.requestNumber}
                amount={rupee(request.catalogMrpTotal)}
                onOpen={() => setParams({ request: String(request.id) })}
              />
            ))}
          </section>
          {PIPELINE_COLUMNS.map((column) => {
            const cards = filteredQuotes.filter((quote) => quote.status === column.status)
            return (
              <section key={column.status} className="kanban-column">
                <h3>
                  {column.label}
                  <span className="muted"> {cards.length}</span>
                </h3>
                {cards.map((quote) => (
                  <BoardCard
                    key={`quote-${quote.id}`}
                    selected={openQuote === quote.id}
                    status={quote.status.replaceAll('_', ' ')}
                    title={quote.customerName}
                    meta={quote.quoteNumber}
                    amount={`₹${money(quote.totalAmount)}`}
                    onOpen={() => setParams({ quote: String(quote.id) })}
                  />
                ))}
              </section>
            )
          })}
        </div>
      </Panel>

      {openRequest != null || openQuote != null ? (
        <div className="drawer-backdrop" onClick={() => setParams({})}>
          <aside className="drawer" onClick={(event) => event.stopPropagation()}>
            <div className="drawer-head">
              <button className="link" type="button" onClick={() => setParams({})}>
                Close
              </button>
            </div>
            {openRequest != null ? (
              <SellerRequestPanel
                requestId={openRequest}
                onOpenQuotation={(id) => setParams({ quote: String(id) })}
              />
            ) : null}
            {openQuote != null ? <QuotationPanel quotationId={openQuote} /> : null}
          </aside>
        </div>
      ) : null}
    </div>
  )
}

function BoardCard({
  selected,
  status,
  title,
  meta,
  amount,
  onOpen,
}: {
  selected: boolean
  status: string
  title: string
  meta: string
  amount: string
  onOpen: () => void
}) {
  return (
    <button className={selected ? 'quote-card quote-card-selected' : 'quote-card'} type="button" onClick={onOpen}>
      <p className="quote-card-status">{status}</p>
      <p className="quote-card-customer">{title}</p>
      <p className="quote-card-meta">{meta}</p>
      <p className="quote-card-amount">{amount}</p>
    </button>
  )
}
