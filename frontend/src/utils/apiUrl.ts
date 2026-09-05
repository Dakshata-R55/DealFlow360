export function apiBaseUrl(): string {
  return (import.meta.env.VITE_API_URL ?? 'http://127.0.0.1:18080').replace(/\/$/, '')
}

export function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${apiBaseUrl()}${normalizedPath}`
}
