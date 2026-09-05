import type { ReactNode } from 'react'

export function Panel({
  title,
  badge,
  className,
  children,
}: {
  title: string
  badge?: ReactNode
  className?: string
  children: ReactNode
}) {
  return (
    <section className={className ? `panel ${className}` : 'panel'}>
      <div className="panel-head">
        <h2>{title}</h2>
        {badge}
      </div>
      {children}
    </section>
  )
}
