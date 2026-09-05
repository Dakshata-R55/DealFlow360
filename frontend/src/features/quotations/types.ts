import { isRecord } from '../../types/api'
import type { BillingType } from '../admin/types'

export type QuotationStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'NEGOTIATION'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'CANCELLED'

export type RiskLevel = 'NONE' | 'MEDIUM' | 'HIGH'

export type Customer = {
  id: number
  name: string
  customerTierId: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export type LikelyRoute = {
  requiresManager: boolean
  requiresFinance: boolean
}

export type QuotationLine = {
  id: number
  productId: number
  productName: string
  variantId: number | null
  variantLabel: string | null
  quantity: number
  baseUnitPrice: number
  resolvedUnitPrice: number
  costPrice: number
  discountPercent: number
  discountAmount: number
  allowedDiscountPercent: number
  excess: number
  lineTotal: number
  marginAmount: number
  marginPercent: number
  billingType: BillingType
  customerExpectedDiscountPercent: number | null
  customerExpectedIsDefault: boolean
}

export type Quotation = {
  id: number
  quoteNumber: string
  customerId: number
  customerName: string
  customerTierId: number
  customerTierName: string
  salesRepId: number
  salesRepName: string
  priceListId: number
  priceListName: string
  status: QuotationStatus
  subtotal: number
  discountAmount: number
  totalAmount: number
  totalCost: number
  marginAmount: number
  marginPercent: number
  riskScore: number
  riskLevel: RiskLevel
  maxLineExcess: number
  likelyRoute: LikelyRoute
  createdAt: string
  updatedAt: string
  submittedAt: string | null
  managerApprovedAt: string | null
  financeApprovedAt: string | null
  lines: QuotationLine[]
  sourceRequestNumber: string | null
  customerExpectedDiscountPercent: number | null
}

export type Recommendation = {
  productId: number
  productName: string
  promotion: boolean
  marginDelta: number
  score: number
}

const STATUSES: QuotationStatus[] = [
  'DRAFT',
  'PENDING_APPROVAL',
  'APPROVED',
  'NEGOTIATION',
  'CONFIRMED',
  'REJECTED',
  'CANCELLED',
]

const RISK_LEVELS: RiskLevel[] = ['NONE', 'MEDIUM', 'HIGH']

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function isIso(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

export function isCustomer(value: unknown): value is Customer {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    isNumber(value.customerTierId) &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

function isLikelyRoute(value: unknown): value is LikelyRoute {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.requiresManager === 'boolean' && typeof value.requiresFinance === 'boolean'
}

function isQuotationLine(value: unknown): value is QuotationLine {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.productId) &&
    typeof value.productName === 'string' &&
    (value.variantId === null || isNumber(value.variantId)) &&
    (value.variantLabel === null || typeof value.variantLabel === 'string') &&
    isNumber(value.quantity) &&
    isNumber(value.baseUnitPrice) &&
    isNumber(value.resolvedUnitPrice) &&
    isNumber(value.costPrice) &&
    isNumber(value.discountPercent) &&
    isNumber(value.discountAmount) &&
    isNumber(value.allowedDiscountPercent) &&
    isNumber(value.excess) &&
    isNumber(value.lineTotal) &&
    isNumber(value.marginAmount) &&
    isNumber(value.marginPercent) &&
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING') &&
    (value.customerExpectedDiscountPercent === null || isNumber(value.customerExpectedDiscountPercent)) &&
    typeof value.customerExpectedIsDefault === 'boolean'
  )
}

export function isQuotation(value: unknown): value is Quotation {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.quoteNumber === 'string' &&
    isNumber(value.customerId) &&
    typeof value.customerName === 'string' &&
    isNumber(value.customerTierId) &&
    typeof value.customerTierName === 'string' &&
    isNumber(value.salesRepId) &&
    typeof value.salesRepName === 'string' &&
    isNumber(value.priceListId) &&
    typeof value.priceListName === 'string' &&
    typeof value.status === 'string' &&
    STATUSES.includes(value.status as QuotationStatus) &&
    isNumber(value.subtotal) &&
    isNumber(value.discountAmount) &&
    isNumber(value.totalAmount) &&
    isNumber(value.totalCost) &&
    isNumber(value.marginAmount) &&
    isNumber(value.marginPercent) &&
    isNumber(value.riskScore) &&
    typeof value.riskLevel === 'string' &&
    RISK_LEVELS.includes(value.riskLevel as RiskLevel) &&
    isNumber(value.maxLineExcess) &&
    isLikelyRoute(value.likelyRoute) &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt) &&
    (value.submittedAt === null || isIso(value.submittedAt)) &&
    (value.managerApprovedAt === null || isIso(value.managerApprovedAt)) &&
    (value.financeApprovedAt === null || isIso(value.financeApprovedAt)) &&
    Array.isArray(value.lines) &&
    value.lines.every(isQuotationLine) &&
    (value.sourceRequestNumber === null || typeof value.sourceRequestNumber === 'string') &&
    (value.customerExpectedDiscountPercent === null || isNumber(value.customerExpectedDiscountPercent))
  )
}

export function isRecommendation(value: unknown): value is Recommendation {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.productId) &&
    typeof value.productName === 'string' &&
    typeof value.promotion === 'boolean' &&
    isNumber(value.marginDelta) &&
    isNumber(value.score)
  )
}

export function isListOf<T>(isItem: (value: unknown) => value is T) {
  return (value: unknown): value is T[] => Array.isArray(value) && value.every(isItem)
}

