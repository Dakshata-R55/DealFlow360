import type { ReactNode } from 'react'
import { APP_TITLE } from '../../constants/app'

export function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="auth-page">
      <section className="auth-card">
        <p className="auth-kicker">{APP_TITLE}</p>
        <h1>{title}</h1>
        {children}
      </section>
    </div>
  )
}
