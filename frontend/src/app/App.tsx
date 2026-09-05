import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppProviders } from './providers'
import { AppLayout } from '../components/layout/AppLayout'
import { CatalogPage } from '../features/admin/CatalogPage'
import { PlansPage } from '../features/admin/PlansPage'
import { PoliciesPage } from '../features/admin/PoliciesPage'
import { ProductDetailPage } from '../features/admin/ProductDetailPage'
import { WarehousesPage } from '../features/admin/WarehousesPage'
import { LoginPage } from '../features/auth/LoginPage'
import { SignupPage } from '../features/auth/SignupPage'
import { QuotationDetailPage } from '../features/quotations/QuotationDetailPage'
import { QuotationsListPage } from '../features/quotations/QuotationsListPage'
import { DashboardPage } from '../pages/DashboardPage'
import { AdminRoute } from '../routes/AdminRoute'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { SalesWorkspaceRoute } from '../routes/SalesWorkspaceRoute'

export function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route element={<SalesWorkspaceRoute />}>
                <Route path="/quotations" element={<QuotationsListPage />} />
                <Route path="/quotations/:id" element={<QuotationDetailPage />} />
              </Route>
              <Route element={<AdminRoute />}>
                <Route path="/admin/catalog" element={<CatalogPage />} />
                <Route path="/admin/products/:id" element={<ProductDetailPage />} />
                <Route path="/admin/policies" element={<PoliciesPage />} />
                <Route path="/admin/warehouses" element={<WarehousesPage />} />
                <Route path="/admin/plans" element={<PlansPage />} />
              </Route>
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProviders>
  )
}
