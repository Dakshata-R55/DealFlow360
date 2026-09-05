import { useEffect, useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { StatusBadge, toneForQuotationStatus } from '../../components/ui/StatusBadge'
import {
  useConfirmCustomerCreditMutation,
  useCounterCustomerQuotationMutation,
  useGetCustomerQuotationQuery,
} from '../../stores/api/quoteRequestApi'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, rupee } from './types'

export function CustomerQuotationDetailPage() {
  const { id } = useParams()
  return <Navigate to={id ? `/customer/quotations?quote=${id}` : '/customer/quotations'} replace />
}

export function CustomerQuotationPanel({ quotationId }: { quotationId: number }) {
  const query = useGetCustomerQuotationQuery(quotationId, { skip: !Number.isFinite(quotationId) })
  const [counter, counterState] = useCounterCustomerQuotationMutation()
  const [confirmCredit, confirmState] = useConfirmCustomerCreditMutation()
  const [overall, setOverall] = useState('')
  const [lineExpected, setLineExpected] = useState<Record<number, string>>({})
  const [error, setError] = useState<string | null>(null)

  const quote = query.data

  useEffect(() => {
    if (!quote) {
      return
    }
    setOverall(quote.expectedDiscountPercent == null ? '' : String(quote.expectedDiscountPercent))
    const next: Record<number, string> = {}
    for (const line of quote.lines) {
      next[line.productId] = String(line.discountPercent)
    }
    setLineExpected(next)
  }, [quote])

  async function onCounter() {
    if (!quote) {
      return
    }
    setError(null)
    try {
      await counter({
        id: quote.id,
        body: {
          expectedDiscountPercent: overall ? Number(overall) : null,
          lines: quote.lines.map((line) => ({
            productId: line.productId,
            expectedDiscountPercent: Number(lineExpected[line.productId] ?? line.discountPercent),
          })),
        },
      }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not send counter'))
    }
  }

  async function onConfirm() {
    if (!quote) {
      return
    }
    setError(null)
    try {
      await confirmCredit(quote.id).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not confirm on credit'))
    }
  }

  if (!Number.isFinite(quotationId)) {
    return <p className="error">Invalid quotation.</p>
  }
  if (query.isLoading) {
    return <p className="muted">Loading quotation…</p>
  }
  if (query.isError || !quote) {
    return <p className="error">Could not load quotation.</p>
  }

  return (
    <Panel
      title={quote.quoteNumber}
      badge={<StatusBadge label={quote.statusLabel} tone={toneForQuotationStatus(quote.status)} />}
    >
      <p className="muted">
        {quote.sellerCompanyName} · {quote.sourceRequestNumber}
      </p>
      {quote.lines.map((line) => (
        <article key={line.id} className="quote-line">
          <h3>{line.productName}</h3>
          <p>
            {rupee(line.unitPrice)} each · qty {line.quantity} · {percentLabel(line.discountPercent)} off
          </p>
          <p>Line {rupee(line.lineTotal)}</p>
          {quote.status === 'NEGOTIATION' ? (
            <label className="field">
              Counter discount %
              <input
                className="input"
                type="number"
                min="0"
                max="100"
                step="0.01"
                value={lineExpected[line.productId] ?? ''}
                onChange={(event) =>
                  setLineExpected((current) => ({ ...current, [line.productId]: event.target.value }))
                }
              />
            </label>
          ) : null}
        </article>
      ))}
      <div className="request-totals">
        <div>
          <span>Subtotal</span>
          <strong>{rupee(quote.subtotal)}</strong>
        </div>
        <div>
          <span>Discount</span>
          <strong>{rupee(quote.discountAmount)}</strong>
        </div>
        <div className="indicative">
          <span>Total</span>
          <strong>{rupee(quote.totalAmount)}</strong>
        </div>
      </div>
      {quote.status === 'NEGOTIATION' ? (
        <label className="field">
          Overall expected discount %
          <input
            className="input"
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={overall}
            onChange={(event) => setOverall(event.target.value)}
          />
        </label>
      ) : null}
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}
      {quote.status === 'NEGOTIATION' ? (
        <div className="form-actions">
          <button className="button" type="button" disabled={counterState.isLoading} onClick={() => void onCounter()}>
            {counterState.isLoading ? 'Sending…' : 'Send counter'}
          </button>
        </div>
      ) : null}
      {quote.status === 'APPROVED' ? (
        <div className="form-actions">
          <button className="button" type="button" disabled={confirmState.isLoading} onClick={() => void onConfirm()}>
            {confirmState.isLoading ? 'Confirming…' : 'Confirm on credit'}
          </button>
        </div>
      ) : null}
      {quote.status === 'CONFIRMED' ? <p className="muted">Purchased. This status is locked.</p> : null}
      {quote.status === 'PENDING_APPROVAL' || quote.status === 'DRAFT' ? (
        <p className="muted">Seller is working this. It stays in Active Requests until they send an offer.</p>
      ) : null}
    </Panel>
  )
}
