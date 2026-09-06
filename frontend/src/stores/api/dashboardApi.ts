import { isApiResponse } from '../../types/api'
import { isDashboard, isSearchHitList, type Dashboard, type SearchHit } from '../../features/dashboard/types'
import { baseApi } from './baseApi'

function unwrap<T>(isData: (value: unknown) => value is T, label: string) {
  return (payload: unknown): T => {
    if (!isApiResponse(payload, isData)) {
      throw new Error(`${label} returned an unexpected payload`)
    }
    return payload.data
  }
}

export const dashboardApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['Dashboard'],
  })
  .injectEndpoints({
  endpoints: (builder) => ({
    getDashboard: builder.query<Dashboard, void>({
      query: () => ({
        url: '/api/dashboard',
        validateStatus: (_response, json) => isApiResponse(json, isDashboard),
      }),
      transformResponse: unwrap(isDashboard, 'GET /api/dashboard'),
      providesTags: ['Dashboard'],
    }),
    searchCompany: builder.query<SearchHit[], string>({
      query: (q) => ({
        url: `/api/search?q=${encodeURIComponent(q)}`,
        validateStatus: (_response, json) => isApiResponse(json, isSearchHitList),
      }),
      transformResponse: unwrap(isSearchHitList, 'GET /api/search'),
    }),
  }),
})

export const { useGetDashboardQuery, useLazySearchCompanyQuery } = dashboardApi