export const PIPELINE_COLUMNS: Array<{ status: QuotationStatus; label: string }> = [
  { status: 'DRAFT', label: 'Draft' },
  { status: 'PENDING_APPROVAL', label: 'Pending Approval' },
  { status: 'APPROVED', label: 'Approved' },
  { status: 'NEGOTIATION', label: 'Negotiation' },
  { status: 'CONFIRMED', label: 'Confirmed' },
]

export function routeLabel(route: LikelyRoute): string {
  if (route.requiresManager && route.requiresFinance) {
    return 'Sales Manager → Finance'
  }
  if (route.requiresManager) {
    return 'Sales Manager'
  }
  if (route.requiresFinance) {
    return 'Finance'
  }
  return 'None'
}

export function percent(value: number): string {
  return `${Number(value.toFixed(2))}%`
}

export function money(value: number): string {
  return value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export type SalesUser = {
  id: number
  name: string
  email: string
  role: 'SALES_REP' | 'SALES_MANAGER' | 'FINANCE_OPS'
}

const SALES_ROLES = ['SALES_REP', 'SALES_MANAGER', 'FINANCE_OPS'] as const

export function isSalesUser(value: unknown): value is SalesUser {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.email === 'string' &&
    typeof value.role === 'string' &&
    (SALES_ROLES as readonly string[]).includes(value.role)
  )
}

export function statusLabel(status: QuotationStatus): string {
  const column = PIPELINE_COLUMNS.find((item) => item.status === status)
  return column ? column.label : status.replaceAll('_', ' ')
}

export type QuoteStatusAction =
  | 'submit'
  | 'reopen'
  | 'negotiate'
  | 'approve'
  | 'returnToQueue'
  | 'returnToPending'

export type StatusOption = {
  action: QuoteStatusAction
  label: string
}

export function legalStatusOptions(args: {
  status: QuotationStatus
  canSubmit: boolean
  role: string | undefined
  riskLevel: RiskLevel
  managerApprovedAt: string | null
  hasSourceRequest?: boolean
}): StatusOption[] {
  const { status, canSubmit, role, riskLevel, managerApprovedAt, hasSourceRequest } = args
  const isApprover = role === 'SALES_MANAGER' || role === 'FINANCE_OPS'
  if (status === 'DRAFT') {
    const options: StatusOption[] = []
    if (canSubmit) {
      options.push({ action: 'submit', label: 'Submit for approval' })
      if (hasSourceRequest) {
        options.push({ action: 'returnToQueue', label: 'To do' })
      }
    }
    return options
  }
  if (status === 'PENDING_APPROVAL') {
    const options: StatusOption[] = []
    if (canSubmit) {
      options.push({ action: 'reopen', label: 'Draft' })
    }
    if (isApprover) {
      options.push({ action: 'negotiate', label: 'Negotiation' })
    }
    if (role === 'SALES_MANAGER' && !managerApprovedAt) {
      options.push({ action: 'approve', label: riskLevel === 'HIGH' ? 'Stamp for Finance' : 'Approve' })
    }
    if (role === 'FINANCE_OPS' && riskLevel === 'HIGH' && managerApprovedAt) {
      options.push({ action: 'approve', label: 'Approve' })
    }
    return options
  }
  if (status === 'APPROVED') {
    const options: StatusOption[] = []
    if (canSubmit) {
      options.push({ action: 'reopen', label: 'Draft' })
      options.push({ action: 'returnToPending', label: 'Pending Approval' })
    }
    if (isApprover) {
      options.push({ action: 'negotiate', label: 'Negotiation' })
    }
    return options
  }
  if (status === 'NEGOTIATION') {
    const options: StatusOption[] = []
    if (canSubmit) {
      options.push({ action: 'reopen', label: 'Draft' })
      options.push({ action: 'returnToPending', label: 'Pending Approval' })
    }
    if (isApprover) {
      options.push({ action: 'approve', label: 'Approve' })
    }
    return options
  }
  return []
}

export type SellerBoardColumn = 'TODO' | QuotationStatus

export function quoteDropAction(
  fromStatus: QuotationStatus,
  toColumn: SellerBoardColumn,
  options: StatusOption[],
): QuoteStatusAction | 'noop' | null {
  if (toColumn === fromStatus) {
    return 'noop'
  }
  if (toColumn === 'TODO') {
    return options.some((item) => item.action === 'returnToQueue') ? 'returnToQueue' : null
  }
  if (toColumn === 'CONFIRMED' || toColumn === 'REJECTED' || toColumn === 'CANCELLED') {
    return null
  }
  const action: QuoteStatusAction | null =
    toColumn === 'DRAFT'
      ? 'reopen'
      : toColumn === 'PENDING_APPROVAL'
        ? fromStatus === 'DRAFT'
          ? 'submit'
          : 'returnToPending'
        : toColumn === 'NEGOTIATION'
          ? 'negotiate'
          : toColumn === 'APPROVED'
            ? 'approve'
            : null
  if (!action) {
    return null
  }
  const option = options.find((item) => item.action === action)
  if (!option) {
    return null
  }
  if (action === 'approve' && option.label !== 'Approve') {
    return null
  }
  return action
}

export function requestDropAction(toColumn: SellerBoardColumn, canConvert: boolean): 'convert' | 'noop' | null {
  if (toColumn === 'TODO') {
    return 'noop'
  }
  if (toColumn === 'DRAFT' && canConvert) {
    return 'convert'
  }
  return null
}
