import {
  isCustomer,
  isListOf,
  isQuotation,
  isRecommendation,
  isSalesUser,
  type Customer,
  type Quotation,
  type Recommendation,
  type SalesUser,
} from '../../features/quotations/types'
import { isApiResponse } from '../../types/api'
import { baseApi } from './baseApi'

const isCustomerList = isListOf(isCustomer)
const isQuotationList = isListOf(isQuotation)
const isRecommendationList = isListOf(isRecommendation)
const isSalesUserList = isListOf(isSalesUser)

function unwrap<T>(isData: (value: unknown) => value is T, label: string) {
  return (payload: unknown): T => {
    if (!isApiResponse(payload, isData)) {
      throw new Error(`${label} returned an unexpected payload`)
    }
    return payload.data
  }
}

export type CreateQuotationBody = {
  customerId: number
}

export type AddQuotationLineBody = {
  productId: number
  variantId?: number | null
  quantity: number
}

export type PatchQuotationLineBody = {
  quantity?: number
  discountPercent?: number
}

export const quotationApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['Customer', 'Quotation', 'QuotationList', 'Recommendation', 'SalesUser', 'QuoteRequestList', 'CustomerQuotation'],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getCustomers: builder.query<Customer[], void>({
        query: () => ({
          url: '/api/customers',
          validateStatus: (_response, json) => isApiResponse(json, isCustomerList),
        }),
        transformResponse: unwrap(isCustomerList, 'GET /api/customers'),
        providesTags: ['Customer'],
      }),
      getQuotations: builder.query<Quotation[], void>({
        query: () => ({
          url: '/api/quotations',
          validateStatus: (_response, json) => isApiResponse(json, isQuotationList),
        }),
        transformResponse: unwrap(isQuotationList, 'GET /api/quotations'),
        providesTags: ['QuotationList'],
      }),
      getSalesUsers: builder.query<SalesUser[], void>({
        query: () => ({
          url: '/api/quotations/sales-users',
          validateStatus: (_response, json) => isApiResponse(json, isSalesUserList),
        }),
        transformResponse: unwrap(isSalesUserList, 'GET /api/quotations/sales-users'),
        providesTags: ['SalesUser'],
      }),
      getQuotation: builder.query<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}`,
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'GET /api/quotations/{id}'),
        providesTags: (_result, _error, id) => [
          { type: 'Quotation', id },
          { type: 'Recommendation', id },
        ],
      }),
      createQuotation: builder.mutation<Quotation, CreateQuotationBody>({
        query: (body) => ({
          url: '/api/quotations',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations'),
        invalidatesTags: ['QuotationList'],
      }),
      saveQuotationDraft: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}`,
          method: 'PATCH',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'PATCH /api/quotations/{id}'),
        invalidatesTags: (_result, _error, id) => ['QuotationList', { type: 'Quotation', id }],
      }),
      addQuotationLine: builder.mutation<Quotation, { quotationId: number; body: AddQuotationLineBody }>({
        query: ({ quotationId, body }) => ({
          url: `/api/quotations/${quotationId}/lines`,
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/lines'),
        invalidatesTags: (_result, _error, { quotationId }) => [
          'QuotationList',
          { type: 'Quotation', id: quotationId },
          { type: 'Recommendation', id: quotationId },
        ],
      }),
      updateQuotationLine: builder.mutation<
        Quotation,
        { quotationId: number; lineId: number; body: PatchQuotationLineBody }
      >({
        query: ({ quotationId, lineId, body }) => ({
          url: `/api/quotations/${quotationId}/lines/${lineId}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'PATCH /api/quotations/{id}/lines'),
        invalidatesTags: (_result, _error, { quotationId }) => [
          'QuotationList',
          { type: 'Quotation', id: quotationId },
          { type: 'Recommendation', id: quotationId },
        ],
      }),
      deleteQuotationLine: builder.mutation<Quotation, { quotationId: number; lineId: number }>({
        query: ({ quotationId, lineId }) => ({
          url: `/api/quotations/${quotationId}/lines/${lineId}`,
          method: 'DELETE',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'DELETE /api/quotations/{id}/lines'),
        invalidatesTags: (_result, _error, { quotationId }) => [
          'QuotationList',
          { type: 'Quotation', id: quotationId },
          { type: 'Recommendation', id: quotationId },
        ],
      }),
      evaluateQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/evaluate`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/evaluate'),
        invalidatesTags: (_result, _error, id) => ['QuotationList', { type: 'Quotation', id }],
      }),
      getRecommendations: builder.query<Recommendation[], number>({
        query: (id) => ({
          url: `/api/quotations/${id}/recommendations`,
          validateStatus: (_response, json) => isApiResponse(json, isRecommendationList),
        }),
        transformResponse: unwrap(isRecommendationList, 'GET /api/quotations/{id}/recommendations'),
        providesTags: (_result, _error, id) => [{ type: 'Recommendation', id }],
      }),
      dismissRecommendation: builder.mutation<Recommendation[], { quotationId: number; productId: number }>({
        query: ({ quotationId, productId }) => ({
          url: `/api/quotations/${quotationId}/recommendations/${productId}/dismiss`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isRecommendationList),
        }),
        transformResponse: unwrap(isRecommendationList, 'POST /api/quotations/{id}/recommendations/dismiss'),
        invalidatesTags: (_result, _error, { quotationId }) => [{ type: 'Recommendation', id: quotationId }],
      }),
      submitQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/submit`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/submit'),
        invalidatesTags: (_result, _error, id) => [
          'QuotationList',
          { type: 'Quotation', id },
          { type: 'Recommendation', id },
        ],
      }),
      assignQuotation: builder.mutation<Quotation, { quotationId: number; salesRepId: number }>({
        query: ({ quotationId, salesRepId }) => ({
          url: `/api/quotations/${quotationId}/assignee`,
          method: 'PATCH',
          body: { salesRepId },
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'PATCH /api/quotations/{id}/assignee'),
        invalidatesTags: (_result, _error, { quotationId }) => [
          'QuotationList',
          { type: 'Quotation', id: quotationId },
        ],
      }),
      reopenQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/reopen`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/reopen'),
        invalidatesTags: (_result, _error, id) => [
          'QuotationList',
          { type: 'Quotation', id },
          { type: 'Recommendation', id },
        ],
      }),
      negotiateQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/negotiate`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/negotiate'),
        invalidatesTags: (_result, _error, id) => ['QuotationList', { type: 'Quotation', id }],
      }),
      approveQuotation: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/approve`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/approve'),
        invalidatesTags: (_result, _error, id) => ['QuotationList', { type: 'Quotation', id }, 'QuoteRequestList', 'CustomerQuotation'],
      }),
      returnQuotationToQueue: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/return-to-queue`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/return-to-queue'),
        invalidatesTags: (_result, _error, id) => [
          'QuotationList',
          { type: 'Quotation', id },
          'QuoteRequestList',
          'CustomerQuotation',
        ],
      }),
      returnQuotationToPending: builder.mutation<Quotation, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/return-to-pending`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isQuotation),
        }),
        transformResponse: unwrap(isQuotation, 'POST /api/quotations/{id}/return-to-pending'),
        invalidatesTags: (_result, _error, id) => [
          'QuotationList',
          { type: 'Quotation', id },
          'QuoteRequestList',
          'CustomerQuotation',
        ],
      }),
    }),
  })

export const {
  useGetCustomersQuery,
  useGetQuotationsQuery,
  useGetSalesUsersQuery,
  useGetQuotationQuery,
  useCreateQuotationMutation,
  useSaveQuotationDraftMutation,
  useAddQuotationLineMutation,
  useUpdateQuotationLineMutation,
  useDeleteQuotationLineMutation,
  useEvaluateQuotationMutation,
  useGetRecommendationsQuery,
  useDismissRecommendationMutation,
  useSubmitQuotationMutation,
  useAssignQuotationMutation,
  useReopenQuotationMutation,
  useNegotiateQuotationMutation,
  useApproveQuotationMutation,
  useReturnQuotationToQueueMutation,
  useReturnQuotationToPendingMutation,
} = quotationApi
