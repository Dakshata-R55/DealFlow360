import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import {
  useGetCustomerRequestQuery,
  usePatchCustomerRequestMutation,
  useSubmitCustomerRequestMutation,
  useUpdateCustomerRequestLineMutation,
  useDeleteCustomerRequestLineMutation,
} from '../../stores/api/quoteRequestApi'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, rupee, type QuoteRequestLine } from './types'

export function CustomerRequestDetailPage() {
  const { id } = useParams()
  const requestId = Number(id)
  const query = useGetCustomerRequestQuery(requestId, { skip: !Number.isFinite(requestId) })
  const [patchRequest] = usePatchCustomerRequestMutation()
  const [submitRequest, submitState] = useSubmitCustomerRequestMutation()
  const [updateLine] = useUpdateCustomerRequestLineMutation()
  const [deleteLine] = useDeleteCustomerRequestLineMutation()
  const [delivery, setDelivery] = useState('')
  const [budget, setBudget] = useState('')
  const [overallExpected, setOverallExpected] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const request = query.data
  const canEdit = request?.status === 'DRAFT'

  useEffect(() => {
    if (!request) {
      return
    }
    setDelivery(request.requestedDeliveryDate ?? '')
    setBudget(request.targetBudget == null ? '' : String(request.targetBudget))
    setOverallExpected(request.expectedDiscountPercent == null ? '' : String(request.expectedDiscountPercent))
    setNotes(request.notes)
  }, [request])

  function headerBody() {
    return {
      requestedDeliveryDate: delivery || null,
      targetBudget: budget ? Number(budget) : null,
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
    <div className="stack">
      <p>
        <Link className="link" to="/customer/requests">
          ← My Requests
        </Link>
      </p>
      <Panel title={`Request a Quote · ${request.requestNumber}`}>
        <p>Seller: {request.sellerCompanyName}</p>
        <p className="muted">
          {request.statusLabel} · {request.customerTierName} standing
        </p>
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
            Overall expected discount %
            <span className="muted">
              Ask for a discount on the whole bill. Products still at the default available rate will use this when
              the seller opens the draft. Set a higher % on a product if that item needs more.
            </span>
            <input
              className="input"
              type="number"
              min="0"
              max="100"
              step="0.01"
              value={overallExpected}
              onChange={(event) => setOverallExpected(event.target.value)}
              disabled={!canEdit}
              placeholder="Optional"
            />
          </label>
          <label className="field">
            Target budget
            <span className="muted">
              Optional rupee amount. MRP is {rupee(request.catalogMrpTotal)}. After your expected discounts the bill
              is {rupee(request.expectedTotal)}.
            </span>
            <input
              className="input"
              type="number"
              min="0"
              value={budget}
              onChange={(event) => setBudget(event.target.value)}
              disabled={!canEdit}
              placeholder={String(Math.round(request.expectedTotal))}
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
          {error ? (
            <p className="error" role="alert">
              {error}
            </p>
          ) : null}
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
          {request.status === 'QUOTED' ? (
            <p className="muted">Quotation is ready. Customer quotation view comes in a later slice.</p>
          ) : null}
        </form>
      </Panel>
    </div>
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
        {billing} each · Line MRP {rupee(line.lineMrp)}
      </p>
      <p className="ok-text">Available by default: {percentLabel(line.availableDiscountPercent)}</p>
      <p className="muted">
        {line.categoryName} allows up to {percentLabel(line.categoryDiscountPercent)}; your standing allows{' '}
        {percentLabel(line.standingDiscountPercent)}. The seller can go up to the lower of those two without extra
        approval.
      </p>
      <label className="field">
        Expected discount %
        <span className="muted">
          Starts at the available rate. Raise it if you need more on this product.
        </span>
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
        <p className="expected-default">
          By default uses overall {percentLabel(overallExpected)}
        </p>
      ) : (
        <p className="expected-default">By default {percentLabel(line.availableDiscountPercent)}</p>
      )}
      <p>
        After expected {rupee(line.expectedUnitPrice)}
        {billing} each · Line {rupee(line.expectedLineTotal)}
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
