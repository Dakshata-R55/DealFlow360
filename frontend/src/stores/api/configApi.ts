import {
  isApprovalPolicy,
  isCategory,
  isCustomerTier,
  isDiscountPolicy,
  isInventory,
  isListOf,
  isPriceList,
  isPriceListItem,
  isProduct,
  isProductVariant,
  isStandingRule,
  isSubscriptionPlan,
  isUpsellRule,
  isWarehouse,
  type ApprovalPolicy,
  type Category,
  type CustomerTier,
  type DiscountPolicy,
  type Inventory,
  type PlanCycle,
  type ProrationRule,
  type CancellationRule,
  type PriceList,
  type PriceListItem,
  type Product,
  type ProductVariant,
  type StandingRule,
  type SubscriptionPlan,
  type UpsellRule,
  type Warehouse,
  type BillingType,
} from '../../features/admin/types'
import { isApiResponse } from '../../types/api'
import { baseApi } from './baseApi'

const isCategoryList = isListOf(isCategory)
const isProductList = isListOf(isProduct)
const isTierList = isListOf(isCustomerTier)
const isPriceListList = isListOf(isPriceList)
const isDiscountPolicyList = isListOf(isDiscountPolicy)
const isWarehouseList = isListOf(isWarehouse)
const isInventoryList = isListOf(isInventory)
const isPlanList = isListOf(isSubscriptionPlan)
const isUpsellRuleList = isListOf(isUpsellRule)

function unwrap<T>(isData: (value: unknown) => value is T, label: string) {
  return (payload: unknown): T => {
    if (!isApiResponse(payload, isData)) {
      throw new Error(`${label} returned an unexpected payload`)
    }
    return payload.data
  }
}

export type CategoryBody = {
  name: string
  active?: boolean
}

export type CreateProductBody = {
  categoryId: number
  name: string
  description?: string
  unit: string
  basePrice: number
  costPrice: number
  taxPercent: number
  billingType: BillingType
  active?: boolean
}

export type PatchProductBody = Partial<CreateProductBody>

export type VariantBody = {
  attributeName: string
  attributeValue: string
  extraPrice: number
}

export type CustomerTierBody = {
  name: string
  defaultDiscountLimit: number
  active?: boolean
}

export type PriceListBody = {
  name: string
  currency: string
  customerTierId: number
  active?: boolean
}

export type PriceListItemBody = {
  price: number
}

export type DiscountPolicyReplaceBody = {
  policies: Array<{
    customerTierId: number | null
    categoryId: number | null
    maxDiscountPct: number
  }>
}

export type ApprovalPolicyReplaceBody = {
  managerLineExcessPercent: number
  financeLineExcessPercent: number
  managerQuoteExcessPercent: number
  financeQuoteExcessPercent: number
}

export type WarehouseBody = {
  name: string
  location: string
  shippingCostWeight: number
  active?: boolean
}

export type InventoryBody = {
  onHand: number
  reserved?: number
  minStock: number
  reorderQty: number
}

export type SubscriptionPlanBody = {
  name: string
  cycle: PlanCycle
  prorationRule: ProrationRule
  cancellationRule: CancellationRule
  active?: boolean
}

export type UpsellRuleBody = {
  triggerProductId: number
  suggestedProductId: number
  score: number
  promotionBoost: number
  minMarginPct: number
  active?: boolean
}

export type StandingRuleBody = {
  silverMinSpend: number
  goldMinSpend: number
  windowMonths?: number
}

