import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { StatusBadge, toneForTicket } from '../../components/ui/StatusBadge'
import {
  useCancelCustomerRequestMutation,
  useDeleteCustomerRequestLineMutation,
  useGetCustomerRequestQuery,
  usePatchCustomerRequestMutation,
  useSubmitCustomerRequestMutation,
  useUpdateCustomerRequestLineMutation,
  useWithdrawCustomerRequestMutation,
} from '../../stores/api/quoteRequestApi'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, rupee, type QuoteRequestLine } from './types'

export function CustomerRequestDetailPage() {
  const { id } = useParams()
  return <Navigate to={id ? `/customer/requests?request=${id}` : '/customer/requests'} replace />
}

export function CustomerRequestPanel({
  requestId,
  onOpenQuotation,
  onRevoked,
}: {
  requestId: number
  onOpenQuotation: (quotationId: number) => void
  onRevoked: () => void
}) {
  const query = useGetCustomerRequestQuery(requestId, { skip: !Number.isFinite(requestId) })
  const [patchRequest] = usePatchCustomerRequestMutation()
  const [submitRequest, submitState] = useSubmitCustomerRequestMutation()
  const [withdrawRequest, withdrawState] = useWithdrawCustomerRequestMutation()
  const [cancelRequest, cancelState] = useCancelCustomerRequestMutation()
  const [updateLine] = useUpdateCustomerRequestLineMutation()
  const [deleteLine] = useDeleteCustomerRequestLineMutation()
  const [delivery, setDelivery] = useState('')
  const [overallExpected, setOverallExpected] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const request = query.data
  const canEdit = request?.status === 'DRAFT'
  const canRevoke =
    request != null &&
    request.quotationId == null &&
    (request.status === 'DRAFT' || request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW')
  const canWithdraw =
    request != null &&
    request.quotationId == null &&
    (request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW')

  useEffect(() => {
    if (!request) {
      return
    }
    setDelivery(request.requestedDeliveryDate ?? '')
    setOverallExpected(request.expectedDiscountPercent == null ? '' : String(request.expectedDiscountPercent))
    setNotes(request.notes)
  }, [request])

  function headerBody() {
    return {
      requestedDeliveryDate: delivery || null,
      expectedDiscountPercent: overallExpected ? Number(overallExpected) : null,
      notes,
    }
  }

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canEdit) {
      return
    }
    setError(null)
    try {
      await patchRequest({ id: requestId, body: headerBody() }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save request'))
    }
  }

  async function onSubmit() {
    setError(null)
    try {
      await patchRequest({ id: requestId, body: headerBody() }).unwrap()
      await submitRequest(requestId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not submit request'))
    }
  }

  async function onWithdraw() {
    setError(null)
    try {
      await withdrawRequest(requestId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not pull request back'))
    }
  }

  async function onRevoke() {
    setError(null)
    try {
      await cancelRequest(requestId).unwrap()
      onRevoked()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not revoke request'))
    }
  }

  if (!Number.isFinite(requestId)) {
    return <p className="error">Invalid request.</p>
  }
  if (query.isLoading) {
    return <p className="muted">Loading request…</p>
  }
  if (query.isError || !request) {
    return <p className="error">Could not load request.</p>
  }

  return (
    <Panel
      title={request.requestNumber}
      badge={
        <StatusBadge
          label={request.statusLabel}
          tone={toneForTicket(request.status, request.quotationStatus)}
        />
      }
    >
      <p>Seller: {request.sellerCompanyName}</p>
      <p className="muted">{request.customerTierName} standing</p>
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}
      {canWithdraw || canRevoke ? (
        <div className="ticket-controls">
          {canWithdraw ? (
            <button className="button" type="button" disabled={withdrawState.isLoading} onClick={() => void onWithdraw()}>
              {withdrawState.isLoading ? 'Converting…' : 'Convert to Draft'}
            </button>
          ) : null}
          {canRevoke ? (
            <button className="link" type="button" disabled={cancelState.isLoading} onClick={() => void onRevoke()}>
              {cancelState.isLoading ? 'Revoking…' : 'Revoke proposal'}
            </button>
          ) : null}
        </div>
      ) : null}
      {request.lines.map((line) => (
        <RequestLine
          key={line.id}
          line={line}
          overallExpected={request.expectedDiscountPercent}
          canEdit={canEdit}
          onMinus={() =>
            void updateLine({
              requestId,
              lineId: line.id,
              body: { quantity: Math.max(1, line.quantity - 1) },
            })
          }
          onPlus={() =>
            void updateLine({
              requestId,
              lineId: line.id,
              body: { quantity: line.quantity + 1 },
            })
          }
          onExpected={(expectedDiscountPercent) =>
            void updateLine({
              requestId,
              lineId: line.id,
              body: { expectedDiscountPercent },
            })
          }
          onRemove={() => void deleteLine({ requestId, lineId: line.id })}
        />
      ))}
      <form className="form" onSubmit={(event) => void onSave(event)}>
        <label className="field">
          Overall expected discount %
          <span className="muted">
            Ask for a discount on the whole bill. The totals below update after you leave this field. Raise a product
            if that item needs more than the overall rate.
          </span>
          <input
            className="input"
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={overallExpected}
            onChange={(event) => setOverallExpected(event.target.value)}
            onBlur={() => {
              if (!canEdit) {
                return
              }
              void patchRequest({ id: requestId, body: headerBody() })
                .unwrap()
                .catch((err) => setError(apiErrorMessage(err, 'Could not save request')))
            }}
            disabled={!canEdit}
            placeholder="Optional"
          />
        </label>
      </form>
      <div className="request-totals">
        <div>
          <span>Catalog MRP</span>
          <strong>{rupee(request.catalogMrpTotal)}</strong>
        </div>
        <div>
          <span>After available discounts</span>
          <strong>{rupee(request.indicativeTotal)}</strong>
        </div>
        <div className="indicative">
          <span>After your expected discounts</span>
          <strong>{rupee(request.expectedTotal)}</strong>
        </div>
      </div>
      <form className="form" onSubmit={(event) => void onSave(event)}>
        <label className="field">
          Requested delivery date
          <input
            className="input"
            type="date"
            value={delivery}
            onChange={(event) => setDelivery(event.target.value)}
            disabled={!canEdit}
          />
        </label>
        <label className="field">
          Notes
          <textarea
            className="input"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            disabled={!canEdit}
          />
        </label>
        {canEdit ? (
          <div className="form-actions">
            <button className="button" type="submit">
              Save Draft
            </button>
            <button
              className="button"
              type="button"
              disabled={submitState.isLoading || request.lines.length === 0}
              onClick={() => void onSubmit()}
            >
              {submitState.isLoading ? 'Submitting…' : 'Submit Request'}
            </button>
          </div>
        ) : null}
        {request.status === 'QUOTED' &&
        (request.quotationStatus === 'DRAFT' || request.quotationStatus === 'PENDING_APPROVAL') ? (
          <p className="muted">
            Seller is working this
            {request.quotationStatus === 'PENDING_APPROVAL' ? ' — Pending Approval' : ' as a draft'}. It stays an
            active request until they send you an offer.
          </p>
        ) : null}
        {request.quotationId &&
        (request.quotationStatus === 'NEGOTIATION' ||
          request.quotationStatus === 'APPROVED' ||
          request.quotationStatus === 'CONFIRMED') ? (
          <button
            className="link"
            type="button"
            onClick={() => {
              if (request.quotationId) {
                onOpenQuotation(request.quotationId)
              }
            }}
          >
            Open quotation
          </button>
        ) : null}
      </form>
    </Panel>
  )
}

function RequestLine({
  line,
  overallExpected,
  canEdit,
  onMinus,
  onPlus,
  onExpected,
  onRemove,
}: {
  line: QuoteRequestLine
  overallExpected: number | null
  canEdit: boolean
  onMinus: () => void
  onPlus: () => void
  onExpected: (expectedDiscountPercent: number) => void
  onRemove: () => void
}) {
  const billing = line.billingType === 'RECURRING' ? ' / month' : ''
  const [draftExpected, setDraftExpected] = useState(String(line.expectedDiscountPercent))

  useEffect(() => {
    setDraftExpected(String(line.expectedDiscountPercent))
  }, [line.expectedDiscountPercent])

  function commitExpected() {
    const next = Number(draftExpected)
    if (!Number.isFinite(next) || next === line.expectedDiscountPercent) {
      setDraftExpected(String(line.expectedDiscountPercent))
      return
    }
    onExpected(next)
  }

  return (
    <div className="quote-line">
      <h3>{line.productName}</h3>
      <p className="muted">
        {line.categoryName} · {line.unit}
        {line.billingType === 'RECURRING' ? ' · recurring' : ''}
      </p>
      <p>
        MRP {rupee(line.mrp)}
        {billing} each · Product MRP {rupee(line.lineMrp)}
      </p>
      <p className="ok-text">Available by default: {percentLabel(line.availableDiscountPercent)}</p>
      <p className="muted">
        {line.categoryName} allows up to {percentLabel(line.categoryDiscountPercent)}; your standing allows{' '}
        {percentLabel(line.standingDiscountPercent)}. The seller can go up to the lower of those two without extra
        approval.
      </p>
      <label className="field">
        Expected discount %
        <span className="muted">Starts at the available rate. Raise it if you need more on this product.</span>
        <input
          className="input"
          type="number"
          min="0"
          max="100"
          step="0.01"
          disabled={!canEdit}
          value={draftExpected}
          onChange={(event) => setDraftExpected(event.target.value)}
          onBlur={commitExpected}
        />
      </label>
      {line.independentExpected ? (
        <p className="expected-independent">Independent expected {percentLabel(line.expectedDiscountPercent)}</p>
      ) : overallExpected != null ? (
        <p className="expected-default">By default uses overall {percentLabel(overallExpected)}</p>
      ) : (
        <p className="expected-default">By default {percentLabel(line.availableDiscountPercent)}</p>
      )}
      <p>
        After expected {rupee(line.expectedUnitPrice)}
        {billing} each · Product {rupee(line.expectedLineTotal)}
      </p>
      <p>Qty: {line.quantity}</p>
      {canEdit ? (
        <div className="form-actions">
          <button className="button" type="button" onClick={onMinus}>
            -
          </button>
          <button className="button" type="button" onClick={onPlus}>
            +
          </button>
          <button className="link" type="button" onClick={onRemove}>
            Remove
          </button>
        </div>
      ) : null}
    </div>
  )
}
