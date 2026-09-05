import { createSlice, type PayloadAction } from '@reduxjs/toolkit'
import { clearAccessToken, readAccessToken, writeAccessToken } from './tokenStorage'
import type { AuthUser } from './types'

export type AuthState = {
  user: AuthUser | null
  accessToken: string | null
  isAuthenticated: boolean
  hydrated: boolean
}

const initialState: AuthState = {
  user: null,
  accessToken: readAccessToken(),
  isAuthenticated: false,
  hydrated: false,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action: PayloadAction<{ user: AuthUser; accessToken: string }>) {
      state.user = action.payload.user
      state.accessToken = action.payload.accessToken
      state.isAuthenticated = true
      writeAccessToken(action.payload.accessToken)
    },
    logout(state) {
      state.user = null
      state.accessToken = null
      state.isAuthenticated = false
      clearAccessToken()
    },
    markHydrated(state) {
      state.hydrated = true
    },
  },
})

export const { setCredentials, logout, markHydrated } = authSlice.actions
export const authReducer = authSlice.reducer
