import { isRecord } from '../../types/api'

export type BillingType = 'ONE_TIME' | 'RECURRING'
export type PlanCycle = 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
export type ProrationRule = 'PRORATE_DAYS' | 'CHARGE_FULL'
export type CancellationRule = 'CREDIT_NOTE' | 'REFUND' | 'FORFEIT'
export type RiskLevel = 'NONE' | 'MEDIUM' | 'HIGH'

export type Category = {
  id: number
  name: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export type ProductVariant = {
  id: number
  productId: number
  attributeName: string
  attributeValue: string
  extraPrice: number
  createdAt: string
  updatedAt: string
}

export type Product = {
  id: number
  categoryId: number
  name: string
  description: string
  unit: string
  basePrice: number
  costPrice: number
  taxPercent: number
  billingType: BillingType
  active: boolean
  createdAt: string
  updatedAt: string
  variants: ProductVariant[]
}

export type CustomerTier = {
  id: number
  name: string
  defaultDiscountLimit: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export type PriceListItem = {
  priceListId: number
  productId: number
  price: number
}

export type PriceList = {
  id: number
  name: string
  currency: string
  customerTierId: number
  active: boolean
  items: PriceListItem[]
}

export type DiscountPolicy = {
  id: number
  customerTierId: number | null
  categoryId: number | null
  maxDiscountPct: number
}

export type ApprovalPolicy = {
  id: number
  managerLineExcessPercent: number
  financeLineExcessPercent: number
  managerQuoteExcessPercent: number
  financeQuoteExcessPercent: number
}

export type Warehouse = {
  id: number
  name: string
  location: string
  shippingCostWeight: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export type Inventory = {
  warehouseId: number
  productId: number
  onHand: number
  reserved: number
  available: number
  minStock: number
  reorderQty: number
}

export type SubscriptionPlan = {
  id: number
  name: string
  cycle: PlanCycle
  prorationRule: ProrationRule
  cancellationRule: CancellationRule
  active: boolean
  createdAt: string
  updatedAt: string
}

export type UpsellRule = {
  id: number
  triggerProductId: number
  suggestedProductId: number
  score: number
  promotionBoost: number
  minMarginPct: number
  active: boolean
  createdAt: string
  updatedAt: string
}

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function isIso(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

export function isCategory(value: unknown): value is Category {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isProductVariant(value: unknown): value is ProductVariant {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.productId) &&
    typeof value.attributeName === 'string' &&
    typeof value.attributeValue === 'string' &&
    isNumber(value.extraPrice) &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isProduct(value: unknown): value is Product {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.categoryId) &&
    typeof value.name === 'string' &&
    typeof value.description === 'string' &&
    typeof value.unit === 'string' &&
    isNumber(value.basePrice) &&
    isNumber(value.costPrice) &&
    isNumber(value.taxPercent) &&
    (value.billingType === 'ONE_TIME' || value.billingType === 'RECURRING') &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt) &&
    Array.isArray(value.variants) &&
    value.variants.every(isProductVariant)
  )
}

export function isCustomerTier(value: unknown): value is CustomerTier {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    isNumber(value.defaultDiscountLimit) &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isPriceListItem(value: unknown): value is PriceListItem {
  if (!isRecord(value)) {
    return false
  }
  return isNumber(value.priceListId) && isNumber(value.productId) && isNumber(value.price)
}

export function isPriceList(value: unknown): value is PriceList {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.currency === 'string' &&
    isNumber(value.customerTierId) &&
    typeof value.active === 'boolean' &&
    Array.isArray(value.items) &&
    value.items.every(isPriceListItem)
  )
}

export function isDiscountPolicy(value: unknown): value is DiscountPolicy {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    (value.customerTierId === null || isNumber(value.customerTierId)) &&
    (value.categoryId === null || isNumber(value.categoryId)) &&
    isNumber(value.maxDiscountPct)
  )
}

export function isApprovalPolicy(value: unknown): value is ApprovalPolicy {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.managerLineExcessPercent) &&
    isNumber(value.financeLineExcessPercent) &&
    isNumber(value.managerQuoteExcessPercent) &&
    isNumber(value.financeQuoteExcessPercent)
  )
}

export function isWarehouse(value: unknown): value is Warehouse {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    typeof value.location === 'string' &&
    isNumber(value.shippingCostWeight) &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isInventory(value: unknown): value is Inventory {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.warehouseId) &&
    isNumber(value.productId) &&
    isNumber(value.onHand) &&
    isNumber(value.reserved) &&
    isNumber(value.available) &&
    isNumber(value.minStock) &&
    isNumber(value.reorderQty)
  )
}

export function isSubscriptionPlan(value: unknown): value is SubscriptionPlan {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    typeof value.name === 'string' &&
    (value.cycle === 'MONTHLY' || value.cycle === 'QUARTERLY' || value.cycle === 'YEARLY') &&
    (value.prorationRule === 'PRORATE_DAYS' || value.prorationRule === 'CHARGE_FULL') &&
    (value.cancellationRule === 'CREDIT_NOTE' ||
      value.cancellationRule === 'REFUND' ||
      value.cancellationRule === 'FORFEIT') &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isUpsellRule(value: unknown): value is UpsellRule {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.triggerProductId) &&
    isNumber(value.suggestedProductId) &&
    isNumber(value.score) &&
    isNumber(value.promotionBoost) &&
    isNumber(value.minMarginPct) &&
    typeof value.active === 'boolean' &&
    isIso(value.createdAt) &&
    isIso(value.updatedAt)
  )
}

export function isListOf<T>(isItem: (value: unknown) => value is T) {
  return (value: unknown): value is T[] => Array.isArray(value) && value.every(isItem)
}
