import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { useGetCompaniesQuery } from '../../stores/api/marketplaceApi'

export function CompaniesPage() {
  const [search, setSearch] = useState('')
  const companies = useGetCompaniesQuery(search.trim() || undefined)

  return (
    <div className="stack">
      <Panel title="Companies">
        <label className="field">
          Search companies
          <input
            className="input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search companies..."
          />
        </label>
        {companies.isLoading ? <p className="muted">Loading companies…</p> : null}
        {companies.isError ? <p className="error">Could not load companies.</p> : null}
        <div className="recs">
          {(companies.data ?? []).map((company) => (
            <article key={company.id} className="rec-card">
              <h3>{company.name}</h3>
              <p className="muted">{company.categories.join(' • ') || 'Catalog'}</p>
              <p>{company.description}</p>
              <p>
                <Link className="link" to={`/customer/companies/${company.id}`}>
                  View Company
                </Link>
              </p>
            </article>
          ))}
        </div>
      </Panel>
    </div>
  )
}
