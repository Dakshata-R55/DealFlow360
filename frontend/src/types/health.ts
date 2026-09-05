export type HealthStatus = 'loading' | 'ok' | 'error'

export type ApiResponse<T> = {
  success: boolean
  status: number
  data: T
  timestamp: string
}

export type HealthData = {
  backend: string
  database: string
}

export type HealthState =
  | { status: 'loading' }
  | { status: 'ok'; data: HealthData; timestamp: string }
  | { status: 'error'; message: string; data?: HealthData; timestamp?: string }

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

export function isApiResponse<T>(
  value: unknown,
  isData: (data: unknown) => data is T,
): value is ApiResponse<T> {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.success === 'boolean' &&
    typeof value.status === 'number' &&
    typeof value.timestamp === 'string' &&
    isData(value.data)
  )
}

export function isHealthData(value: unknown): value is HealthData {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.backend === 'string' && typeof value.database === 'string'
}

export function isHealthy(data: HealthData): boolean {
  return data.backend === 'up' && data.database === 'up'
}
