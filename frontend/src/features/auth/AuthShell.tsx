import type { ReactNode } from 'react'
import { APP_TITLE } from '../../constants/app'
import { useScreenPinch } from './useScreenPinch'

export function AuthShell({
  title,
  lede,
  children,
}: {
  title: string
  lede?: string
  children: ReactNode
}) {
  const pinch = useScreenPinch()

  return (
    <div className="auth-page">
      <div className="auth-leaf auth-leaf-1" aria-hidden />
      <div className="auth-leaf auth-leaf-2" aria-hidden />
      <div className="auth-desk" aria-hidden />
      <div className="auth-hand auth-hand-left" aria-hidden />
      <div className="auth-hand auth-hand-right" aria-hidden />
      <div className="auth-scene">
        <div
          className="auth-device"
          ref={pinch.deviceRef}
          onPointerDown={pinch.onPointerDown}
          onPointerMove={pinch.onPointerMove}
          onPointerUp={pinch.onPointerUp}
          onPointerCancel={pinch.onPointerUp}
          onWheel={pinch.onWheel}
          onDoubleClick={pinch.onDoubleClick}
        >
          <div className="auth-screen">
            <section className="auth-login-pane">
              <div className="auth-brand">
                <span className="auth-logo" aria-hidden />
                <span>{APP_TITLE}</span>
              </div>
              <div className="auth-form-wrap">
                {lede ? <p className="auth-kicker">{lede}</p> : null}
                <h1>{title}</h1>
                {children}
              </div>
              <p className="auth-copy">© 2026 {APP_TITLE}</p>
            </section>
            <aside className="auth-hero-pane" aria-hidden="true">
              <div className="auth-window-light" />
              <div className="auth-plant" />
              <div className="auth-hero-content">
                <h2>
                  From quote
                  <br />
                  to cash.
                </h2>
                <p>A modern sales operations platform for growing businesses.</p>
                <div className="auth-accent" />
                <div className="auth-features">
                  <div className="auth-feature">
                    <div className="auth-feature-badge">✓</div>
                    Faster approvals
                  </div>
                  <div className="auth-feature">
                    <div className="auth-feature-badge">▣</div>
                    Inventory aware
                  </div>
                  <div className="auth-feature">
                    <div className="auth-feature-badge">◌</div>
                    Smarter deals
                  </div>
                </div>
              </div>
              <div className="auth-mock-desk" />
              <div className="auth-laptop" />
              <div className="auth-notebook" />
              <div className="auth-mug" />
            </aside>
          </div>
        </div>
      </div>
    </div>
  )
}
