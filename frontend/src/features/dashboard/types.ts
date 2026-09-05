import { isListOf } from '../admin/types'
import { isRecord } from '../../types/api'

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

export type DashboardKpi = { label: string; value: string; href: string }
export type DashboardBar = { label: string; count: number; href: string }
export type DashboardActivity = { title: string; subtitle: string; href: string; at: string }
export type DashboardTableRow = {
  idLabel: string
  primary: string
  secondary: string
  amount: string
  status: string
  href: string
  updatedAt: string
}
export type DashboardAction = { label: string; href: string }

export type Dashboard = {
  greeting: string
  subtitle: string
  primaryCtaLabel: string
  primaryCtaHref: string
  kpis: DashboardKpi[]
  chartTitle: string
  bars: DashboardBar[]
  activity: DashboardActivity[]
  tableTitle: string
  table: DashboardTableRow[]
  actions: DashboardAction[]
}

export type SearchHit = { kind: string; id: number; label: string; href: string }

export function isDashboard(value: unknown): value is Dashboard {
  if (!isRecord(value)) {
    return false
  }
  return (
    typeof value.greeting === 'string' &&
    typeof value.subtitle === 'string' &&
    typeof value.primaryCtaLabel === 'string' &&
    typeof value.primaryCtaHref === 'string' &&
    Array.isArray(value.kpis) &&
    Array.isArray(value.bars) &&
    Array.isArray(value.activity) &&
    Array.isArray(value.table) &&
    Array.isArray(value.actions) &&
    typeof value.chartTitle === 'string' &&
    typeof value.tableTitle === 'string'
  )
}

function isSearchHit(value: unknown): value is SearchHit {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.kind === 'string' && isNumber(value.id) && typeof value.label === 'string' && typeof value.href === 'string'
}

export const isSearchHitList = isListOf(isSearchHit)
