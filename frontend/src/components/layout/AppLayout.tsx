import { Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '../../stores/hooks'
import { initials } from '../common/initials'
import { Sidebar } from './Sidebar'

function titleForPath(pathname: string) {
  if (pathname === '/customer') {
    return 'Home'
  }
  if (pathname.startsWith('/customer/companies/') && pathname !== '/customer/companies') {
    return 'Storefront'
  }
  if (pathname.startsWith('/customer/companies')) {
    return 'Companies'
  }
  if (pathname.startsWith('/customer/requests')) {
    return 'My Requests'
  }
  if (pathname.startsWith('/customer/quotations')) {
    return 'Quotations'
  }
  if (pathname.startsWith('/customer/orders')) {
    return 'Orders'
  }
  if (pathname.startsWith('/fulfillment')) {
    return 'Fulfillment'
  }
  if (pathname.startsWith('/quotations')) {
    return 'Board'
  }
  if (pathname.startsWith('/requests')) {
    return 'Requests'
  }
  if (pathname.startsWith('/admin/catalog') || pathname.startsWith('/admin/products')) {
    return 'Catalog'
  }
  if (pathname.startsWith('/admin/policies')) {
    return 'Policies'
  }
  if (pathname.startsWith('/admin/warehouses')) {
    return 'Warehouses'
  }
  if (pathname.startsWith('/admin/plans')) {
    return 'Plans'
  }
  if (pathname.startsWith('/dashboard')) {
    return 'Dashboard'
  }
  return 'Dealflow360'
}

export function AppLayout() {
  const location = useLocation()
  const user = useAppSelector((state) => state.auth.user)

  return (
    <div className="app-frame">
      <Sidebar />
      <div className="workspace">
        <header className="topbar">
          {user ? (
            <div className="topbar-user">
              <span className="avatar">{initials(user.name)}</span>
              <span>
                <strong>{user.name}</strong>
                <span className="muted">
                  {user.email} · {user.role}
                </span>
              </span>
            </div>
          ) : null}
        </header>
        <main className="workspace-main">
          <h1 className="page-title">{titleForPath(location.pathname)}</h1>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
