import { useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { canWriteQuotations } from '../../features/auth/types'
import {
  useConvertRequestToQuotationMutation,
  useGetSellerRequestQuery,
} from '../../stores/api/quoteRequestApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, rupee } from './types'

export function SellerRequestDetailPage() {
  const { id } = useParams()
  return <Navigate to={id ? `/quotations?request=${id}` : '/quotations'} replace />
}

export function SellerRequestPanel({
  requestId,
  onOpenQuotation,
}: {
  requestId: number
  onOpenQuotation: (quotationId: number) => void
}) {
  const user = useAppSelector((state) => state.auth.user)
  const query = useGetSellerRequestQuery(requestId, { skip: !Number.isFinite(requestId) })
  const [convert, convertState] = useConvertRequestToQuotationMutation()
  const [error, setError] = useState<string | null>(null)
  const canConvert = canWriteQuotations(user?.role)

  async function onConvert() {
    setError(null)
    try {
      const quotation = await convert(requestId).unwrap()
      onOpenQuotation(quotation.id)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create quotation'))
    }
  }

  if (!Number.isFinite(requestId)) {
    return <p className="error">Invalid request.</p>
  }
  if (query.isLoading) {
    return <p className="muted">Loading request…</p>
  }
  if (query.isError || !query.data) {
    return <p className="error">Could not load request.</p>
  }

  const request = query.data
  return (
    <div className="stack">
      <Panel title={request.requestNumber}>
        <dl className="facts">
          <div>
            <dt>Customer</dt>
            <dd>{request.customerName}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{request.statusLabel}</dd>
          </div>
          <div>
            <dt>Requested delivery</dt>
            <dd>{request.requestedDeliveryDate ?? '—'}</dd>
          </div>
          <div>
            <dt>Standing</dt>
            <dd>{request.customerTierName}</dd>
          </div>
          <div>
            <dt>Catalog MRP</dt>
            <dd>{rupee(request.catalogMrpTotal)}</dd>
          </div>
          <div>
            <dt>Indicative total</dt>
            <dd>{rupee(request.indicativeTotal)}</dd>
          </div>
          <div>
            <dt>Target budget</dt>
            <dd>{request.targetBudget == null ? '—' : rupee(request.targetBudget)}</dd>
          </div>
          <div>
            <dt>Overall expected discount</dt>
            <dd>
              {request.expectedDiscountPercent == null ? '—' : percentLabel(request.expectedDiscountPercent)}
            </dd>
          </div>
          <div>
            <dt>Expected total</dt>
            <dd>{rupee(request.expectedTotal)}</dd>
          </div>
          <div>
            <dt>Notes</dt>
            <dd>{request.notes || '—'}</dd>
          </div>
          <div>
            <dt>Submitted</dt>
            <dd>{request.submittedAt ? new Date(request.submittedAt).toLocaleString() : '—'}</dd>
          </div>
        </dl>
        <ul>
          {request.lines.map((line) => (
            <li key={line.id}>
              {line.quantity} × {line.productName} · MRP {rupee(line.lineMrp)} · available{' '}
              {percentLabel(line.availableDiscountPercent)}
              {line.independentExpected
                ? ` · independent expected ${percentLabel(line.expectedDiscountPercent)}`
                : ` · by default ${percentLabel(line.appliedExpectedPercent)}`}
              {' '}
              · expected {rupee(line.expectedLineTotal)}
            </li>
          ))}
        </ul>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
        {canConvert &&
        (request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW' || request.status === 'QUOTED') ? (
          <div className="form-actions">
            <button className="button" type="button" disabled={convertState.isLoading} onClick={() => void onConvert()}>
              {request.quotationId ? 'Open quotation' : convertState.isLoading ? 'Creating…' : 'Move to Draft'}
            </button>
          </div>
        ) : null}
      </Panel>
    </div>
  )
}
