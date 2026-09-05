import type { DragEvent, ReactNode } from 'react'

export type DropHint = 'idle' | 'allowed' | 'blocked'

export function KanbanColumn({
  columnId,
  title,
  count,
  tint,
  dropHint,
  canDrop,
  onHover,
  onLeave,
  onDrop,
  children,
}: {
  columnId: string
  title: string
  count: number
  tint: string
  dropHint: DropHint
  canDrop: boolean
  onHover: (columnId: string) => void
  onLeave: (columnId: string) => void
  onDrop: (columnId: string, event: DragEvent<HTMLElement>) => void
  children: ReactNode
}) {
  const hintClass =
    dropHint === 'allowed' ? ' kanban-column-allowed' : dropHint === 'blocked' ? ' kanban-column-blocked' : ''
  return (
    <section
      className={`kanban-column ${tint}${hintClass}`}
      onDragOver={(event) => {
        event.preventDefault()
        event.dataTransfer.dropEffect = canDrop ? 'move' : 'none'
        onHover(columnId)
      }}
      onDragLeave={(event) => {
        const next = event.relatedTarget
        if (next instanceof Node && event.currentTarget.contains(next)) {
          return
        }
        onLeave(columnId)
      }}
      onDrop={(event) => {
        event.preventDefault()
        onDrop(columnId, event)
      }}
    >
      <h3>
        {title}
        <span>{count}</span>
      </h3>
      <div className="kanban-lane">{children}</div>
    </section>
  )
}
