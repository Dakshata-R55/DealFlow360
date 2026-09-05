export type ApiResponse<T> = {
  success: boolean
  status: number
  data: T
  timestamp: string
}

export type ErrorData = {
  code: string
  message: string
  path: string
}

export function isRecord(value: unknown): value is Record<string, unknown> {
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

export function isErrorData(value: unknown): value is ErrorData {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.code === 'string' &&
    typeof value.message === 'string' &&
    typeof value.path === 'string'
  )
}

export function apiErrorMessage(error: unknown, fallback = 'Request failed'): string {
  if (isRecord(error) && 'data' in error) {
    const body = error.data
    if (isApiResponse(body, isErrorData)) {
      return body.data.message
    }
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}
