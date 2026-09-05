import { isApiResponse, isRecord, type ApiResponse } from './api'

export type { ApiResponse }
export { isApiResponse }

export type HealthStatus = 'loading' | 'ok' | 'error'

export type HealthData = {
  backend: string
  database: string
}

export type HealthState =
  | { status: 'loading' }
  | { status: 'ok'; data: HealthData; timestamp: string }
  | { status: 'error'; message: string; data?: HealthData; timestamp?: string }

export function isHealthData(value: unknown): value is HealthData {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.backend === 'string' && typeof value.database === 'string'
}

export function isHealthy(data: HealthData): boolean {
  return data.backend === 'up' && data.database === 'up'
}
