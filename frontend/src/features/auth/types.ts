import { isRecord } from '../../types/api'

export type UserRole = 'ADMIN' | 'SALES_REP' | 'SALES_MANAGER' | 'FINANCE_OPS' | 'CUSTOMER'

export type AuthUser = {
  id: number
  name: string
  email: string
  role: UserRole
  companyId: number | null
  companyName: string | null
}

export type AuthSession = {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export type LoginRequest = {
  email: string
  password: string
}

export type SignupRequest = {
  companyName: string
  name: string
  email: string
  password: string
}

export type CustomerSignupRequest = {
  name: string
  email: string
  password: string
}

const ROLES: UserRole[] = ['ADMIN', 'SALES_REP', 'SALES_MANAGER', 'FINANCE_OPS', 'CUSTOMER']

export function isUserRole(value: unknown): value is UserRole {
  return typeof value === 'string' && (ROLES as string[]).includes(value)
}

export function canAccessQuotations(role: UserRole | undefined): boolean {
  return role === 'SALES_REP' || role === 'SALES_MANAGER' || role === 'FINANCE_OPS'
}

export function canWriteQuotations(role: UserRole | undefined): boolean {
  return role === 'SALES_REP'
}

export function isCustomerUser(role: UserRole | undefined): boolean {
  return role === 'CUSTOMER'
}

export function homePath(role: UserRole | undefined): string {
  if (isCustomerUser(role)) {
    return '/customer'
  }
  if (canAccessQuotations(role)) {
    return '/quotations'
  }
  return '/dashboard'
}

export function isAuthUser(value: unknown): value is AuthUser {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.id === 'number' &&
    typeof value.name === 'string' &&
    typeof value.email === 'string' &&
    isUserRole(value.role) &&
    (value.companyId === null || typeof value.companyId === 'number') &&
    (value.companyName === null || typeof value.companyName === 'string')
  )
}

export function isAuthSession(value: unknown): value is AuthSession {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.accessToken === 'string' &&
    typeof value.tokenType === 'string' &&
    typeof value.expiresIn === 'number' &&
    isAuthUser(value.user)
  )
}
