import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { ACCESS_TOKEN_KEY } from '../../features/auth/tokenStorage'
import { apiBaseUrl } from '../../utils/apiUrl'

type AuthSliceState = {
  auth?: {
    accessToken: string | null
  }
}

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: apiBaseUrl(),
    prepareHeaders: (headers, { getState }) => {
      const state = getState() as AuthSliceState
      const token = state.auth?.accessToken ?? localStorage.getItem(ACCESS_TOKEN_KEY)
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  endpoints: () => ({}),
})
