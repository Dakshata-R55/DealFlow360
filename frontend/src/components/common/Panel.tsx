import type { ReactNode } from 'react'

export function Panel({
  title,
  badge,
  children,
}: {
  title: string
  badge?: ReactNode
  children: ReactNode
}) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
        {badge}
      </div>
      {children}
    </section>
  )
}
