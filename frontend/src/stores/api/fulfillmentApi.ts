import { isApiResponse } from '../../types/api'
import {
  isFulfillmentList,
  isFulfillmentPlan,
  type FulfillmentListItem,
  type FulfillmentPlan,
} from '../../features/fulfillment/types'
import { baseApi } from './baseApi'

function unwrap<T>(isData: (value: unknown) => value is T, label: string) {
  return (payload: unknown): T => {
    if (!isApiResponse(payload, isData)) {
      throw new Error(`${label} returned an unexpected payload`)
    }
    return payload.data
  }
}

export type FulfillmentOverrideBody = {
  lineId: number
  rows: Array<{ warehouseId: number | null; quantity: number; kind: 'SHIP' | 'BACKORDER' }>
}

export const fulfillmentApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['Fulfillment', 'FulfillmentList', 'Inventory'],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getFulfillmentList: builder.query<FulfillmentListItem[], void>({
        query: () => ({
          url: '/api/fulfillment',
          validateStatus: (_response, json) => isApiResponse(json, isFulfillmentList),
        }),
        transformResponse: unwrap(isFulfillmentList, 'GET /api/fulfillment'),
        providesTags: ['FulfillmentList'],
      }),
      getFulfillment: builder.query<FulfillmentPlan, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/fulfillment`,
          validateStatus: (_response, json) => isApiResponse(json, isFulfillmentPlan),
        }),
        transformResponse: unwrap(isFulfillmentPlan, 'GET /api/quotations/{id}/fulfillment'),
        providesTags: (_result, _error, id) => [{ type: 'Fulfillment', id }],
      }),
      autoFulfillment: builder.mutation<FulfillmentPlan, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/fulfillment/auto`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isFulfillmentPlan),
        }),
        transformResponse: unwrap(isFulfillmentPlan, 'POST /api/quotations/{id}/fulfillment/auto'),
        invalidatesTags: (_result, _error, id) => [
          { type: 'Fulfillment', id },
          'FulfillmentList',
          'Inventory',
        ],
      }),
      overrideFulfillment: builder.mutation<FulfillmentPlan, { id: number; body: FulfillmentOverrideBody }>({
        query: ({ id, body }) => ({
          url: `/api/quotations/${id}/fulfillment`,
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isFulfillmentPlan),
        }),
        transformResponse: unwrap(isFulfillmentPlan, 'PUT /api/quotations/{id}/fulfillment'),
        invalidatesTags: (_result, _error, { id }) => [
          { type: 'Fulfillment', id },
          'FulfillmentList',
          'Inventory',
        ],
      }),
      consolidateBackorder: builder.mutation<FulfillmentPlan, number>({
        query: (id) => ({
          url: `/api/quotations/${id}/fulfillment/consolidate-backorder`,
          method: 'POST',
          validateStatus: (_response, json) => isApiResponse(json, isFulfillmentPlan),
        }),
        transformResponse: unwrap(isFulfillmentPlan, 'POST consolidate-backorder'),
        invalidatesTags: (_result, _error, id) => [{ type: 'Fulfillment', id }, 'FulfillmentList'],
      }),
    }),
  })

export const {
  useGetFulfillmentListQuery,
  useGetFulfillmentQuery,
  useAutoFulfillmentMutation,
  useOverrideFulfillmentMutation,
  useConsolidateBackorderMutation,
} = fulfillmentApi
