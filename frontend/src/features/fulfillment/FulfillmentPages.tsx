import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import { useGetWarehousesQuery } from '../../stores/api/configApi'
import {
  useAutoFulfillmentMutation,
  useConsolidateBackorderMutation,
  useGetFulfillmentListQuery,
  useGetFulfillmentQuery,
  useOverrideFulfillmentMutation,
} from '../../stores/api/fulfillmentApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import type { FulfillmentLine } from './types'

export function FulfillmentListPage() {
  const query = useGetFulfillmentListQuery()
  const rows = query.data ?? []

  return (
    <div className="stack">
      <Panel title="Confirmed quotes">
        <p className="muted">Split across warehouses after the customer confirms. Recurring products are skipped.</p>
        {query.isLoading ? <p className="muted">Loading fulfillment…</p> : null}
        {query.isError ? <p className="error">Could not load fulfillment.</p> : null}
        {rows.length === 0 && query.isSuccess ? <p className="muted">No confirmed quotes yet.</p> : null}
        {rows.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Quote</th>
                <th>Ships from</th>
                <th>Ship qty</th>
                <th>Backorder</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.quotationId} className="board-row">
                  <td>
                    <Link className="table-primary" to={`/fulfillment/${row.quotationId}`}>
                      {row.quoteNumber}
                    </Link>
                    <div className="table-secondary">{row.customerName}</div>
                  </td>
                  <td>{row.warehouses.length === 0 ? '—' : row.warehouses.join(', ')}</td>
                  <td>{row.shipQty}</td>
                  <td>{row.backorderQty}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Panel>
    </div>
  )
}

export function FulfillmentDetailPage() {
  const { id } = useParams()
  const quotationId = Number(id)
  if (!Number.isFinite(quotationId) || quotationId <= 0) {
    return <p className="error">Invalid quote.</p>
  }
  return (
    <div className="stack">
      <p>
        <Link to="/fulfillment">Back to fulfillment</Link>
      </p>
      <FulfillmentPanel quotationId={quotationId} ops />
    </div>
  )
}

export function FulfillmentPanel({ quotationId, ops = false }: { quotationId: number; ops?: boolean }) {
  const role = useAppSelector((state) => state.auth.user?.role)
  const canMutate = ops && role === 'FINANCE_OPS'
  const query = useGetFulfillmentQuery(quotationId)
  const warehousesQuery = useGetWarehousesQuery()
  const [autoPlan, autoState] = useAutoFulfillmentMutation()
  const [overridePlan, overrideState] = useOverrideFulfillmentMutation()
  const [consolidate, consolidateState] = useConsolidateBackorderMutation()
  const [error, setError] = useState<string | null>(null)
  const [line, setLine] = useState<FulfillmentLine | null>(null)
  const [rows, setRows] = useState<Array<{ warehouseId: string; quantity: string }>>([])

  const plan = query.data

  function openOverride(next: FulfillmentLine) {
    setLine(next)
    setError(null)
    setRows(
      next.allocations.length > 0
        ? next.allocations.map((allocation) => ({
            warehouseId: allocation.warehouseId == null ? '' : String(allocation.warehouseId),
            quantity: String(allocation.quantity),
          }))
        : [{ warehouseId: '', quantity: String(next.quantity) }],
    )
  }

  async function onAuto() {
    setError(null)
    try {
      await autoPlan(quotationId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not auto-split'))
    }
  }

  async function onConsolidate() {
    setError(null)
    try {
      await consolidate(quotationId).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not consolidate backorder'))
    }
  }

  async function onOverride(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (line == null) {
      return
    }
    setError(null)
    try {
      await overridePlan({
        id: quotationId,
        body: {
          lineId: line.lineId,
          rows: rows.map((row) => ({
            warehouseId: row.warehouseId === '' ? null : Number(row.warehouseId),
            quantity: Number(row.quantity),
            kind: row.warehouseId === '' ? 'BACKORDER' : 'SHIP',
          })),
        },
      }).unwrap()
      setLine(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save split'))
    }
  }

  if (query.isLoading) {
    return <p className="muted">Loading split…</p>
  }
  if (query.isError || !plan) {
    return <p className="error">Could not load fulfillment. Confirm the quote first.</p>
  }

  return (
    <>
      <Panel
        title={`${plan.quoteNumber} · ${plan.customerName}`}
        badge={
          canMutate ? (
            <div className="form-actions">
              <button className="button" type="button" disabled={autoState.isLoading} onClick={() => void onAuto()}>
                {autoState.isLoading ? 'Splitting…' : 'Auto split'}
              </button>
              <button
                className="button button-secondary"
                type="button"
                disabled={consolidateState.isLoading}
                onClick={() => void onConsolidate()}
              >
                {consolidateState.isLoading ? 'Merging…' : 'Consolidate backorder'}
              </button>
            </div>
          ) : undefined
        }
      >
        <p className="muted">
          Ships {plan.shipQty}
          {plan.warehouses.length > 0 ? ` from ${plan.warehouses.join(', ')}` : ''}
          {plan.backorderQty > 0 ? ` · ${plan.backorderQty} on backorder` : ''}.
        </p>
        {error && line == null ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
        {plan.lines.length === 0 ? <p className="muted">No one-time products to ship.</p> : null}
        {plan.lines.map((item) => (
          <article key={item.lineId} className="quote-line">
            <h3>
              {item.productName} · qty {item.quantity}
            </h3>
            <table className="board-table">
              <thead>
                <tr>
                  <th>From</th>
                  <th>Qty</th>
                  <th>Kind</th>
                </tr>
              </thead>
              <tbody>
                {item.allocations.map((allocation) => (
                  <tr key={allocation.id}>
                    <td>{allocation.warehouseName ?? 'Backorder'}</td>
                    <td>{allocation.quantity}</td>
                    <td>{allocation.kind === 'SHIP' ? 'Ship' : 'Backorder'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {canMutate ? (
              <div className="form-actions">
                <button className="button" type="button" onClick={() => openOverride(item)}>
                  Override
                </button>
              </div>
            ) : null}
          </article>
        ))}
      </Panel>

      {line != null ? (
        <Modal title={`Split ${line.productName}`} onClose={() => setLine(null)}>
          <form className="form" onSubmit={onOverride}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <p className="muted field-full">Quantities must add up to {line.quantity}. Leave warehouse empty for backorder.</p>
            {rows.map((row, index) => (
              <div className="field-full policy-picker" key={index}>
                <label className="field">
                  Warehouse
                  <select
                    className="input"
                    value={row.warehouseId}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item, i) => (i === index ? { ...item, warehouseId: event.target.value } : item)),
                      )
                    }
                  >
                    <option value="">Backorder</option>
                    {(warehousesQuery.data ?? []).map((warehouse) => (
                      <option key={warehouse.id} value={warehouse.id}>
                        {warehouse.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  Qty
                  <input
                    className="input"
                    type="number"
                    min="1"
                    step="1"
                    value={row.quantity}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item, i) => (i === index ? { ...item, quantity: event.target.value } : item)),
                      )
                    }
                    required
                  />
                </label>
              </div>
            ))}
            <div className="form-actions">
              <button
                className="button button-secondary"
                type="button"
                onClick={() => setRows((current) => [...current, { warehouseId: '', quantity: '1' }])}
              >
                Add row
              </button>
              <button className="button" type="submit" disabled={overrideState.isLoading}>
                {overrideState.isLoading ? 'Saving…' : 'Save split'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </>
  )
}
