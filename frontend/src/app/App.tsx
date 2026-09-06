import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AppProviders } from './providers'
import { AppLayout } from '../components/layout/AppLayout'
import { CatalogPage } from '../features/admin/CatalogPage'
import { PlansPage } from '../features/admin/PlansPage'
import { PoliciesPage } from '../features/admin/PoliciesPage'
import { ProductDetailPage } from '../features/admin/ProductDetailPage'
import { UsersPage } from '../features/admin/UsersPage'
import { WarehousesPage } from '../features/admin/WarehousesPage'
import { CustomerSignupPage } from '../features/auth/CustomerSignupPage'
import { LoginPage } from '../features/auth/LoginPage'
import { SignupPage } from '../features/auth/SignupPage'
import { CustomerHomePage } from '../features/requests/CustomerHomePage'
import { CompaniesPage } from '../features/requests/CompaniesPage'
import { CompanyStorefrontPage } from '../features/requests/CompanyStorefrontPage'
import { CustomerOrdersPage } from '../features/requests/CustomerOrdersPage'
import { CustomerQuotationDetailPage } from '../features/requests/CustomerQuotationDetailPage'
import { CustomerQuotationsPage } from '../features/requests/CustomerQuotationsPage'
import { CustomerRequestDetailPage } from '../features/requests/CustomerRequestDetailPage'
import { MyRequestsPage } from '../features/requests/MyRequestsPage'
import { SellerRequestDetailPage } from '../features/requests/SellerRequestDetailPage'
import { SellerRequestsPage } from '../features/requests/SellerRequestsPage'
import { QuotationDetailPage } from '../features/quotations/QuotationDetailPage'
import { QuotationsListPage } from '../features/quotations/QuotationsListPage'
import { FulfillmentDetailPage, FulfillmentListPage } from '../features/fulfillment/FulfillmentPages'
import { DashboardPage } from '../pages/DashboardPage'
import { AdminRoute } from '../routes/AdminRoute'
import { CustomerRoute } from '../routes/CustomerRoute'
import { HomeRedirect } from '../routes/HomeRedirect'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { SalesWorkspaceRoute } from '../routes/SalesWorkspaceRoute'

export function App() {
  return (
    <AppProviders>
      <Toaster position="top-right" richColors closeButton />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/signup/customer" element={<CustomerSignupPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<HomeRedirect />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route element={<CustomerRoute />}>
                <Route path="/customer" element={<CustomerHomePage />} />
                <Route path="/customer/companies" element={<CompaniesPage />} />
                <Route path="/customer/companies/:companyId" element={<CompanyStorefrontPage />} />
                <Route path="/customer/requests" element={<MyRequestsPage />} />
                <Route path="/customer/requests/:id" element={<CustomerRequestDetailPage />} />
                <Route path="/customer/quotations" element={<CustomerQuotationsPage />} />
                <Route path="/customer/quotations/:id" element={<CustomerQuotationDetailPage />} />
                <Route path="/customer/orders" element={<CustomerOrdersPage />} />
              </Route>
              <Route element={<SalesWorkspaceRoute />}>
                <Route path="/quotations" element={<QuotationsListPage />} />
                <Route path="/quotations/:id" element={<QuotationDetailPage />} />
                <Route path="/fulfillment" element={<FulfillmentListPage />} />
                <Route path="/fulfillment/:id" element={<FulfillmentDetailPage />} />
                <Route path="/requests" element={<SellerRequestsPage />} />
                <Route path="/requests/:id" element={<SellerRequestDetailPage />} />
              </Route>
              <Route element={<AdminRoute />}>
                <Route path="/admin/catalog" element={<CatalogPage />} />
                <Route path="/admin/products/:id" element={<ProductDetailPage />} />
                <Route path="/admin/policies" element={<PoliciesPage />} />
                <Route path="/admin/warehouses" element={<WarehousesPage />} />
                <Route path="/admin/plans" element={<PlansPage />} />
                <Route path="/admin/users" element={<UsersPage />} />
              </Route>
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProviders>
  )
}
