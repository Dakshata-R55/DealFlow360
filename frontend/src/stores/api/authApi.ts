import { isAuthSession, isAuthUser, type AuthSession, type AuthUser, type LoginRequest, type SignupRequest } from '../../features/auth/types'
import { isApiResponse } from '../../types/api'
import { baseApi } from './baseApi'

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    login: builder.mutation<AuthSession, LoginRequest>({
      query: (body) => ({
        url: '/api/auth/login',
        method: 'POST',
        body,
        validateStatus: (_response, json) => isApiResponse(json, isAuthSession),
      }),
      transformResponse: (payload: unknown) => {
        if (!isApiResponse(payload, isAuthSession)) {
          throw new Error('Login returned an unexpected payload')
        }
        return payload.data
      },
    }),
    signup: builder.mutation<AuthSession, SignupRequest>({
      query: (body) => ({
        url: '/api/auth/signup',
        method: 'POST',
        body,
        validateStatus: (_response, json) => isApiResponse(json, isAuthSession),
      }),
      transformResponse: (payload: unknown) => {
        if (!isApiResponse(payload, isAuthSession)) {
          throw new Error('Signup returned an unexpected payload')
        }
        return payload.data
      },
    }),
    getCurrentUser: builder.query<AuthUser, void>({
      query: () => ({
        url: '/api/auth/me',
        validateStatus: (_response, json) => isApiResponse(json, isAuthUser),
      }),
      transformResponse: (payload: unknown) => {
        if (!isApiResponse(payload, isAuthUser)) {
          throw new Error('/me returned an unexpected payload')
        }
        return payload.data
      },
    }),
  }),
})

export const { useLoginMutation, useSignupMutation, useGetCurrentUserQuery } = authApi
