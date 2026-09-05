import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { useGetProductsQuery } from '../../stores/api/configApi'
import {
  useAddQuotationLineMutation,
  useDeleteQuotationLineMutation,
  useDismissRecommendationMutation,
  useGetQuotationQuery,
  useGetRecommendationsQuery,
  useSaveQuotationDraftMutation,
  useSubmitQuotationMutation,
  useUpdateQuotationLineMutation,
} from '../../stores/api/quotationApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { money, percent, routeLabel, type QuotationLine } from './types'

export function QuotationDetailPage() {
  const { id } = useParams()
  return <Navigate to={id ? `/quotations?quote=${id}` : '/quotations'} replace />
}

export function QuotationPanel({ quotationId }: { quotationId: number }) {
  const user = useAppSelector((state) => state.auth.user)
  const quoteQuery = useGetQuotationQuery(quotationId, { skip: !Number.isFinite(quotationId) })
  const recsQuery = useGetRecommendationsQuery(quotationId, { skip: !Number.isFinite(quotationId) })
  const products = useGetProductsQuery()
  const [addLine, addLineState] = useAddQuotationLineMutation()
  const [updateLine] = useUpdateQuotationLineMutation()
  const [deleteLine] = useDeleteQuotationLineMutation()
  const [saveDraft, saveDraftState] = useSaveQuotationDraftMutation()
  const [submitQuote, submitState] = useSubmitQuotationMutation()
  const [dismissRec] = useDismissRecommendationMutation()
  const [error, setError] = useState<string | null>(null)
  const [productId, setProductId] = useState('')
  const [variantId, setVariantId] = useState('')
  const [draftDiscount, setDraftDiscount] = useState<Record<number, string>>({})

  const quote = quoteQuery.data
  const canEdit = Boolean(quote) && quote?.status === 'DRAFT' && user?.role === 'SALES_REP'

  useEffect(() => {
    if (!quote) {
      return
    }
    const next: Record<number, string> = {}
    for (const line of quote.lines) {
      next[line.id] = String(line.discountPercent)
    }
    setDraftDiscount(next)
  }, [quote])

  async function onAddLine(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canEdit) {
      return
    }
    setError(null)
    try {
      await addLine({
        quotationId,
        body: {
          productId: Number(productId),
          variantId: variantId ? Number(variantId) : null,
          quantity: 1,
        },
      }).unwrap()
      setProductId('')
      setVariantId('')
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add product'))
    }
  }

  async function changeQty(line: QuotationLine, delta: number) {
    const quantity = line.quantity + delta
    if (!canEdit || quantity <= 0) {
      return
    }
    setError(null)
    try {
      await updateLine({ quotationId, lineId: line.id, body: { quantity } }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not update quantity'))
    }
  }

  async function commitDiscount(line: QuotationLine) {
    if (!canEdit) {
      return
    }
    const raw = draftDiscount[line.id]
    const discountPercent = Number(raw)
    if (!Number.isFinite(discountPercent) || discountPercent === line.discountPercent) {
      return
    }
    setError(null)
    try {
      await updateLine({ quotationId, lineId: line.id, body: { discountPercent } }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not update discount'))
    }
  }

  async function onRemove(lineId: number) {
    if (!canEdit) {
      return
    }
    setError(null)
    try {
      await deleteLine({ quotationId, lineId }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not remove line'))
    }
  }

  async function onSaveDraft() {
    setError(null)
    try {
      await saveDraft(quotationId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save draft'))
    }
  }

  async function onSubmit() {
    setError(null)
    try {
      await submitQuote(quotationId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not submit quotation'))
    }
  }

  async function onAddRecommendation(productIdToAdd: number) {
    if (!canEdit) {
      return
    }
    setError(null)
    try {
      await addLine({
        quotationId,
        body: { productId: productIdToAdd, quantity: 1 },
      }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add recommendation'))
    }
  }

  async function onDismiss(productIdToDismiss: number) {
    if (!canEdit) {
      return
    }
    setError(null)
    try {
      await dismissRec({ quotationId, productId: productIdToDismiss }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not dismiss recommendation'))
    }
  }

  const selectedProduct = (products.data ?? []).find((product) => product.id === Number(productId))

  if (!Number.isFinite(quotationId)) {
    return <p className="error">Invalid quotation.</p>
  }
  if (quoteQuery.isLoading) {
    return <p className="muted">Loading quotation…</p>
  }
  if (quoteQuery.isError || !quote) {
    return <p className="error">Could not load quotation.</p>
  }

  return (
    <div className="stack">
      <Panel title={`${quote.quoteNumber} · ${quote.status.replaceAll('_', ' ')}`}>
        <dl className="facts">
          <div>
            <dt>Customer</dt>
            <dd>{quote.customerName}</dd>
          </div>
          <div>
            <dt>Tier</dt>
            <dd>{quote.customerTierName}</dd>
          </div>
          <div>
            <dt>Price list</dt>
            <dd>{quote.priceListName}</dd>
          </div>
          {quote.sourceRequestNumber ? (
            <div>
              <dt>From request</dt>
              <dd>{quote.sourceRequestNumber}</dd>
            </div>
          ) : null}
          {quote.customerExpectedDiscountPercent != null ? (
            <div>
              <dt>Overall expected discount</dt>
              <dd>{percent(quote.customerExpectedDiscountPercent)}</dd>
            </div>
          ) : null}
          {quote.customerTargetBudget != null ? (
            <div>
              <dt>Target budget</dt>
              <dd>₹{money(quote.customerTargetBudget)}</dd>
            </div>
          ) : null}
        </dl>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}

        {quote.lines.map((line) => {
          const over = line.excess > 0
          return (
            <article key={line.id} className={over ? 'quote-line quote-line-over' : 'quote-line'}>
              <h3>
                {line.productName}
                {line.variantLabel ? <span className="muted"> · {line.variantLabel}</span> : null}
              </h3>
              <p className="muted">
                ₹{money(line.resolvedUnitPrice)} each · {line.billingType}
              </p>
              <div className="line-controls">
                <div className="qty-stepper">
                  <button
                    className="button"
                    type="button"
                    disabled={!canEdit || line.quantity <= 1}
                    onClick={() => void changeQty(line, -1)}
                  >
                    -
                  </button>
                  <span>{line.quantity}</span>
                  <button
                    className="button"
                    type="button"
                    disabled={!canEdit}
                    onClick={() => void changeQty(line, 1)}
                  >
                    +
                  </button>
                </div>
                <label className="field">
                  Discount %
                  <input
                    className="input"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    disabled={!canEdit}
                    value={draftDiscount[line.id] ?? String(line.discountPercent)}
                    onChange={(event) =>
                      setDraftDiscount((current) => ({ ...current, [line.id]: event.target.value }))
                    }
                    onBlur={() => void commitDiscount(line)}
                  />
                </label>
              </div>
              <p>
                Allowed {line.allowedDiscountPercent}%{' '}
                {over ? (
                  <span className="error">
                    ⚠ {line.excess}% above allowed limit
                  </span>
                ) : (
                  <span className="ok-text">✓</span>
                )}
              </p>
              {line.customerExpectedDiscountPercent != null ? (
                line.customerExpectedIsDefault ? (
                  <p className="expected-default">
                    By default {percent(line.customerExpectedDiscountPercent)} from the customer request
                  </p>
                ) : (
                  <p className="expected-independent">
                    Independent expected {percent(line.customerExpectedDiscountPercent)}
                  </p>
                )
              ) : null}
              <p>
                Line total ₹{money(line.lineTotal)} · Margin ₹{money(line.marginAmount)} (
                {line.marginPercent}%)
              </p>
              {canEdit ? (
                <button className="link" type="button" onClick={() => void onRemove(line.id)}>
                  Remove
                </button>
              ) : null}
            </article>
          )
        })}

        {canEdit ? (
          <form className="form" onSubmit={onAddLine}>
            <label className="field">
              Add product
              <select
                className="input"
                value={productId}
                onChange={(event) => {
                  setProductId(event.target.value)
                  setVariantId('')
                }}
                required
              >
                <option value="">Select product</option>
                {(products.data ?? [])
                  .filter((product) => product.active)
                  .map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
              </select>
            </label>
            {selectedProduct && selectedProduct.variants.length > 0 ? (
              <label className="field">
                Variant
                <select
                  className="input"
                  value={variantId}
                  onChange={(event) => setVariantId(event.target.value)}
                >
                  <option value="">None</option>
                  {selectedProduct.variants.map((variant) => (
                    <option key={variant.id} value={variant.id}>
                      {variant.attributeName} {variant.attributeValue} (+{variant.extraPrice})
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            <div className="form-actions">
              <button className="button" type="submit" disabled={addLineState.isLoading || !productId}>
                {addLineState.isLoading ? 'Adding…' : 'Add to quote'}
              </button>
            </div>
          </form>
        ) : null}
      </Panel>

      <Panel title="Quote summary">
        <dl className="facts">
          <div>
            <dt>Subtotal</dt>
            <dd>₹{money(quote.subtotal)}</dd>
          </div>
          <div>
            <dt>Discount</dt>
            <dd>₹{money(quote.discountAmount)}</dd>
          </div>
          {quote.customerExpectedDiscountPercent != null ? (
            <div>
              <dt>Customer overall expected</dt>
              <dd>{percent(quote.customerExpectedDiscountPercent)}</dd>
            </div>
          ) : null}
          <div>
            <dt>Total</dt>
            <dd>₹{money(quote.totalAmount)}</dd>
          </div>
          <div>
            <dt>Margin</dt>
            <dd>
              ₹{money(quote.marginAmount)} ({quote.marginPercent}%)
            </dd>
          </div>
          <div>
            <dt>Max line excess</dt>
            <dd>{percent(quote.maxLineExcess)}</dd>
          </div>
          <div>
            <dt>Quote-wide excess</dt>
            <dd>{percent(quote.riskScore)} of catalog</dd>
          </div>
          <div>
            <dt>Likely approval</dt>
            <dd>{routeLabel(quote.likelyRoute)}</dd>
          </div>
        </dl>
        {canEdit ? (
          <div className="form-actions">
            <button className="button" type="button" disabled={saveDraftState.isLoading} onClick={() => void onSaveDraft()}>
              {saveDraftState.isLoading ? 'Saving…' : 'Save Draft'}
            </button>
            <button
              className="button"
              type="button"
              disabled={submitState.isLoading || quote.lines.length === 0}
              onClick={() => void onSubmit()}
            >
              {submitState.isLoading ? 'Submitting…' : 'Submit for Approval'}

            </button>
          </div>
        ) : null}
      </Panel>

      <Panel title="Recommended">
        {recsQuery.isLoading ? <p className="muted">Loading suggestions…</p> : null}
        {(recsQuery.data ?? []).length === 0 ? <p className="muted">No suggestions right now.</p> : null}
        <div className="recs">
          {(recsQuery.data ?? []).map((rec) => (
            <article key={rec.productId} className="rec-card">
              <h3>{rec.productName}</h3>
              <p>+₹{money(rec.marginDelta)} margin</p>
              {rec.promotion ? <p className="muted">Promotion</p> : null}
              {canEdit ? (
                <div className="form-actions">
                  <button className="button" type="button" onClick={() => void onAddRecommendation(rec.productId)}>
                    Add to Quote
                  </button>
                  <button className="link" type="button" onClick={() => void onDismiss(rec.productId)}>
                    Dismiss
                  </button>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      </Panel>
    </div>
  )
}