export const configApi = baseApi
  .enhanceEndpoints({
    addTagTypes: [
      'Category',
      'Product',
      'CustomerTier',
      'PriceList',
      'DiscountPolicy',
      'ApprovalPolicy',
      'Warehouse',
      'Inventory',
      'SubscriptionPlan',
      'UpsellRule',
      'StandingRule',
      'Dashboard',
    ],
  })
  .injectEndpoints({
    endpoints: (builder) => ({
      getCategories: builder.query<Category[], void>({
        query: () => ({
          url: '/api/categories',
          validateStatus: (_response, json) => isApiResponse(json, isCategoryList),
        }),
        transformResponse: unwrap(isCategoryList, 'GET /api/categories'),
        providesTags: ['Category'],
      }),
      createCategory: builder.mutation<Category, CategoryBody>({
        query: (body) => ({
          url: '/api/categories',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isCategory),
        }),
        transformResponse: unwrap(isCategory, 'POST /api/categories'),
        invalidatesTags: ['Category', 'Dashboard'],
      }),
      updateCategory: builder.mutation<Category, { id: number; body: CategoryBody }>({
        query: ({ id, body }) => ({
          url: `/api/categories/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isCategory),
        }),
        transformResponse: unwrap(isCategory, 'PATCH /api/categories'),
        invalidatesTags: ['Category', 'Dashboard'],
      }),
      getProducts: builder.query<Product[], void>({
        query: () => ({
          url: '/api/products',
          validateStatus: (_response, json) => isApiResponse(json, isProductList),
        }),
        transformResponse: unwrap(isProductList, 'GET /api/products'),
        providesTags: ['Product'],
      }),
      getProduct: builder.query<Product, number>({
        query: (id) => ({
          url: `/api/products/${id}`,
          validateStatus: (_response, json) => isApiResponse(json, isProduct),
        }),
        transformResponse: unwrap(isProduct, 'GET /api/products/{id}'),
        providesTags: (_result, _error, id) => [{ type: 'Product', id }],
      }),
      createProduct: builder.mutation<Product, CreateProductBody>({
        query: (body) => ({
          url: '/api/products',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isProduct),
        }),
        transformResponse: unwrap(isProduct, 'POST /api/products'),
        invalidatesTags: ['Product', 'Dashboard'],
      }),
      updateProduct: builder.mutation<Product, { id: number; body: PatchProductBody }>({
        query: ({ id, body }) => ({
          url: `/api/products/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isProduct),
        }),
        transformResponse: unwrap(isProduct, 'PATCH /api/products'),
        invalidatesTags: (_result, _error, { id }) => ['Product', { type: 'Product', id }, 'Dashboard'],
      }),
      createVariant: builder.mutation<ProductVariant, { productId: number; body: VariantBody }>({
        query: ({ productId, body }) => ({
          url: `/api/products/${productId}/variants`,
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isProductVariant),
        }),
        transformResponse: unwrap(isProductVariant, 'POST /api/products/{id}/variants'),
        invalidatesTags: (_result, _error, { productId }) => ['Product', { type: 'Product', id: productId }],
      }),
      updateVariant: builder.mutation<
        ProductVariant,
        { productId: number; variantId: number; body: VariantBody }
      >({
        query: ({ productId, variantId, body }) => ({
          url: `/api/products/${productId}/variants/${variantId}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isProductVariant),
        }),
        transformResponse: unwrap(isProductVariant, 'PATCH /api/products/{id}/variants'),
        invalidatesTags: (_result, _error, { productId }) => ['Product', { type: 'Product', id: productId }],
      }),
      getCustomerTiers: builder.query<CustomerTier[], void>({
        query: () => ({
          url: '/api/customer-tiers',
          validateStatus: (_response, json) => isApiResponse(json, isTierList),
        }),
        transformResponse: unwrap(isTierList, 'GET /api/customer-tiers'),
        providesTags: ['CustomerTier'],
      }),
      createCustomerTier: builder.mutation<CustomerTier, CustomerTierBody>({
        query: (body) => ({
          url: '/api/customer-tiers',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isCustomerTier),
        }),
        transformResponse: unwrap(isCustomerTier, 'POST /api/customer-tiers'),
        invalidatesTags: ['CustomerTier', 'PriceList', 'DiscountPolicy'],
      }),
      getPriceLists: builder.query<PriceList[], void>({
        query: () => ({
          url: '/api/price-lists',
          validateStatus: (_response, json) => isApiResponse(json, isPriceListList),
        }),
        transformResponse: unwrap(isPriceListList, 'GET /api/price-lists'),
        providesTags: ['PriceList'],
      }),
      createPriceList: builder.mutation<PriceList, PriceListBody>({
        query: (body) => ({
          url: '/api/price-lists',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isPriceList),
        }),
        transformResponse: unwrap(isPriceList, 'POST /api/price-lists'),
        invalidatesTags: ['PriceList', 'Dashboard'],
      }),
      updatePriceList: builder.mutation<PriceList, { id: number; body: PriceListBody }>({
        query: ({ id, body }) => ({
          url: `/api/price-lists/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isPriceList),
        }),
        transformResponse: unwrap(isPriceList, 'PATCH /api/price-lists'),
        invalidatesTags: ['PriceList', 'Dashboard'],
      }),
      upsertPriceListItem: builder.mutation<
        PriceListItem,
        { priceListId: number; productId: number; body: PriceListItemBody }
      >({
        query: ({ priceListId, productId, body }) => ({
          url: `/api/price-lists/${priceListId}/items/${productId}`,
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isPriceListItem),
        }),
        transformResponse: unwrap(isPriceListItem, 'PUT /api/price-lists/{id}/items'),
        invalidatesTags: ['PriceList'],
      }),
      getDiscountPolicy: builder.query<DiscountPolicy[], void>({
        query: () => ({
          url: '/api/discount-policy',
          validateStatus: (_response, json) => isApiResponse(json, isDiscountPolicyList),
        }),
        transformResponse: unwrap(isDiscountPolicyList, 'GET /api/discount-policy'),
        providesTags: ['DiscountPolicy'],
      }),
      replaceDiscountPolicy: builder.mutation<DiscountPolicy[], DiscountPolicyReplaceBody>({
        query: (body) => ({
          url: '/api/discount-policy',
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isDiscountPolicyList),
        }),
        transformResponse: unwrap(isDiscountPolicyList, 'PUT /api/discount-policy'),
        invalidatesTags: ['DiscountPolicy'],
      }),
      getApprovalPolicy: builder.query<ApprovalPolicy, void>({
        query: () => ({
          url: '/api/approval-policy',
          validateStatus: (_response, json) => isApiResponse(json, isApprovalPolicy),
        }),
        transformResponse: unwrap(isApprovalPolicy, 'GET /api/approval-policy'),
        providesTags: ['ApprovalPolicy'],
      }),
      replaceApprovalPolicy: builder.mutation<ApprovalPolicy, ApprovalPolicyReplaceBody>({
        query: (body) => ({
          url: '/api/approval-policy',
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isApprovalPolicy),
        }),
        transformResponse: unwrap(isApprovalPolicy, 'PUT /api/approval-policy'),
        invalidatesTags: ['ApprovalPolicy'],
      }),
      getWarehouses: builder.query<Warehouse[], void>({
        query: () => ({
          url: '/api/warehouses',
          validateStatus: (_response, json) => isApiResponse(json, isWarehouseList),
        }),
        transformResponse: unwrap(isWarehouseList, 'GET /api/warehouses'),
        providesTags: ['Warehouse'],
      }),
      createWarehouse: builder.mutation<Warehouse, WarehouseBody>({
        query: (body) => ({
          url: '/api/warehouses',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isWarehouse),
        }),
        transformResponse: unwrap(isWarehouse, 'POST /api/warehouses'),
        invalidatesTags: ['Warehouse', 'Dashboard'],
      }),
      getWarehouseInventory: builder.query<Inventory[], number>({
        query: (id) => ({
          url: `/api/warehouses/${id}/inventory`,
          validateStatus: (_response, json) => isApiResponse(json, isInventoryList),
        }),
        transformResponse: unwrap(isInventoryList, 'GET /api/warehouses/{id}/inventory'),
        providesTags: (_result, _error, id) => [{ type: 'Inventory', id }],
      }),
      upsertInventory: builder.mutation<
        Inventory,
        { warehouseId: number; productId: number; body: InventoryBody }
      >({
        query: ({ warehouseId, productId, body }) => ({
          url: `/api/warehouses/${warehouseId}/inventory/${productId}`,
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isInventory),
        }),
        transformResponse: unwrap(isInventory, 'PUT /api/warehouses/{id}/inventory'),
        invalidatesTags: (_result, _error, { warehouseId }) => [{ type: 'Inventory', id: warehouseId }],
      }),
      getSubscriptionPlans: builder.query<SubscriptionPlan[], void>({
        query: () => ({
          url: '/api/subscription-plans',
          validateStatus: (_response, json) => isApiResponse(json, isPlanList),
        }),
        transformResponse: unwrap(isPlanList, 'GET /api/subscription-plans'),
        providesTags: ['SubscriptionPlan'],
      }),
      createSubscriptionPlan: builder.mutation<SubscriptionPlan, SubscriptionPlanBody>({
        query: (body) => ({
          url: '/api/subscription-plans',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isSubscriptionPlan),
        }),
        transformResponse: unwrap(isSubscriptionPlan, 'POST /api/subscription-plans'),
        invalidatesTags: ['SubscriptionPlan', 'Dashboard'],
      }),
      updateSubscriptionPlan: builder.mutation<SubscriptionPlan, { id: number; body: SubscriptionPlanBody }>({
        query: ({ id, body }) => ({
          url: `/api/subscription-plans/${id}`,
          method: 'PATCH',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isSubscriptionPlan),
        }),
        transformResponse: unwrap(isSubscriptionPlan, 'PATCH /api/subscription-plans'),
        invalidatesTags: ['SubscriptionPlan', 'Dashboard'],
      }),
      getUpsellRules: builder.query<UpsellRule[], void>({
        query: () => ({
          url: '/api/upsell-rules',
          validateStatus: (_response, json) => isApiResponse(json, isUpsellRuleList),
        }),
        transformResponse: unwrap(isUpsellRuleList, 'GET /api/upsell-rules'),
        providesTags: ['UpsellRule'],
      }),
      createUpsellRule: builder.mutation<UpsellRule, UpsellRuleBody>({
        query: (body) => ({
          url: '/api/upsell-rules',
          method: 'POST',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isUpsellRule),
        }),
        transformResponse: unwrap(isUpsellRule, 'POST /api/upsell-rules'),
        invalidatesTags: ['UpsellRule'],
      }),
      getStandingRules: builder.query<StandingRule, void>({
        query: () => ({
          url: '/api/standing-rules',
          validateStatus: (_response, json) => isApiResponse(json, isStandingRule),
        }),
        transformResponse: unwrap(isStandingRule, 'GET /api/standing-rules'),
        providesTags: ['StandingRule'],
      }),
      replaceStandingRules: builder.mutation<StandingRule, StandingRuleBody>({
        query: (body) => ({
          url: '/api/standing-rules',
          method: 'PUT',
          body,
          validateStatus: (_response, json) => isApiResponse(json, isStandingRule),
        }),
        transformResponse: unwrap(isStandingRule, 'PUT /api/standing-rules'),
        invalidatesTags: ['StandingRule'],
      }),
    }),
  })

export const {
  useGetCategoriesQuery,
  useCreateCategoryMutation,
  useUpdateCategoryMutation,
  useGetProductsQuery,
  useGetProductQuery,
  useCreateProductMutation,
  useUpdateProductMutation,
  useCreateVariantMutation,
  useUpdateVariantMutation,
  useGetCustomerTiersQuery,
  useCreateCustomerTierMutation,
  useGetPriceListsQuery,
  useCreatePriceListMutation,
  useUpdatePriceListMutation,
  useUpsertPriceListItemMutation,
  useGetDiscountPolicyQuery,
  useReplaceDiscountPolicyMutation,
  useGetApprovalPolicyQuery,
  useReplaceApprovalPolicyMutation,
  useGetWarehousesQuery,
  useCreateWarehouseMutation,
  useGetWarehouseInventoryQuery,
  useUpsertInventoryMutation,
  useGetSubscriptionPlansQuery,
  useCreateSubscriptionPlanMutation,
  useUpdateSubscriptionPlanMutation,
  useGetUpsellRulesQuery,
  useCreateUpsellRuleMutation,
  useGetStandingRulesQuery,
  useReplaceStandingRulesMutation,
} = configApi
