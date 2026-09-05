import { isListOf } from '../admin/types'
import { isRecord } from '../../types/api'

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

export type FulfillmentAllocation = {
  id: number
  quotationLineId: number
  warehouseId: number | null
  warehouseName: string | null
  quantity: number
  kind: 'SHIP' | 'BACKORDER'
  source: 'AUTO' | 'OVERRIDE'
  available: number | null
}

export type FulfillmentLine = {
  lineId: number
  productId: number
  productName: string
  quantity: number
  billingType: string
  allocations: FulfillmentAllocation[]
}

export type FulfillmentPlan = {
  quotationId: number
  quoteNumber: string
  customerName: string
  status: string
  shipQty: number
  backorderQty: number
  warehouses: string[]
  lines: FulfillmentLine[]
}

export type FulfillmentListItem = {
  quotationId: number
  quoteNumber: string
  customerName: string
  shipQty: number
  backorderQty: number
  warehouses: string[]
}

function isAllocation(value: unknown): value is FulfillmentAllocation {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.id) &&
    isNumber(value.quotationLineId) &&
    (value.warehouseId === null || isNumber(value.warehouseId)) &&
    (value.warehouseName === null || typeof value.warehouseName === 'string') &&
    isNumber(value.quantity) &&
    (value.kind === 'SHIP' || value.kind === 'BACKORDER') &&
    (value.source === 'AUTO' || value.source === 'OVERRIDE') &&
    (value.available === null || isNumber(value.available))
  )
}

function isLine(value: unknown): value is FulfillmentLine {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.lineId) &&
    isNumber(value.productId) &&
    typeof value.productName === 'string' &&
    isNumber(value.quantity) &&
    typeof value.billingType === 'string' &&
    Array.isArray(value.allocations) &&
    value.allocations.every(isAllocation)
  )
}

export function isFulfillmentPlan(value: unknown): value is FulfillmentPlan {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.quotationId) &&
    typeof value.quoteNumber === 'string' &&
    typeof value.customerName === 'string' &&
    typeof value.status === 'string' &&
    isNumber(value.shipQty) &&
    isNumber(value.backorderQty) &&
    Array.isArray(value.warehouses) &&
    value.warehouses.every((name) => typeof name === 'string') &&
    Array.isArray(value.lines) &&
    value.lines.every(isLine)
  )
}

export function isFulfillmentListItem(value: unknown): value is FulfillmentListItem {
  if (!isRecord(value)) {
    return false
  }
  return (
    isNumber(value.quotationId) &&
    typeof value.quoteNumber === 'string' &&
    typeof value.customerName === 'string' &&
    isNumber(value.shipQty) &&
    isNumber(value.backorderQty) &&
    Array.isArray(value.warehouses) &&
    value.warehouses.every((name) => typeof name === 'string')
  )
}

export const isFulfillmentList = isListOf(isFulfillmentListItem)
