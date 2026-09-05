import {
  isQuoteRequest,
  isQuoteRequestList,
  type QuoteRequest,
} from '../../features/requests/types'
import { isQuotation, type Quotation } from '../../features/quotations/types'
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

export const quoteRequestApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['QuoteRequest', 'QuoteRequestList', 'QuotationList'],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getCustomerRequests: builder.query<QuoteRequest[], void>({
        query: () => ({
          url: '/api/customer/requests',
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequestList),
        }),
        transformResponse: unwrap(isQuoteRequestList, 'GET /api/customer/requests'),
        providesTags: ['QuoteRequestList'],
      }),
      getCustomerRequest: builder.query<QuoteRequest, number>({
        query: (id) => ({
          url: `/api/customer/requests/${id}`,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'GET /api/customer/requests/{id}'),
        providesTags: (_result, _error, id) => [{ type: 'QuoteRequest', id }],
      }),
      createCustomerRequest: builder.mutation<QuoteRequest, { sellerCompanyId: number }>({
        query: (body) => ({
          url: '/api/customer/requests',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'POST /api/customer/requests'),
        invalidatesTags: ['QuoteRequestList'],
      }),
      patchCustomerRequest: builder.mutation<
        QuoteRequest,
        { id: number; body: { requestedDeliveryDate?: string | null; targetBudget?: number | null; expectedDiscountPercent?: number | null; notes?: string } }
      >({
        query: ({ id, body }) => ({
          url: `/api/customer/requests/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'PATCH /api/customer/requests/{id}'),
        invalidatesTags: (_result, _error, { id }) => [{ type: 'QuoteRequest', id }, 'QuoteRequestList'],
      }),
      addCustomerRequestLine: builder.mutation<
        QuoteRequest,
        { requestId: number; body: { productId: number; quantity: number; notes?: string } }
      >({
        query: ({ requestId, body }) => ({
          url: `/api/customer/requests/${requestId}/lines`,
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'POST /api/customer/requests/{id}/lines'),
        invalidatesTags: (_result, _error, { requestId }) => [
          { type: 'QuoteRequest', id: requestId },
          'QuoteRequestList',
        ],
      }),
      updateCustomerRequestLine: builder.mutation<
        QuoteRequest,
        { requestId: number; lineId: number; body: { quantity?: number; notes?: string; expectedDiscountPercent?: number } }
      >({
        query: ({ requestId, lineId, body }) => ({
          url: `/api/customer/requests/${requestId}/lines/${lineId}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'PATCH /api/customer/requests/{id}/lines'),
        invalidatesTags: (_result, _error, { requestId }) => [
          { type: 'QuoteRequest', id: requestId },
          'QuoteRequestList',
        ],
      }),
      deleteCustomerRequestLine: builder.mutation<QuoteRequest, { requestId: number; lineId: number }>({
        query: ({ requestId, lineId }) => ({
          url: `/api/customer/requests/${requestId}/lines/${lineId}`,
          method: 'DELETE',
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'DELETE /api/customer/requests/{id}/lines'),
        invalidatesTags: (_result, _error, { requestId }) => [
          { type: 'QuoteRequest', id: requestId },
          'QuoteRequestList',
        ],
      }),
      submitCustomerRequest: builder.mutation<QuoteRequest, number>({
        query: (id) => ({
          url: `/api/customer/requests/${id}/submit`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'POST /api/customer/requests/{id}/submit'),
        invalidatesTags: (_result, _error, id) => [{ type: 'QuoteRequest', id }, 'QuoteRequestList'],
      }),
      cancelCustomerRequest: builder.mutation<QuoteRequest, number>({
        query: (id) => ({
          url: `/api/customer/requests/${id}/cancel`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'POST /api/customer/requests/{id}/cancel'),
        invalidatesTags: (_result, _error, id) => [{ type: 'QuoteRequest', id }, 'QuoteRequestList'],
      }),
      getSellerRequests: builder.query<QuoteRequest[], void>({
        query: () => ({
          url: '/api/requests',
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequestList),
        }),
        transformResponse: unwrap(isQuoteRequestList, 'GET /api/requests'),
        providesTags: ['QuoteRequestList'],
      }),
      getSellerRequest: builder.query<QuoteRequest, number>({
        query: (id) => ({
          url: `/api/requests/${id}`,
          validateStatus: (_response, json) => isApiResponse(json, isQuoteRequest),
        }),
        transformResponse: unwrap(isQuoteRequest, 'GET /api/requests/{id}'),
        providesTags: (_result, _error, id) => [{ type: 'QuoteRequest', id }],
      }),
      convertRequestToQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/requests/${id}/convert-to-quotation`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/requests/{id}/convert-to-quotation'),
        invalidatesTags: ['QuoteRequestList', 'QuotationList'],
      }),
    }),
  })

export const {
  useGetCustomerRequestsQuery,
  useGetCustomerRequestQuery,
  useCreateCustomerRequestMutation,
  usePatchCustomerRequestMutation,
  useAddCustomerRequestLineMutation,
  useUpdateCustomerRequestLineMutation,
  useDeleteCustomerRequestLineMutation,
  useSubmitCustomerRequestMutation,
  useCancelCustomerRequestMutation,
  useGetSellerRequestsQuery,
  useGetSellerRequestQuery,
  useConvertRequestToQuotationMutation,
} = quoteRequestApi
