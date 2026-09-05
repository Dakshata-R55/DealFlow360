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
}

export type Quotation = {
  id: number
  quoteNumber: string
  customerId: number
  customerName: string
  customerTierId: number
  customerTierName: string
  salesRepId: number
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
  likelyRoute: LikelyRoute
  createdAt: string
  updatedAt: string
  submittedAt: string | null
  lines: QuotationLine[]
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
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING')
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
    isLikelyRoute(value.likelyRoute) &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt) &&
    (value.submittedAt === null || isIso(value.submittedAt)) &&
    Array.isArray(value.lines) &&
    value.lines.every(isQuotationLine)
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

export function money(value: number): string {
  return value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
