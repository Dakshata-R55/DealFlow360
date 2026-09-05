import { isApiResponse, isHealthData, type HealthData } from '../../types/health'
import { baseApi } from './baseApi'

export const healthApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getHealth: builder.query<{ data: HealthData; timestamp: string; success: boolean }, void>({
      query: () => ({
        url: '/api/health',
        validateStatus: (_response, body) => isApiResponse(body, isHealthData),
      }),
      transformResponse: (payload: unknown) => {
        if (!isApiResponse(payload, isHealthData)) {
          throw new Error('Health check returned an unexpected payload')
        }
        return {
          data: payload.data,
          timestamp: payload.timestamp,
          success: payload.success,
        }
      },
    }),
  }),
})

export const { useGetHealthQuery } = healthApi
