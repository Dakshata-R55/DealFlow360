import {
  isPublicProductList,
  isSellerCompany,
  isSellerCompanyList,
  type PublicProduct,
  type SellerCompany,
} from '../../features/requests/types'
import { isApiResponse } from '../../types/api'
import { baseApi } from './baseApi'

function unwrap<T>(isData: (value: unknown) => value is T, label: string) {
  return (payload: unknown): T => {
    if (!isApiResponse(payload, isData)) {
      throw new Error(`${label} returned an unexpected payload`)
    }
    return payload.data
  }
}

export const marketplaceApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['Company', 'CompanyProducts'],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getCompanies: builder.query<SellerCompany[], string | void>({
        query: (query) => ({
          url: '/api/customer/companies',
          params: query ? { q: query } : undefined,
          validateStatus: (_response, json) => isApiResponse(json, isSellerCompanyList),
        }),
        transformResponse: unwrap(isSellerCompanyList, 'GET /api/customer/companies'),
        providesTags: ['Company'],
      }),
      getCompany: builder.query<SellerCompany, number>({
        query: (companyId) => ({
          url: `/api/customer/companies/${companyId}`,
          validateStatus: (_response, json) => isApiResponse(json, isSellerCompany),
        }),
        transformResponse: unwrap(isSellerCompany, 'GET /api/customer/companies/{id}'),
      }),
      getCompanyProducts: builder.query<PublicProduct[], number>({
        query: (companyId) => ({
          url: `/api/customer/companies/${companyId}/products`,
          validateStatus: (_response, json) => isApiResponse(json, isPublicProductList),
        }),
        transformResponse: unwrap(isPublicProductList, 'GET /api/customer/companies/{id}/products'),
        providesTags: (_result, _error, companyId) => [{ type: 'CompanyProducts', id: companyId }],
      }),
    }),
  })

export const { useGetCompaniesQuery, useGetCompanyQuery, useGetCompanyProductsQuery } = marketplaceApi
