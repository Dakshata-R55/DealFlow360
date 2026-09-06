import { isTeamUser, isTeamUserList, type CreateTeamUserBody, type PatchTeamUserBody, type TeamUser } from '../../features/admin/teamUserTypes'
import { isApiResponse } from '../../types/api'
import { baseApi } from './baseApi'

function unwrapTeamUser(payload: unknown): TeamUser {
  if (!isApiResponse(payload, isTeamUser)) {
    throw new Error('Team user returned an unexpected payload')
  }
  return payload.data
}

function unwrapTeamUserList(payload: unknown): TeamUser[] {
  if (!isApiResponse(payload, isTeamUserList)) {
    throw new Error('Team users returned an unexpected payload')
  }
  return payload.data
}

export const adminUsersApi = baseApi
  .enhanceEndpoints({
    addTagTypes: ['TeamUser'],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getTeamUsers: builder.query<TeamUser[], void>({
        query: () => ({
          url: '/api/admin/users',
          validateStatus: (_response, json) => isApiResponse(json, isTeamUserList),
        }),
        transformResponse: unwrapTeamUserList,
        providesTags: ['TeamUser'],
      }),
      createTeamUser: builder.mutation<TeamUser, CreateTeamUserBody>({
        query: (body) => ({
          url: '/api/admin/users',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isTeamUser),
        }),
        transformResponse: unwrapTeamUser,
        invalidatesTags: ['TeamUser'],
      }),
      patchTeamUser: builder.mutation<TeamUser, { id: number; body: PatchTeamUserBody }>({
        query: ({ id, body }) => ({
          url: `/api/admin/users/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isTeamUser),
        }),
        transformResponse: unwrapTeamUser,
        invalidatesTags: ['TeamUser'],
      }),
    }),
  })

export const { useGetTeamUsersQuery, useCreateTeamUserMutation, usePatchTeamUserMutation } = adminUsersApi
