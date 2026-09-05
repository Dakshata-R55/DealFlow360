import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import { useGetCompaniesQuery } from '../../stores/api/marketplaceApi'

export function CompaniesPage() {
  const [search, setSearch] = useState('')
  const companies = useGetCompaniesQuery(search.trim() || undefined)

  return (
    <div className="stack">
      <Panel title="Sellers">
        <div className="board-toolbar">
          <label className="field">
            Search
            <input
              className="input"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search companies"
            />
          </label>
        </div>
        {companies.isLoading ? <p className="muted">Loading companies…</p> : null}
        {companies.isError ? <p className="error">Could not load companies.</p> : null}
        {(companies.data ?? []).length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Company</th>
                <th>Categories</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {(companies.data ?? []).map((company) => (
                <tr key={company.id}>
                  <td>
                    <div className="table-stack">
                      <span className="table-primary">{company.name}</span>
                      <span className="table-secondary">{company.description}</span>
                    </div>
                  </td>
                  <td>{company.categories.join(' • ') || 'Catalog'}</td>
                  <td>
                    <Link className="button button-secondary" to={`/customer/companies/${company.id}`}>
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Panel>
    </div>
  )
}
