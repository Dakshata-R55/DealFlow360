import { Link, Navigate, useNavigate } from 'react-router-dom'
import { StatusBadge, toneForQuotationStatus } from '../components/ui/StatusBadge'
import { isCustomerUser } from '../features/auth/types'
import { useGetDashboardQuery } from '../stores/api/dashboardApi'
import { useAppSelector } from '../stores/hooks'

function ago(iso: string) {
  const then = Date.parse(iso)
  if (!Number.isFinite(then)) {
    return ''
  }
  const minutes = Math.max(0, Math.round((Date.now() - then) / 60000))
  if (minutes < 60) {
    return `${minutes}m ago`
  }
  const hours = Math.round(minutes / 60)
  if (hours < 48) {
    return `${hours}h ago`
  }
  return `${Math.round(hours / 24)}d ago`
}

function kpiTone(label: string) {
  const text = label.toLowerCase()
  if (text.includes('pending')) {
    return 'warn'
  }
  if (text.includes('revenue')) {
    return 'ok'
  }
  if (text.includes('customer') || text.includes('warehouse')) {
    return 'teal'
  }
  if (text.includes('plan')) {
    return 'ok'
  }
  return 'info'
}

function activityTone(title: string) {
  const text = title.toLowerCase()
  if (text.includes('pending') || text.includes('negotiation')) {
    return 'warn'
  }
  if (text.includes('approved') || text.includes('confirmed')) {
    return 'ok'
  }
  if (text.includes('warehouse') || text.includes('draft')) {
    return 'muted'
  }
  return 'info'
}

function actionTone(label: string) {
  return kpiTone(label)
}

function Icon({ name }: { name: string }) {
  const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round' as const }
  if (name === 'clock') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="8" {...stroke} />
        <path d="M12 8v5l3 2" {...stroke} />
      </svg>
    )
  }
  if (name === 'money') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="3" y="6" width="18" height="12" rx="2" {...stroke} />
        <circle cx="12" cy="12" r="2.4" {...stroke} />
      </svg>
    )
  }
  if (name === 'people') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="9" cy="9" r="3" {...stroke} />
        <circle cx="16" cy="10" r="2.4" {...stroke} />
        <path d="M4 19c.6-3 2.8-5 5-5s4.4 2 5 5M14 19c.3-2 1.6-3.4 3.2-3.4 1.5 0 2.6 1 3 2.4" {...stroke} />
      </svg>
    )
  }
  if (name === 'box') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 8h16v11H4zM4 8l8-4 8 4M12 4v15" {...stroke} />
      </svg>
    )
  }
  if (name === 'grid') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <rect x="4" y="4" width="7" height="7" rx="1.2" {...stroke} />
        <rect x="13" y="4" width="7" height="7" rx="1.2" {...stroke} />
        <rect x="4" y="13" width="7" height="7" rx="1.2" {...stroke} />
        <rect x="13" y="13" width="7" height="7" rx="1.2" {...stroke} />
      </svg>
    )
  }
  if (name === 'send') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 12 20 5l-6 14-2.5-5.5L4 12z" {...stroke} />
      </svg>
    )
  }
  if (name === 'check') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="8" {...stroke} />
        <path d="m8.5 12.2 2.4 2.4 4.6-5.2" {...stroke} />
      </svg>
    )
  }
  if (name === 'plus') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="8" {...stroke} />
        <path d="M12 8v8M8 12h8" {...stroke} />
      </svg>
    )
  }
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7 4h8l4 4v12H7z" {...stroke} />
      <path d="M15 4v4h4M9 12h6M9 16h4" {...stroke} />
    </svg>
  )
}

function iconForKpi(label: string) {
  const text = label.toLowerCase()
  if (text.includes('pending')) {
    return 'clock'
  }
  if (text.includes('revenue')) {
    return 'money'
  }
  if (text.includes('customer')) {
    return 'people'
  }
  if (text.includes('warehouse')) {
    return 'box'
  }
  if (text.includes('categor') || text.includes('plan')) {
    return 'grid'
  }
  return 'doc'
}

function iconForActivity(title: string) {
  const text = title.toLowerCase()
  if (text.includes('pending') || text.includes('negotiation')) {
    return 'clock'
  }
  if (text.includes('approved') || text.includes('confirmed')) {
    return 'check'
  }
  if (text.includes('warehouse')) {
    return 'box'
  }
  if (text.includes('customer')) {
    return 'people'
  }
  if (text.includes('draft')) {
    return 'doc'
  }
  return 'send'
}

function iconForAction(label: string) {
  const text = label.toLowerCase()
  if (text.includes('new') || text.includes('add') || text.includes('create')) {
    return 'plus'
  }
  if (text.includes('pending') || text.includes('fulfill')) {
    return 'clock'
  }
  if (text.includes('warehouse')) {
    return 'box'
  }
  if (text.includes('board') || text.includes('catalog') || text.includes('polic')) {
    return 'grid'
  }
  return 'doc'
}

