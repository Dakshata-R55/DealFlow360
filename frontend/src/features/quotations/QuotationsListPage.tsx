import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { canWriteQuotations } from '../../features/auth/types'
import {
  useCreateQuotationMutation,
  useGetCustomersQuery,
  useGetQuotationsQuery,
} from '../../stores/api/quotationApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { money, PIPELINE_COLUMNS, type Quotation } from './types'

export function QuotationsListPage() {
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const canCreate = canWriteQuotations(user?.role)
  const quotations = useGetQuotationsQuery()
  const customers = useGetCustomersQuery(undefined, { skip: !canCreate })
  const [createQuotation, createState] = useCreateQuotationMutation()
  const [customerId, setCustomerId] = useState('')
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const created = await createQuotation({ customerId: Number(customerId) }).unwrap()
      navigate(`/quotations/${created.id}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create quotation'))
    }
  }

  const rows = quotations.data ?? []
  const query = search.trim().toLowerCase()
  const filtered = query
    ? rows.filter(
        (quote) =>
          quote.quoteNumber.toLowerCase().includes(query) ||
          quote.customerName.toLowerCase().includes(query),
      )
    : rows

  return (
    <div className="stack">
      {canCreate ? (
        <Panel title="New quotation">
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
                {createState.isLoading ? 'Creating…' : '+ New Quotation'}
              </button>
            </div>
          </form>
        </Panel>
      ) : null}

      <Panel title="Quotations">
        <label className="field">
          Search
          <input
            className="input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Quote number or customer"
          />
        </label>
        {quotations.isLoading ? <p className="muted">Loading quotations…</p> : null}
        {quotations.isError ? <p className="error">Could not load quotations.</p> : null}
        <div className="kanban">
          {PIPELINE_COLUMNS.map((column) => (
            <section key={column.status} className="kanban-column">
              <h3>
                {column.label}
                <span className="muted">
                  {' '}
                  {filtered.filter((quote) => quote.status === column.status).length}
                </span>
              </h3>
              {filtered
                .filter((quote) => quote.status === column.status)
                .map((quote) => (
                  <QuoteCard key={quote.id} quote={quote} />
                ))}
            </section>
          ))}
        </div>
      </Panel>
    </div>
  )
}

function QuoteCard({ quote }: { quote: Quotation }) {
  return (
    <Link className="quote-card" to={`/quotations/${quote.id}`}>
      <p className="quote-card-status">{quote.status.replaceAll('_', ' ')}</p>
      <p className="quote-card-customer">{quote.customerName}</p>
      <p className="quote-card-meta">{quote.quoteNumber}</p>
      <p className="quote-card-amount">₹{money(quote.totalAmount)}</p>
    </Link>
  )
}
