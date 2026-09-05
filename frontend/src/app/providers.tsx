import type { ReactNode } from 'react'
import { Provider } from 'react-redux'
import { AuthBootstrap } from '../features/auth/AuthBootstrap'
import { store } from '../stores'

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <Provider store={store}>
      <AuthBootstrap>{children}</AuthBootstrap>
    </Provider>
  )
}
