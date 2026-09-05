import { useState, type DragEvent, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Drawer } from '../../components/common/Drawer'
import { KanbanColumn, type DropHint } from '../../components/common/KanbanColumn'
import { notifyBlocked, notifyError, notifyMoved } from '../../components/common/notify'
import { Panel } from '../../components/common/Panel'
import { TicketCard } from '../../components/common/TicketCard'
import { StatusBadge, toneForQuotationStatus } from '../../components/ui/StatusBadge'
import { canClaimTodo, canWriteQuotations } from '../../features/auth/types'
import { SellerRequestPanel } from '../requests/SellerRequestDetailPage'
import { rupee } from '../requests/types'
import {
  useConvertRequestToQuotationMutation,
  useGetSellerRequestsQuery,
} from '../../stores/api/quoteRequestApi'
import {
  useApproveQuotationMutation,
  useCreateQuotationMutation,
  useGetCustomersQuery,
  useGetQuotationsQuery,
  useNegotiateQuotationMutation,
  useReopenQuotationMutation,
  useReturnQuotationToPendingMutation,
  useReturnQuotationToQueueMutation,
  useSubmitQuotationMutation,
} from '../../stores/api/quotationApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { QuotationPanel } from './QuotationDetailPage'
import {
  legalStatusOptions,
  money,
  PIPELINE_COLUMNS,
  quoteDropAction,
  requestDropAction,
  statusLabel,
  type QuotationStatus,
  type SellerBoardColumn,
} from './types'

type BoardView = 'board' | 'list'

type SellerDrag =
  | { kind: 'request'; id: number }
  | { kind: 'quote'; id: number }

function parseSellerDrag(event: DragEvent<HTMLElement>): SellerDrag | null {
  const raw = event.dataTransfer.getData('application/json') || event.dataTransfer.getData('text/plain')
  if (!raw) {
    return null
  }
  try {
    const value = JSON.parse(raw) as { kind?: string; id?: number }
    if ((value.kind === 'request' || value.kind === 'quote') && typeof value.id === 'number') {
      return { kind: value.kind, id: value.id }
    }
  } catch {
    return null
  }
  return null
}

function boardColumnLabel(column: SellerBoardColumn): string {
  if (column === 'TODO') {
    return 'To do'
  }
  return statusLabel(column)
}

function columnTint(status: QuotationStatus): string {
  if (status === 'DRAFT') {
    return 'kanban-gold'
  }
  if (status === 'PENDING_APPROVAL') {
    return 'kanban-tan'
  }
  if (status === 'APPROVED') {
    return 'kanban-royal'
  }
  if (status === 'NEGOTIATION') {
    return 'kanban-sky'
  }
  return 'kanban-green'
}

