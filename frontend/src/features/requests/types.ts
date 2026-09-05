import { isRecord } from '../../types/api'
import type { BillingType } from '../admin/types'
import { isListOf, type QuotationStatus } from '../quotations/types'

export type SellerCompany = {
  id: number
  name: string
  code: string
  description: string
  categories: string[]
}

export type PublicProduct = {
  id: number
  name: string
  categoryName: string
  description: string
  unit: string
  indicativePrice: number
  categoryDiscountPercent: number
  billingType: BillingType
  active: boolean
}

export type QuoteRequestStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'QUOTED'
  | 'CANCELLED'
  | 'CLOSED'

export type QuoteRequestLine = {
  id: number
  productId: number
  productName: string
  categoryName: string
  unit: string
  billingType: BillingType
  quantity: number
  notes: string
  mrp: number
  lineMrp: number
  categoryDiscountPercent: number
  standingDiscountPercent: number
  availableDiscountPercent: number
  expectedDiscountPercent: number
  independentExpected: boolean
  appliedExpectedPercent: number
  indicativeUnitPrice: number
  indicativeLineTotal: number
  expectedUnitPrice: number
  expectedLineTotal: number
}

export type QuoteRequest = {
  id: number
  requestNumber: string
  sellerCompanyId: number
  sellerCompanyName: string
  customerUserId: number
  customerName: string
  status: QuoteRequestStatus
  statusLabel: string
  requestedDeliveryDate: string | null
  expectedDiscountPercent: number | null
  notes: string
  quotationId: number | null
  createdAt: string
  updatedAt: string
  submittedAt: string | null
  customerTierName: string
  catalogMrpTotal: number
  indicativeTotal: number
  expectedTotal: number
  quotationStatus: QuotationStatus | null
  quotationTotal: number | null
  lines: QuoteRequestLine[]
}

const STATUSES: QuoteRequestStatus[] = [
  'DRAFT',
  'SUBMITTED',
  'UNDER_REVIEW',
  'QUOTED',
  'CANCELLED',
  'CLOSED',
]

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function isIso(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

export function isSellerCompany(value: unknown): value is SellerCompany {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.code === 'string' &&
    typeof value.description === 'string' &&
    Array.isArray(value.categories) &&
    value.categories.every((item) => typeof item === 'string')
  )
}

export function isPublicProduct(value: unknown): value is PublicProduct {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.categoryName === 'string' &&
    typeof value.description === 'string' &&
    typeof value.unit === 'string' &&
    isNumber(value.indicativePrice) &&
    isNumber(value.categoryDiscountPercent) &&
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING') &&
    typeof value.active === 'boolean'
  )
}

function isQuoteRequestLine(value: unknown): value is QuoteRequestLine {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.productId) &&
    typeof value.productName === 'string' &&
    typeof value.categoryName === 'string' &&
    typeof value.unit === 'string' &&
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING') &&
    isNumber(value.quantity) &&
    typeof value.notes === 'string' &&
    isNumber(value.mrp) &&
    isNumber(value.lineMrp) &&
    isNumber(value.categoryDiscountPercent) &&
    isNumber(value.standingDiscountPercent) &&
    isNumber(value.availableDiscountPercent) &&
    isNumber(value.expectedDiscountPercent) &&
    typeof value.independentExpected === 'boolean' &&
    isNumber(value.appliedExpectedPercent) &&
    isNumber(value.indicativeUnitPrice) &&
    isNumber(value.indicativeLineTotal) &&
    isNumber(value.expectedUnitPrice) &&
    isNumber(value.expectedLineTotal)
  )
}

export function isQuoteRequest(value: unknown): value is QuoteRequest {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.requestNumber === 'string' &&
    isNumber(value.sellerCompanyId) &&
    typeof value.sellerCompanyName === 'string' &&
    isNumber(value.customerUserId) &&
    typeof value.customerName === 'string' &&
    typeof value.status === 'string' &&
    STATUSES.includes(value.status as QuoteRequestStatus) &&
    typeof value.statusLabel === 'string' &&
    (value.requestedDeliveryDate === null || typeof value.requestedDeliveryDate === 'string') &&
    (value.expectedDiscountPercent === null || isNumber(value.expectedDiscountPercent)) &&
    typeof value.notes === 'string' &&
    (value.quotationId === null || isNumber(value.quotationId)) &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt) &&
    (value.submittedAt === null || isIso(value.submittedAt)) &&
    typeof value.customerTierName === 'string' &&
    isNumber(value.catalogMrpTotal) &&
    isNumber(value.indicativeTotal) &&
    isNumber(value.expectedTotal) &&
    (value.quotationStatus === null ||
      value.quotationStatus === 'DRAFT' ||
      value.quotationStatus === 'PENDING_APPROVAL' ||
      value.quotationStatus === 'APPROVED' ||
      value.quotationStatus === 'NEGOTIATION' ||
      value.quotationStatus === 'CONFIRMED' ||
      value.quotationStatus === 'REJECTED' ||
      value.quotationStatus === 'CANCELLED') &&
    (value.quotationTotal === null || isNumber(value.quotationTotal)) &&
    Array.isArray(value.lines) &&
    value.lines.every(isQuoteRequestLine)
  )
}

