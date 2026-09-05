import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { apiBaseUrl } from '../../utils/apiUrl'

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: apiBaseUrl(),
  }),
  endpoints: () => ({}),
})