export function QuotationsListPage() {
  const user = useAppSelector((state) => state.auth.user)
  const canCreate = canWriteQuotations(user?.role)
  const canClaim = canClaimTodo(user?.role)
  const quotations = useGetQuotationsQuery()
  const requests = useGetSellerRequestsQuery()
  const customers = useGetCustomersQuery(undefined, { skip: !canCreate })
  const [createQuotation, createState] = useCreateQuotationMutation()
  const [convertRequest] = useConvertRequestToQuotationMutation()
  const [submitQuote] = useSubmitQuotationMutation()
  const [reopenQuote] = useReopenQuotationMutation()
  const [negotiateQuote] = useNegotiateQuotationMutation()
  const [approveQuote] = useApproveQuotationMutation()
  const [returnToQueue] = useReturnQuotationToQueueMutation()
  const [returnToPending] = useReturnQuotationToPendingMutation()
  const [customerId, setCustomerId] = useState('')
  const [search, setSearch] = useState('')
  const [drag, setDrag] = useState<SellerDrag | null>(null)
  const [over, setOver] = useState<string | null>(null)
  const [dropBusy, setDropBusy] = useState(false)
  const [params, setParams] = useSearchParams()
  const quoteId = Number(params.get('quote'))
  const requestId = Number(params.get('request'))
  const openQuote = Number.isFinite(quoteId) && quoteId > 0 ? quoteId : null
  const openRequest = Number.isFinite(requestId) && requestId > 0 ? requestId : null
  const view: BoardView = params.get('view') === 'list' ? 'list' : 'board'

  const rows = quotations.data ?? []
  const query = search.trim().toLowerCase()
  const filteredQuotes = query
    ? rows.filter(
        (quote) =>
          quote.quoteNumber.toLowerCase().includes(query) ||
          quote.customerName.toLowerCase().includes(query) ||
          quote.salesRepName.toLowerCase().includes(query),
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
    mergeParams({ quote: null, request: null })
  }

  function setView(next: BoardView) {
    mergeParams({ view: next === 'list' ? 'list' : null })
  }

  function openQuoteCard(id: number) {
    mergeParams({ quote: String(id), request: null })
  }

  function openRequestCard(id: number) {
    mergeParams({ request: String(id), quote: null })
  }

  function sellerAction(payload: SellerDrag, toColumn: SellerBoardColumn) {
    if (payload.kind === 'request') {
      return requestDropAction(toColumn, canClaim)
    }
    const quote = rows.find((item) => item.id === payload.id)
    if (!quote || quote.status === 'CONFIRMED') {
      return null
    }
    return quoteDropAction(
      quote.status,
      toColumn,
      legalStatusOptions({
        status: quote.status,
        canSubmit: canCreate,
        role: user?.role,
        riskLevel: quote.riskLevel,
        managerApprovedAt: quote.managerApprovedAt,
        hasSourceRequest: quote.sourceRequestNumber != null,
      }),
    )
  }

  function dropHint(columnId: SellerBoardColumn): DropHint {
    if (!drag || over !== columnId) {
      return 'idle'
    }
    const action = sellerAction(drag, columnId)
    return action ? 'allowed' : 'blocked'
  }

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try {
      const created = await createQuotation({ customerId: Number(customerId) }).unwrap()
      setCustomerId('')
      mergeParams({ quote: String(created.id), request: null })
      notifyMoved('Draft')
    } catch (err) {
      notifyError(apiErrorMessage(err, 'Could not create quotation'))
    }
  }

  async function onDropColumn(columnId: string, event: DragEvent<HTMLElement>) {
    const toColumn = columnId as SellerBoardColumn
    const payload = drag ?? parseSellerDrag(event)
    setOver(null)
    setDrag(null)
    if (!payload || dropBusy) {
      return
    }
    const action = sellerAction(payload, toColumn)
    if (action === 'noop') {
      return
    }
    if (!action) {
      notifyBlocked(boardColumnLabel(toColumn))
      return
    }
    setDropBusy(true)
    try {
      if (payload.kind === 'request' && action === 'convert') {
        await convertRequest(payload.id).unwrap()
        notifyMoved('Draft')
        return
      }
      if (payload.kind !== 'quote') {
        return
      }
      if (action === 'submit') {
        await submitQuote(payload.id).unwrap()
        notifyMoved(boardColumnLabel(toColumn))
        return
      }
      if (action === 'reopen') {
        await reopenQuote(payload.id).unwrap()
        notifyMoved('Draft')
        return
      }
      if (action === 'negotiate') {
        await negotiateQuote(payload.id).unwrap()
        notifyMoved('Negotiation')
        return
      }
      if (action === 'approve') {
        await approveQuote(payload.id).unwrap()
        notifyMoved('Approved')
        return
      }
      if (action === 'returnToPending') {
        await returnToPending(payload.id).unwrap()
        notifyMoved('Pending Approval')
        return
      }
      if (action === 'returnToQueue') {
        await returnToQueue(payload.id).unwrap()
        notifyMoved('To do')
        if (openQuote === payload.id) {
          closeDrawer()
        }
      }
    } catch (err) {
      notifyError(apiErrorMessage(err, 'Could not move that card'))
    } finally {
      setDropBusy(false)
    }
  }

  return (
    <div className="stack board-page">
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

      <Panel title="Pipeline" className="board-pipeline">
        <div className="board-toolbar">
          <label className="field">
            Search
            <input
              className="input"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
              }}
              placeholder="Quote number, request, customer, or assignee"
            />
          </label>
          <div className="view-toggle" role="group" aria-label="Board view">
            <button className={view === 'list' ? 'active' : ''} type="button" onClick={() => setView('list')}>
              List
            </button>
            <button className={view === 'board' ? 'active' : ''} type="button" onClick={() => setView('board')}>
              Board
            </button>
          </div>
        </div>
        {quotations.isLoading || requests.isLoading ? <p className="muted">Loading board…</p> : null}
        {quotations.isError ? <p className="error">Could not load quotations.</p> : null}
        {requests.isError ? <p className="error">Could not load requests.</p> : null}
        {view === 'list' ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Client</th>
                <th>Status</th>
                <th>Assignee</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {todoRequests.map((request) => (
                <tr
                  key={`request-${request.id}`}
                  className={openRequest === request.id ? 'board-row board-row-selected' : 'board-row'}
                  onClick={() => openRequestCard(request.id)}
                >
                  <td>
                    <div className="table-stack">
                      <span className="table-primary">{request.customerName}</span>
                      <span className="table-secondary">{request.requestNumber}</span>
                    </div>
                  </td>
                  <td>
                    <StatusBadge label="To do" tone="info" />
                  </td>
                  <td>Unassigned · salesperson or manager can claim</td>
                  <td>{rupee(request.catalogMrpTotal)}</td>
                </tr>
              ))}
              {PIPELINE_COLUMNS.flatMap((column) =>
                filteredQuotes
                  .filter((quote) => quote.status === column.status)
                  .map((quote) => (
                    <tr
                      key={`quote-${quote.id}`}
                      className={openQuote === quote.id ? 'board-row board-row-selected' : 'board-row'}
                      onClick={() => openQuoteCard(quote.id)}
                    >
                      <td>
                        <div className="table-stack">
                          <span className="table-primary">{quote.customerName}</span>
                          <span className="table-secondary">{quote.quoteNumber}</span>
                        </div>
                      </td>
                      <td>
                        <StatusBadge
                          label={statusLabel(quote.status)}
                          tone={toneForQuotationStatus(quote.status)}
                        />
                      </td>
                      <td>{quote.salesRepName}</td>
                      <td>₹{money(quote.totalAmount)}</td>
                    </tr>
                  )),
              )}
            </tbody>
          </table>
        ) : (
          <div className="kanban">
            <KanbanColumn
              columnId="TODO"
              title="To do"
              count={todoRequests.length}
              tint="kanban-sky"
              dropHint={dropHint('TODO')}
              canDrop={Boolean(drag && sellerAction(drag, 'TODO'))}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
            >
              {todoRequests.map((request) => (
                <TicketCard
                  key={`request-${request.id}`}
                  selected={openRequest === request.id}
                  status="To do"
                  tone="info"
                  title={request.customerName}
                  meta={request.requestNumber}
                  amount={rupee(request.catalogMrpTotal)}
                  assignee="Unassigned · salesperson or manager can claim"
                  onOpen={() => openRequestCard(request.id)}
                  dragPayload={JSON.stringify({ kind: 'request', id: request.id })}
                  dragging={drag?.kind === 'request' && drag.id === request.id}
                  onDragBegin={() => setDrag({ kind: 'request', id: request.id })}
                  onDragFinish={() => {
                    setDrag(null)
                    setOver(null)
                  }}
                />
              ))}
            </KanbanColumn>
            {PIPELINE_COLUMNS.map((column) => {
              const cards = filteredQuotes.filter((quote) => quote.status === column.status)
              return (
                <KanbanColumn
                  key={column.status}
                  columnId={column.status}
                  title={column.label}
                  count={cards.length}
                  tint={columnTint(column.status)}
                  dropHint={dropHint(column.status)}
                  canDrop={Boolean(drag && sellerAction(drag, column.status))}
                  onHover={setOver}
                  onLeave={(columnId) => {
                    setOver((current) => (current === columnId ? null : current))
                  }}
                  onDrop={onDropColumn}
                >
                  {cards.map((quote) => {
                    const draggable = quote.status !== 'CONFIRMED'
                    return (
                      <TicketCard
                        key={`quote-${quote.id}`}
                        selected={openQuote === quote.id}
                        status={statusLabel(quote.status)}
                        tone={toneForQuotationStatus(quote.status)}
                        title={quote.customerName}
                        meta={quote.quoteNumber}
                        amount={`₹${money(quote.totalAmount)}`}
                        assignee={quote.salesRepName}
                        onOpen={() => openQuoteCard(quote.id)}
                        dragPayload={draggable ? JSON.stringify({ kind: 'quote', id: quote.id }) : undefined}
                        dragging={drag?.kind === 'quote' && drag.id === quote.id}
                        onDragBegin={draggable ? () => setDrag({ kind: 'quote', id: quote.id }) : undefined}
                        onDragFinish={() => {
                          setDrag(null)
                          setOver(null)
                        }}
                      />
                    )
                  })}
                </KanbanColumn>
              )
            })}
          </div>
        )}
      </Panel>

      {openRequest != null || openQuote != null ? (
        <Drawer onClose={closeDrawer}>
          {openRequest != null ? (
            <SellerRequestPanel requestId={openRequest} onOpenQuotation={openQuoteCard} />
          ) : null}
          {openQuote != null ? <QuotationPanel quotationId={openQuote} onReturnedToQueue={closeDrawer} /> : null}
        </Drawer>
      ) : null}
    </div>
  )
}
