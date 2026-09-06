import { isRecord } from '../../types/api'
import { isUserRole, type UserRole } from '../../features/auth/types'

export type TeamUser = {
  id: number
  name: string
  email: string
  role: UserRole
  active: boolean
}

export type CreatableTeamRole = 'SALES_REP' | 'SALES_MANAGER' | 'FINANCE_OPS'

export type CreateTeamUserBody = {
  name: string
  email: string
  password: string
  role: CreatableTeamRole
}

export type PatchTeamUserBody = {
  active: boolean
}

export function isTeamUser(value: unknown): value is TeamUser {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.id === 'number' &&
    typeof value.name === 'string' &&
    typeof value.email === 'string' &&
    isUserRole(value.role) &&
    typeof value.active === 'boolean'
  )
}

export function isTeamUserList(value: unknown): value is TeamUser[] {
  return Array.isArray(value) && value.every(isTeamUser)
}

export const CREATABLE_TEAM_ROLES: CreatableTeamRole[] = ['SALES_REP', 'SALES_MANAGER', 'FINANCE_OPS']

export function teamRoleLabel(role: UserRole): string {
  switch (role) {
    case 'ADMIN':
      return 'Admin'
    case 'SALES_REP':
      return 'Sales Rep'
    case 'SALES_MANAGER':
      return 'Sales Manager'
    case 'FINANCE_OPS':
      return 'Finance Ops'
    default:
      return role
  }
}
