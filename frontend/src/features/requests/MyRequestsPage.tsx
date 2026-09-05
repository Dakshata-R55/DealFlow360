import { useState, type DragEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { notifyBlocked, notifyError, notifyMoved } from '../../components/common/notify'
import { Drawer } from '../../components/common/Drawer'
import { KanbanColumn, type DropHint } from '../../components/common/KanbanColumn'
import { Panel } from '../../components/common/Panel'
import { TicketCard } from '../../components/common/TicketCard'
import { StatusBadge, toneForTicket } from '../../components/ui/StatusBadge'
import {
  useGetCustomerRequestsQuery,
  useSubmitCustomerRequestMutation,
  useWithdrawCustomerRequestMutation,
} from '../../stores/api/quoteRequestApi'
import { apiErrorMessage } from '../../types/api'
import { CustomerRequestPanel } from './CustomerRequestDetailPage'
import { activeRequestLabel, homeBucket, rupee, type QuoteRequest } from './types'

type BoardView = 'board' | 'list'

type RequestColumnId = 'DRAFT' | 'SUBMITTED' | 'WORKING'

function parseRequestId(event: DragEvent<HTMLElement>): number | null {
  const raw = event.dataTransfer.getData('application/json') || event.dataTransfer.getData('text/plain')
  if (!raw) {
    return null
  }
  try {
    const value = JSON.parse(raw) as { kind?: string; id?: number }
    if (value.kind === 'customer-request' && typeof value.id === 'number') {
      return value.id
    }
  } catch {
    return null
  }
  return null
}

function customerRequestDropAction(
  request: QuoteRequest,
  fromColumn: RequestColumnId,
  toColumn: RequestColumnId,
): 'submit' | 'withdraw' | 'noop' | null {
  if (fromColumn === toColumn) {
    return 'noop'
  }
  if (fromColumn === 'WORKING' || toColumn === 'WORKING') {
    return null
  }
  if (fromColumn === 'DRAFT' && toColumn === 'SUBMITTED') {
    return 'submit'
  }
  if (fromColumn === 'SUBMITTED' && toColumn === 'DRAFT' && request.quotationId == null) {
    return 'withdraw'
  }
  return null
}

export function MyRequestsPage() {
  const requests = useGetCustomerRequestsQuery()
  const [submitRequest] = useSubmitCustomerRequestMutation()
  const [withdrawRequest] = useWithdrawCustomerRequestMutation()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [dragId, setDragId] = useState<number | null>(null)
  const [over, setOver] = useState<string | null>(null)
  const [dropBusy, setDropBusy] = useState(false)
  const requestId = Number(params.get('request'))
  const openRequest = Number.isFinite(requestId) && requestId > 0 ? requestId : null
  const view: BoardView = params.get('view') === 'list' ? 'list' : 'board'
  const rows = (requests.data ?? []).filter((row) => homeBucket(row) === 'active')
  const drafts = rows.filter((row) => row.status === 'DRAFT')
  const sent = rows.filter((row) => row.status === 'SUBMITTED' || row.status === 'UNDER_REVIEW')
  const working = rows.filter(
    (row) => row.quotationStatus === 'DRAFT' || row.quotationStatus === 'PENDING_APPROVAL',
  )

  function mergeParams(updates: Record<string, string | null>) {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(updates)) {
      if (value == null || value === '') {
        next.delete(key)
      } else {
        next.set(key, value)
      }
    }
    setParams(next)
  }

  function closeDrawer() {
    mergeParams({ request: null })
  }

  function openCard(id: number) {
    mergeParams({ request: String(id) })
  }

  function setView(next: BoardView) {
    mergeParams({ view: next === 'list' ? 'list' : null })
  }

  function columnFor(request: QuoteRequest): RequestColumnId | null {
    if (request.status === 'DRAFT') {
      return 'DRAFT'
    }
    if (request.status === 'SUBMITTED' || request.status === 'UNDER_REVIEW') {
      return 'SUBMITTED'
    }
    if (request.quotationStatus === 'DRAFT' || request.quotationStatus === 'PENDING_APPROVAL') {
      return 'WORKING'
    }
    return null
  }

  function dropAction(id: number, toColumn: RequestColumnId) {
    const request = rows.find((row) => row.id === id)
    if (!request) {
      return null
    }
    const fromColumn = columnFor(request)
    if (!fromColumn) {
      return null
    }
    return customerRequestDropAction(request, fromColumn, toColumn)
  }

  function dropHint(columnId: RequestColumnId): DropHint {
    if (dragId == null || over !== columnId) {
      return 'idle'
    }
    return dropAction(dragId, columnId) ? 'allowed' : 'blocked'
  }

  async function onDropColumn(columnId: string, event: DragEvent<HTMLElement>) {
    const toColumn = columnId as RequestColumnId
    const id = dragId ?? parseRequestId(event)
    setOver(null)
    setDragId(null)
    if (id == null || dropBusy) {
      return
    }
    const action = dropAction(id, toColumn)
    if (action === 'noop') {
      return
    }
    if (!action) {
      notifyBlocked(toColumn === 'WORKING' ? 'Seller working' : toColumn === 'DRAFT' ? 'Draft' : 'Submitted')
      return
    }
    setDropBusy(true)
    try {
      if (action === 'submit') {
        await submitRequest(id).unwrap()
        notifyMoved('Submitted')
        return
      }
      await withdrawRequest(id).unwrap()
      notifyMoved('Draft')
    } catch (err) {
      notifyError(apiErrorMessage(err, 'Could not move that card'))
    } finally {
      setDropBusy(false)
    }
  }

  return (
    <div className="stack">
      <Panel title="My Requests">
        <div className="board-toolbar">
          <p className="muted">Open a card to edit, resubmit, or revoke. Drag Draft ↔ Submitted when that move is legal.</p>
          <div className="view-toggle" role="group" aria-label="Requests view">
            <button className={view === 'list' ? 'active' : ''} type="button" onClick={() => setView('list')}>
              List
            </button>
            <button className={view === 'board' ? 'active' : ''} type="button" onClick={() => setView('board')}>
              Board
            </button>
          </div>
        </div>
        {requests.isLoading ? <p className="muted">Loading requests…</p> : null}
        {requests.isError ? <p className="error">Could not load requests.</p> : null}
        {rows.length === 0 && !requests.isLoading ? (
          <p className="muted">
            No active requests. Offers waiting for a counter or credit are under Quotations.
          </p>
        ) : null}
        {rows.length > 0 && view === 'list' ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Request</th>
                <th>Status</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((request) => (
                <tr
                  key={request.id}
                  className={openRequest === request.id ? 'board-row board-row-selected' : 'board-row'}
                  onClick={() => openCard(request.id)}
                >
                  <td>
                    <div className="table-stack">
                      <span className="table-primary">{request.sellerCompanyName}</span>
                      <span className="table-secondary">{request.requestNumber}</span>
                    </div>
                  </td>
                  <td>
                    <StatusBadge
                      label={activeRequestLabel(request)}
                      tone={toneForTicket(request.status, request.quotationStatus)}
                    />
                  </td>
                  <td>MRP {rupee(request.catalogMrpTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
        {rows.length > 0 && view === 'board' ? (
          <div className="kanban kanban-3">
            <RequestColumn
              columnId="DRAFT"
              title="Draft"
              tint="kanban-gold"
              rows={drafts}
              openRequest={openRequest}
              dragId={dragId}
              dropHint={dropHint('DRAFT')}
              canDrop={dragId != null && Boolean(dropAction(dragId, 'DRAFT'))}
              onOpen={openCard}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
              onDragBegin={setDragId}
              onDragFinish={() => {
                setDragId(null)
                setOver(null)
              }}
              draggable
            />
            <RequestColumn
              columnId="SUBMITTED"
              title="Submitted"
              tint="kanban-tan"
              rows={sent}
              openRequest={openRequest}
              dragId={dragId}
              dropHint={dropHint('SUBMITTED')}
              canDrop={dragId != null && Boolean(dropAction(dragId, 'SUBMITTED'))}
              onOpen={openCard}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
              onDragBegin={setDragId}
              onDragFinish={() => {
                setDragId(null)
                setOver(null)
              }}
              draggable
            />
            <RequestColumn
              columnId="WORKING"
              title="Seller working"
              tint="kanban-royal"
              rows={working}
              openRequest={openRequest}
              dragId={dragId}
              dropHint={dropHint('WORKING')}
              canDrop={false}
              onOpen={openCard}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
              onDragBegin={setDragId}
              onDragFinish={() => {
                setDragId(null)
                setOver(null)
              }}
              draggable={false}
            />
          </div>
        ) : null}
      </Panel>
      {openRequest != null ? (
        <Drawer onClose={closeDrawer}>
          <CustomerRequestPanel
            requestId={openRequest}
            onOpenQuotation={(quotationId) => navigate(`/customer/quotations?quote=${quotationId}`)}
            onRevoked={closeDrawer}
          />
        </Drawer>
      ) : null}
    </div>
  )
}

function RequestColumn({
  columnId,
  title,
  tint,
  rows,
  openRequest,
  dragId,
  dropHint,
  canDrop,
  onOpen,
  onHover,
  onLeave,
  onDrop,
  onDragBegin,
  onDragFinish,
  draggable,
}: {
  columnId: RequestColumnId
  title: string
  tint: string
  rows: QuoteRequest[]
  openRequest: number | null
  dragId: number | null
  dropHint: DropHint
  canDrop: boolean
  onOpen: (id: number) => void
  onHover: (columnId: string) => void
  onLeave: (columnId: string) => void
  onDrop: (columnId: string, event: DragEvent<HTMLElement>) => void
  onDragBegin: (id: number) => void
  onDragFinish: () => void
  draggable: boolean
}) {
  return (
    <KanbanColumn
      columnId={columnId}
      title={title}
      count={rows.length}
      tint={tint}
      dropHint={dropHint}
      canDrop={canDrop}
      onHover={onHover}
      onLeave={onLeave}
      onDrop={onDrop}
    >
      {rows.map((request) => (
        <TicketCard
          key={request.id}
          selected={openRequest === request.id}
          status={activeRequestLabel(request)}
          tone={toneForTicket(request.status, request.quotationStatus)}
          title={request.requestNumber}
          meta={request.sellerCompanyName}
          amount={`MRP ${rupee(request.catalogMrpTotal)}`}
          onOpen={() => onOpen(request.id)}
          dragPayload={draggable ? JSON.stringify({ kind: 'customer-request', id: request.id }) : undefined}
          dragging={dragId === request.id}
          onDragBegin={draggable ? () => onDragBegin(request.id) : undefined}
          onDragFinish={onDragFinish}
        />
      ))}
    </KanbanColumn>
  )
}
