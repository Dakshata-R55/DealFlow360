import { Outlet, useLocation } from 'react-router-dom'
import { Header } from './Header'

export function AppLayout() {
  const location = useLocation()
  const wide = location.pathname.startsWith('/admin')

  return (
    <div className={wide ? 'shell wide' : 'shell'}>
      <Header />
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
