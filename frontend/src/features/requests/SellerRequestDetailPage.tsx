import { Navigate, useParams } from 'react-router-dom'
import { notifyError, notifyMoved } from '../../components/common/notify'
import { Panel } from '../../components/common/Panel'
import { StatusBadge, toneForTicket } from '../../components/ui/StatusBadge'
import { canClaimTodo } from '../../features/auth/types'
import {
  useConvertRequestToQuotationMutation,
  useGetSellerRequestQuery,
} from '../../stores/api/quoteRequestApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, rupee } from './types'
import { statusLabel } from '../quotations/types'

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
  const canConvert = canClaimTodo(user?.role)

  async function onConvert() {
    try {
      const quotation = await convert(requestId).unwrap()
      notifyMoved('Draft')
      onOpenQuotation(quotation.id)
    } catch (err) {
      notifyError(apiErrorMessage(err, 'Could not create quotation'))
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
            <dd>
              <StatusBadge
                label={request.quotationStatus ? statusLabel(request.quotationStatus) : 'To do'}
                tone={toneForTicket(request.status, request.quotationStatus)}
              />
            </dd>
          </div>
          <div>
            <dt>Assignee</dt>
            <dd>
              {request.quotationId
                ? 'Claimed on the quotation'
                : 'Unassigned — salesperson or manager can claim'}
            </dd>
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
        {request.quotationId ? (
          <div className="form-actions">
            <button className="button" type="button" onClick={() => onOpenQuotation(request.quotationId as number)}>
              Open quotation
            </button>
          </div>
        ) : canConvert && (request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW') ? (
          <div className="form-actions">
            <button className="button" type="button" disabled={convertState.isLoading} onClick={() => void onConvert()}>
              {convertState.isLoading ? 'Creating…' : 'Move to Draft (claim)'}
            </button>
          </div>
        ) : null}
      </Panel>
    </div>
  )
}
