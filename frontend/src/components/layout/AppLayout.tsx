import { Outlet } from 'react-router-dom'
import { Header } from './Header'

export function AppLayout() {
  return (
    <div className="shell">
      <Header />
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
