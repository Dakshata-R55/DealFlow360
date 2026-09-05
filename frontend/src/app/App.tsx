import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AppProviders } from './providers'
import { AppLayout } from '../components/layout/AppLayout'
import { HomePage } from '../pages/HomePage'

export function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProviders>
  )
}
