import { useState, type DragEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { notifyBlocked } from '../../components/common/notify'
import { Drawer } from '../../components/common/Drawer'
import { KanbanColumn, type DropHint } from '../../components/common/KanbanColumn'
import { Panel } from '../../components/common/Panel'
import { TicketCard } from '../../components/common/TicketCard'
import { StatusBadge, toneForQuotationStatus } from '../../components/ui/StatusBadge'
import { useGetCustomerQuotationsQuery } from '../../stores/api/quoteRequestApi'
import { CustomerQuotationPanel } from './CustomerQuotationDetailPage'
import { rupee } from './types'

type BoardView = 'board' | 'list'

type QuoteColumnId = 'NEGOTIATION' | 'APPROVED'

function dropHintFor(
  dragId: number | null,
  over: string | null,
  columnId: QuoteColumnId,
  origin: QuoteColumnId | null,
): DropHint {
  if (dragId == null || over !== columnId) {
    return 'idle'
  }
  return origin === columnId ? 'allowed' : 'blocked'
}

export function CustomerQuotationsPage() {
  const query = useGetCustomerQuotationsQuery()
  const [params, setParams] = useSearchParams()
  const [dragId, setDragId] = useState<number | null>(null)
  const [over, setOver] = useState<string | null>(null)
  const quoteId = Number(params.get('quote'))
  const openQuote = Number.isFinite(quoteId) && quoteId > 0 ? quoteId : null
  const view: BoardView = params.get('view') === 'list' ? 'list' : 'board'
  const rows = (query.data ?? []).filter((row) => row.status === 'NEGOTIATION' || row.status === 'APPROVED')
  const negotiation = rows.filter((row) => row.status === 'NEGOTIATION')
  const approved = rows.filter((row) => row.status === 'APPROVED')
  const dragged = dragId == null ? null : rows.find((row) => row.id === dragId)
  const origin: QuoteColumnId | null =
    dragged?.status === 'NEGOTIATION' || dragged?.status === 'APPROVED' ? dragged.status : null

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
    mergeParams({ quote: null })
  }

  function openCard(id: number) {
    mergeParams({ quote: String(id) })
  }

  function setView(next: BoardView) {
    mergeParams({ view: next === 'list' ? 'list' : null })
  }

  function onDropColumn(columnId: string, _event: DragEvent<HTMLElement>) {
    const toColumn = columnId as QuoteColumnId
    setOver(null)
    setDragId(null)
    if (origin != null && origin !== toColumn) {
      notifyBlocked(toColumn === 'NEGOTIATION' ? 'Negotiation' : 'Approved')
    }
  }

  return (
    <div className="stack">
      <Panel title="Quotations">
        <div className="board-toolbar">
          <p className="muted">Open a card to counter or confirm on credit. Status stays with the seller — drops here do not move columns.</p>
          <div className="view-toggle" role="group" aria-label="Quotations view">
            <button className={view === 'list' ? 'active' : ''} type="button" onClick={() => setView('list')}>
              List
            </button>
            <button className={view === 'board' ? 'active' : ''} type="button" onClick={() => setView('board')}>
              Board
            </button>
          </div>
        </div>
        {query.isLoading ? <p className="muted">Loading quotations…</p> : null}
        {query.isError ? <p className="error">Could not load quotations.</p> : null}
        {rows.length === 0 && !query.isLoading ? (
          <p className="muted">Nothing waiting for you. Offers appear here once the seller sends them.</p>
        ) : null}
        {rows.length > 0 && view === 'list' ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Quote</th>
                <th>Status</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((quote) => (
                <tr
                  key={quote.id}
                  className={openQuote === quote.id ? 'board-row board-row-selected' : 'board-row'}
                  onClick={() => openCard(quote.id)}
                >
                  <td>
                    <div className="table-stack">
                      <span className="table-primary">{quote.sellerCompanyName}</span>
                      <span className="table-secondary">
                        {quote.quoteNumber} · {quote.sourceRequestNumber}
                      </span>
                    </div>
                  </td>
                  <td>
                    <StatusBadge label={quote.statusLabel} tone={toneForQuotationStatus(quote.status)} />
                  </td>
                  <td>{rupee(quote.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
        {rows.length > 0 && view === 'board' ? (
          <div className="kanban kanban-2">
            <KanbanColumn
              columnId="NEGOTIATION"
              title="Negotiation"
              count={negotiation.length}
              tint="kanban-sky"
              dropHint={dropHintFor(dragId, over, 'NEGOTIATION', origin)}
              canDrop={origin === 'NEGOTIATION'}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
            >
              {negotiation.map((quote) => (
                <TicketCard
                  key={quote.id}
                  selected={openQuote === quote.id}
                  status={quote.statusLabel}
                  tone={toneForQuotationStatus(quote.status)}
                  title={quote.quoteNumber}
                  meta={`${quote.sellerCompanyName} · ${quote.sourceRequestNumber}`}
                  amount={rupee(quote.totalAmount)}
                  onOpen={() => openCard(quote.id)}
                  dragPayload={JSON.stringify({ kind: 'customer-quote', id: quote.id })}
                  dragging={dragId === quote.id}
                  onDragBegin={() => setDragId(quote.id)}
                  onDragFinish={() => {
                    setDragId(null)
                    setOver(null)
                  }}
                />
              ))}
            </KanbanColumn>
            <KanbanColumn
              columnId="APPROVED"
              title="Approved"
              count={approved.length}
              tint="kanban-royal"
              dropHint={dropHintFor(dragId, over, 'APPROVED', origin)}
              canDrop={origin === 'APPROVED'}
              onHover={setOver}
              onLeave={(columnId) => {
                setOver((current) => (current === columnId ? null : current))
              }}
              onDrop={onDropColumn}
            >
              {approved.map((quote) => (
                <TicketCard
                  key={quote.id}
                  selected={openQuote === quote.id}
                  status={quote.statusLabel}
                  tone={toneForQuotationStatus(quote.status)}
                  title={quote.quoteNumber}
                  meta={`${quote.sellerCompanyName} · ${quote.sourceRequestNumber}`}
                  amount={rupee(quote.totalAmount)}
                  onOpen={() => openCard(quote.id)}
                  dragPayload={JSON.stringify({ kind: 'customer-quote', id: quote.id })}
                  dragging={dragId === quote.id}
                  onDragBegin={() => setDragId(quote.id)}
                  onDragFinish={() => {
                    setDragId(null)
                    setOver(null)
                  }}
                />
              ))}
            </KanbanColumn>
          </div>
        ) : null}
      </Panel>
      {openQuote != null ? (
        <Drawer onClose={closeDrawer}>
          <CustomerQuotationPanel quotationId={openQuote} />
        </Drawer>
      ) : null}
    </div>
  )
}