export const isSellerCompanyList = isListOf(isSellerCompany)
export const isPublicProductList = isListOf(isPublicProduct)
export const isQuoteRequestList = isListOf(isQuoteRequest)

export function priceAmountLabel(product: PublicProduct): string {
  const amount = product.indicativePrice.toLocaleString('en-IN', {
    maximumFractionDigits: 0,
  })
  if (product.billingType === 'RECURRING') {
    return `₹${amount} / month`
  }
  return `₹${amount}`
}

export function priceLabel(product: PublicProduct): string {
  return `MRP ${priceAmountLabel(product)}`
}

export function rupee(value: number): string {
  return `₹${value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

export function percentLabel(value: number): string {
  return `${Number(value.toFixed(2))}%`
}

export type HomeBucket = 'active' | 'awaiting' | 'orders'

export function homeBucket(request: QuoteRequest): HomeBucket | null {
  if (request.status === 'CANCELLED' || request.status === 'CLOSED') {
    return null
  }
  const quoteStatus = request.quotationStatus
  if (quoteStatus === 'CONFIRMED') {
    return 'orders'
  }
  if (quoteStatus === 'NEGOTIATION' || quoteStatus === 'APPROVED') {
    return 'awaiting'
  }
  if (
    request.status === 'DRAFT' ||
    request.status === 'SUBMITTED' ||
    request.status === 'UNDER_REVIEW' ||
    quoteStatus === 'DRAFT' ||
    quoteStatus === 'PENDING_APPROVAL'
  ) {
    return 'active'
  }
  return null
}

export function activeRequestLabel(request: QuoteRequest): string {
  if (request.quotationStatus === 'PENDING_APPROVAL') {
    return 'Pending Approval'
  }
  if (request.quotationStatus === 'DRAFT') {
    return 'Seller is working this'
  }
  return request.statusLabel
}

export type CustomerQuotationLine = {
  id: number
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  discountPercent: number
  lineTotal: number
  billingType: BillingType
}

export type CustomerQuotation = {
  id: number
  quoteNumber: string
  sellerCompanyId: number
  sellerCompanyName: string
  status: QuotationStatus
  statusLabel: string
  subtotal: number
  discountAmount: number
  totalAmount: number
  sourceRequestId: number
  sourceRequestNumber: string
  expectedDiscountPercent: number | null
  shipQty: number
  backorderQty: number
  shipFrom: string[]
  lines: CustomerQuotationLine[]
}

function isCustomerQuotationLine(value: unknown): value is CustomerQuotationLine {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.productId) &&
    typeof value.productName === 'string' &&
    isNumber(value.quantity) &&
    isNumber(value.unitPrice) &&
    isNumber(value.discountPercent) &&
    isNumber(value.lineTotal) &&
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING')
  )
}

export function isCustomerQuotation(value: unknown): value is CustomerQuotation {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.quoteNumber === 'string' &&
    isNumber(value.sellerCompanyId) &&
    typeof value.sellerCompanyName === 'string' &&
    typeof value.status === 'string' &&
    typeof value.statusLabel === 'string' &&
    isNumber(value.subtotal) &&
    isNumber(value.discountAmount) &&
    isNumber(value.totalAmount) &&
    isNumber(value.sourceRequestId) &&
    typeof value.sourceRequestNumber === 'string' &&
    (value.expectedDiscountPercent === null || isNumber(value.expectedDiscountPercent)) &&
    isNumber(value.shipQty) &&
    isNumber(value.backorderQty) &&
    Array.isArray(value.shipFrom) &&
    value.shipFrom.every((name) => typeof name === 'string') &&
    Array.isArray(value.lines) &&
    value.lines.every(isCustomerQuotationLine)
  )
}

export const isCustomerQuotationList = isListOf(isCustomerQuotation)
