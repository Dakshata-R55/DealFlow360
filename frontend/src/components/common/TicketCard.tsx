import { useRef } from 'react'
import { StatusBadge, type StatusTone } from '../ui/StatusBadge'
import { initials } from './initials'

export function TicketCard({
  selected,
  status,
  tone,
  title,
  meta,
  amount,
  assignee,
  hint,
  onOpen,
  dragPayload,
  dragging,
  onDragBegin,
  onDragFinish,
}: {
  selected: boolean
  status: string
  tone: StatusTone
  title: string
  meta: string
  amount: string
  assignee?: string
  hint?: string | null
  onOpen: () => void
  dragPayload?: string
  dragging?: boolean
  onDragBegin?: () => void
  onDragFinish?: () => void
}) {
  const skipClick = useRef(false)
  const classes = [
    'quote-card',
    selected ? 'quote-card-selected' : '',
    dragPayload ? 'quote-card-draggable' : '',
    dragging ? 'quote-card-dragging' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      className={classes}
      type="button"
      draggable={Boolean(dragPayload)}
      onDragStart={(event) => {
        if (!dragPayload) {
          event.preventDefault()
          return
        }
        skipClick.current = true
        event.dataTransfer.setData('application/json', dragPayload)
        event.dataTransfer.setData('text/plain', dragPayload)
        event.dataTransfer.effectAllowed = 'move'
        onDragBegin?.()
      }}
      onDragEnd={() => {
        onDragFinish?.()
        window.setTimeout(() => {
          skipClick.current = false
        }, 0)
      }}
      onClick={() => {
        if (skipClick.current) {
          skipClick.current = false
          return
        }
        onOpen()
      }}
    >
      <span className={`quote-card-stripe quote-card-stripe-${tone}`} aria-hidden />
      <div className="quote-card-body">
        <div className="quote-card-top">
          <StatusBadge label={status} tone={tone} />
          <span className="avatar-sm">{initials(assignee ?? title)}</span>
        </div>
        <p className="quote-card-customer">{title}</p>
        <p className="quote-card-meta">{meta}</p>
        <p className="quote-card-amount">{amount}</p>
        {hint ? <p className="quote-card-hint">{hint}</p> : null}
      </div>
    </button>
  )
}