export function DashboardPage() {
  const user = useAppSelector((state) => state.auth.user)
  const navigate = useNavigate()
  const query = useGetDashboardQuery(undefined, { skip: isCustomerUser(user?.role) })

  if (isCustomerUser(user?.role)) {
    return <Navigate to="/customer" replace />
  }

  const dash = query.data
  const maxBar = Math.max(1, ...(dash?.bars.map((bar) => bar.count) ?? [1]))

  return (
    <div className="dash">
      {query.isLoading ? <p className="muted">Loading dashboard…</p> : null}
      {query.isError ? <p className="error">Could not load dashboard.</p> : null}
      {dash ? (
        <div className="dash-shell">
          <div className="dash-main">
            <div className="dash-hero">
              <div>
                <h2 className="dash-hello">{dash.greeting}</h2>
                <p className="dash-sub">{dash.subtitle}</p>
              </div>
              <Link className="button dash-cta" to={dash.primaryCtaHref}>
                + {dash.primaryCtaLabel}
              </Link>
            </div>

            <div className="dash-kpis">
              {dash.kpis.map((kpi) => (
                <button
                  key={kpi.label}
                  className="dash-kpi"
                  type="button"
                  onClick={() => navigate(kpi.href)}
                >
                  <span className={`dash-ico dash-ico-${kpiTone(kpi.label)}`}>
                    <Icon name={iconForKpi(kpi.label)} />
                  </span>
                  <span>
                    <span className="dash-kpi-label">{kpi.label}</span>
                    <strong>{kpi.value}</strong>
                  </span>
                </button>
              ))}
            </div>

            <div className="dash-split">
              <section className="panel dash-panel">
                <div className="panel-head">
                  <h2>{dash.chartTitle}</h2>
                  <span className="dash-chip">This month</span>
                </div>
                <div className="dash-bars" role="img" aria-label={dash.chartTitle}>
                  {dash.bars.map((bar) => (
                    <button
                      key={bar.label}
                      className={bar.count === maxBar && bar.count > 0 ? 'dash-bar dash-bar-peak' : 'dash-bar'}
                      type="button"
                      onClick={() => navigate(bar.href)}
                    >
                      <span className="dash-bar-count">{bar.count}</span>
                      <span
                        className="dash-bar-fill"
                        style={{ height: `${Math.max(12, (bar.count / maxBar) * 100)}%` }}
                      />
                      <span className="dash-bar-label">{bar.label}</span>
                    </button>
                  ))}
                </div>
              </section>
              <section className="panel dash-panel">
                <div className="panel-head">
                  <h2>Recent activity</h2>
                  <Link className="link" to={dash.primaryCtaHref}>
                    View all
                  </Link>
                </div>
                {dash.activity.length === 0 ? <p className="muted">Nothing yet.</p> : null}
                <ul className="dash-activity">
                  {dash.activity.map((item) => (
                    <li key={`${item.href}-${item.at}`}>
                      <button type="button" onClick={() => navigate(item.href)}>
                        <span className={`dash-ico dash-ico-sm dash-ico-${activityTone(item.title)}`}>
                          <Icon name={iconForActivity(item.title)} />
                        </span>
                        <span className="dash-activity-copy">
                          <strong>{item.title}</strong>
                          <span className="muted">{item.subtitle}</span>
                        </span>
                        <span className="dash-activity-time">{ago(item.at)}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </section>
            </div>

            <section className="panel dash-panel dash-table">
              <div className="panel-head">
                <h2>{dash.tableTitle}</h2>
                <Link className="link" to={dash.primaryCtaHref}>
                  View all
                </Link>
              </div>
              {dash.table.length === 0 ? <p className="muted">No rows yet.</p> : null}
              {dash.table.length > 0 ? (
                <table className="board-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Name</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Last updated</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dash.table.map((row) => (
                      <tr key={row.href + row.idLabel} className="board-row" onClick={() => navigate(row.href)}>
                        <td>{row.idLabel}</td>
                        <td>
                          <span className="table-primary">{row.primary}</span>
                          <div className="table-secondary">{row.secondary}</div>
                        </td>
                        <td>{row.amount}</td>
                        <td>
                          <StatusBadge label={row.status} tone={toneForQuotationStatus(row.status)} />
                        </td>
                        <td>{ago(row.updatedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : null}
            </section>
          </div>

          <aside className="dash-rail">
            <section className="dash-promo">
              <span className="dash-leaf dash-leaf-a" />
              <span className="dash-leaf dash-leaf-b" />
              <p>From quote to cash. A modern sales operations platform for growing businesses.</p>
            </section>
            <section className="panel dash-panel">
              <div className="panel-head">
                <h2>Quick actions</h2>
              </div>
              <ul className="dash-actions">
                {dash.actions.map((action) => (
                  <li key={action.href + action.label}>
                    <Link to={action.href}>
                      <span className={`dash-ico dash-ico-sm dash-ico-${actionTone(action.label)}`}>
                        <Icon name={iconForAction(action.label)} />
                      </span>
                      {action.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
            <section className="dash-tagline">
              <span className="dash-leaf dash-leaf-c" />
              <span className="dash-leaf dash-leaf-d" />
              <p>Smarter deals. Stronger growth.</p>
            </section>
          </aside>
        </div>
      ) : null}
    </div>
  )
}
