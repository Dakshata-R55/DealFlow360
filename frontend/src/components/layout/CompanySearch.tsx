import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { isCompanyUser } from '../../features/auth/types'
import { useLazySearchCompanyQuery } from '../../stores/api/dashboardApi'
import { useAppSelector } from '../../stores/hooks'

export function CompanySearch() {
  const user = useAppSelector((state) => state.auth.user)
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [search, result] = useLazySearchCompanyQuery()

  useEffect(() => {
    if (q.trim().length < 2) {
      return
    }
    const timer = window.setTimeout(() => {
      void search(q.trim())
    }, 250)
    return () => window.clearTimeout(timer)
  }, [q, search])

  if (!isCompanyUser(user?.role)) {
    return null
  }

  const hits = q.trim().length < 2 ? [] : (result.data ?? [])

  return (
    <div className="topbar-search">
      <label className="topbar-search-field">
        <span className="visually-hidden">Search</span>
        <svg className="topbar-search-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            d="M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm10-1-4.35-4.35"
          />
        </svg>
        <input
          className="topbar-search-input"
          value={q}
          placeholder="Search quotations, customers, products…"
          onChange={(event) => {
            setQ(event.target.value)
            setOpen(true)
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => window.setTimeout(() => setOpen(false), 150)}
        />
      </label>
      {open && hits.length > 0 ? (
        <ul className="topbar-search-results">
          {hits.map((hit) => (
            <li key={`${hit.kind}-${hit.id}`}>
              <button
                className="topbar-search-hit"
                type="button"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => {
                  navigate(hit.href)
                  setQ('')
                  setOpen(false)
                }}
              >
                <span className="muted">{hit.kind}</span>
                {hit.label}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
