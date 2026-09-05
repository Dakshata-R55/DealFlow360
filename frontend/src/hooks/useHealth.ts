import { useGetHealthQuery } from '../stores/api/healthApi'
import { isHealthy, type HealthState } from '../types/health'

function errorMessage(error: unknown): string {
  if (typeof error === 'object' && error && 'status' in error) {
    const serialized = error as { status?: number | string; data?: unknown }
    if (typeof serialized.status === 'number') {
      return `Health check failed (${serialized.status})`
    }
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Unknown error'
}

export function useHealth(): { state: HealthState; reload: () => void } {
  const { data, error, isFetching, refetch } = useGetHealthQuery()

  const state: HealthState = (() => {
    if (isFetching) {
      return { status: 'loading' }
    }
    if (data) {
      if (data.success && isHealthy(data.data)) {
        return { status: 'ok', data: data.data, timestamp: data.timestamp }
      }
      return {
        status: 'error',
        data: data.data,
        timestamp: data.timestamp,
        message: `Backend ${data.data.backend}, database ${data.data.database}.`,
      }
    }
    if (error) {
      return { status: 'error', message: errorMessage(error) }
    }
    return { status: 'loading' }
  })()

  return { state, reload: () => { void refetch() } }
}
