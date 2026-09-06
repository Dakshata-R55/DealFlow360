import { NavLink, useNavigate } from 'react-router-dom'
import { APP_TITLE } from '../../constants/app'
import { logout } from '../../features/auth/authSlice'
import { canAccessQuotations, isCustomerUser } from '../../features/auth/types'
import { baseApi } from '../../stores/api/baseApi'
import { useAppDispatch, useAppSelector } from '../../stores/hooks'

function linkClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'side-link active' : 'side-link'
}

export function Sidebar() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((state) => state.auth.user)
  const customer = isCustomerUser(user?.role)
  const sales = canAccessQuotations(user?.role)
  const admin = user?.role === 'ADMIN'

  function onLogout() {
    dispatch(logout())
    dispatch(baseApi.util.resetApiState())
    navigate('/login', { replace: true })
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-mark">Df</span>
        <span>{APP_TITLE}</span>
      </div>
      <nav className="sidebar-nav" aria-label="Main">
        <p className="sidebar-group">Main</p>
        {customer ? (
          <>
            <NavLink to="/customer" end className={linkClass}>
              Home
            </NavLink>
            <NavLink to="/customer/companies" className={linkClass}>
              Companies
            </NavLink>
            <NavLink to="/customer/requests" className={linkClass}>
              My Requests
            </NavLink>
            <NavLink to="/customer/quotations" className={linkClass}>
              Quotations
            </NavLink>
            <NavLink to="/customer/orders" className={linkClass}>
              Orders
            </NavLink>
          </>
        ) : null}
        {customer ? null : (
          <NavLink to="/dashboard" className={linkClass}>
            Dashboard
          </NavLink>
        )}
        {sales ? (
          <>
            <NavLink to="/quotations" className={linkClass}>
              Board
            </NavLink>
            {user?.role === 'FINANCE_OPS' ? (
              <NavLink to="/fulfillment" className={linkClass}>
                Fulfillment
              </NavLink>
            ) : null}
          </>
        ) : null}
        {admin ? (
          <>
            <p className="sidebar-group">Admin</p>
            <NavLink to="/admin/catalog" className={linkClass}>
              Catalog
            </NavLink>
            <NavLink to="/admin/policies" className={linkClass}>
              Policies
            </NavLink>
            <NavLink to="/admin/warehouses" className={linkClass}>
              Warehouses
            </NavLink>
            <NavLink to="/admin/plans" className={linkClass}>
              Plans
            </NavLink>
            <NavLink to="/admin/users" className={linkClass}>
              Team
            </NavLink>
          </>
        ) : null}
      </nav>
      <div className="sidebar-foot">
        <p className="sidebar-group">Account</p>
        <button className="side-link side-link-danger" type="button" onClick={onLogout}>
          Log out
        </button>
      </div>
    </aside>
  )
}
