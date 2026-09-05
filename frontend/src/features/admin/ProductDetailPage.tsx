import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import {
  useCreateUpsellRuleMutation,
  useCreateVariantMutation,
  useGetCustomerTiersQuery,
  useGetPriceListsQuery,
  useGetProductQuery,
  useGetProductsQuery,
  useGetUpsellRulesQuery,
  useUpdateProductMutation,
  useUpsertPriceListItemMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { BillingType } from './types'

type ProductModal = 'edit' | 'variant' | 'override' | 'upsell' | null

export function ProductDetailPage() {
  const params = useParams()
  const productId = Number(params.id)
  const productQuery = useGetProductQuery(productId, { skip: !Number.isFinite(productId) })
  const productsQuery = useGetProductsQuery()
  const tiersQuery = useGetCustomerTiersQuery()
  const priceListsQuery = useGetPriceListsQuery()
  const upsellQuery = useGetUpsellRulesQuery()
  const [updateProduct, updateProductState] = useUpdateProductMutation()
  const [createVariant, createVariantState] = useCreateVariantMutation()
  const [upsertPrice, upsertPriceState] = useUpsertPriceListItemMutation()
  const [createUpsell, createUpsellState] = useCreateUpsellRuleMutation()
  const [modal, setModal] = useState<ProductModal>(null)
  const [error, setError] = useState<string | null>(null)

  const product = productQuery.data
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [unit, setUnit] = useState('')
  const [basePrice, setBasePrice] = useState('')
  const [costPrice, setCostPrice] = useState('')
  const [taxPercent, setTaxPercent] = useState('')
  const [billingType, setBillingType] = useState<BillingType>('ONE_TIME')
  const [hydrated, setHydrated] = useState(false)

  if (product && !hydrated) {
    setName(product.name)
    setDescription(product.description)
    setUnit(product.unit)
    setBasePrice(String(product.basePrice))
    setCostPrice(String(product.costPrice))
    setTaxPercent(String(product.taxPercent))
    setBillingType(product.billingType)
    setHydrated(true)
  }

  const [attributeName, setAttributeName] = useState('Size')
  const [attributeValue, setAttributeValue] = useState('')
  const [extraPrice, setExtraPrice] = useState('0')
  const [priceListId, setPriceListId] = useState('')
  const [overridePrice, setOverridePrice] = useState('')
  const [suggestedProductId, setSuggestedProductId] = useState('')
  const [score, setScore] = useState('0.8')
  const [promotionBoost, setPromotionBoost] = useState('0.2')
  const [minMarginPct, setMinMarginPct] = useState('20')

  async function onSaveProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await updateProduct({
        id: productId,
        body: {
          name,
          description,
          unit,
          basePrice: Number(basePrice),
          costPrice: Number(costPrice),
          taxPercent: Number(taxPercent),
          billingType,
        },
      }).unwrap()
      setModal(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save product'))
    }
  }

  async function onAddVariant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await createVariant({
        productId,
        body: {
          attributeName,
          attributeValue,
          extraPrice: Number(extraPrice),
        },
      }).unwrap()
      setAttributeValue('')
      setModal(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add variant'))
    }
  }

  async function onSaveOverride(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await upsertPrice({
        priceListId: Number(priceListId),
        productId,
        body: { price: Number(overridePrice) },
      }).unwrap()
      setModal(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save price override'))
    }
  }

  function onSelectPriceList(nextListId: string) {
    setPriceListId(nextListId)
    const list = (priceListsQuery.data ?? []).find((row) => String(row.id) === nextListId)
    const existing = list?.items.find((item) => item.productId === productId)
    setOverridePrice(existing ? String(existing.price) : '')
  }

  async function onAddUpsell(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await createUpsell({
        triggerProductId: productId,
        suggestedProductId: Number(suggestedProductId),
        score: Number(score),
        promotionBoost: Number(promotionBoost),
        minMarginPct: Number(minMarginPct),
      }).unwrap()
      setModal(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add upsell pairing'))
    }
  }

  if (!Number.isFinite(productId)) {
    return <p className="error">Invalid product.</p>
  }
  if (productQuery.isLoading) {
    return <p className="muted">Loading product…</p>
  }
  if (productQuery.isError || !product) {
    return <p className="error">Product not found.</p>
  }

  const pairings = (upsellQuery.data ?? []).filter((rule) => rule.triggerProductId === productId)
  const priceLists = priceListsQuery.data ?? []
  const tiers = tiersQuery.data ?? []
  const overrides = priceLists.flatMap((list) => {
    const item = list.items.find((row) => row.productId === productId)
    if (!item) {
      return []
    }
    return [{ list, item }]
  })

  function productName(id: number) {
    return productsQuery.data?.find((row) => row.id === id)?.name ?? `Product ${id}`
  }

  function tierName(id: number) {
    return tiers.find((tier) => tier.id === id)?.name ?? `tier ${id}`
  }

  return (
    <div className="stack">
      <p>
        <Link className="link" to="/admin/catalog">
          Back to catalog
        </Link>
      </p>
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}

      <Panel
        title={`Product · ${product.name}`}
        badge={
          <button className="button" type="button" onClick={() => setModal('edit')}>
            Edit
          </button>
        }
      >
        <dl className="facts">
          <div>
            <dt>Name</dt>
            <dd>{product.name}</dd>
          </div>
          <div>
            <dt>Description</dt>
            <dd>{product.description || '—'}</dd>
          </div>
          <div>
            <dt>Unit</dt>
            <dd>{product.unit}</dd>
          </div>
          <div>
            <dt>Selling price</dt>
            <dd>{product.basePrice}</dd>
          </div>
          <div>
            <dt>Cost price</dt>
            <dd>{product.costPrice}</dd>
          </div>
          <div>
            <dt>Tax percent</dt>
            <dd>{product.taxPercent}</dd>
          </div>
          <div>
            <dt>Billing</dt>
            <dd>{product.billingType}</dd>
          </div>
        </dl>
      </Panel>

      <Panel
        title="Variants"
        badge={
          <button className="button" type="button" onClick={() => setModal('variant')}>
            Add variant
          </button>
        }
      >
        {product.variants.length === 0 ? <p className="muted">No variants yet.</p> : null}
        {product.variants.length > 0 ? (
          <ul>
            {product.variants.map((variant) => (
              <li key={variant.id}>
                {variant.attributeName}: {variant.attributeValue}
                <span className="muted"> · extra {variant.extraPrice}</span>
              </li>
            ))}
          </ul>
        ) : null}
      </Panel>

      <Panel
        title="Price list override"
        badge={
          priceLists.length > 0 ? (
            <button className="button" type="button" onClick={() => setModal('override')}>
              Save override
            </button>
          ) : null
        }
      >
        <p className="muted">
          Each customer tier has a price list (Bronze, Silver, Gold, Platinum if you added them). Override is
          this product’s special price on that list. No row means the quote uses selling price ({product.basePrice}).
          Extra currencies still come from{' '}
          <Link className="link" to="/admin/catalog">
            Catalog
          </Link>
          .
        </p>
        {priceListsQuery.isLoading ? <p className="muted">Loading price lists…</p> : null}
        {priceLists.length === 0 ? (
          <p className="muted">No price lists yet. Create one from a customer tier on Catalog.</p>
        ) : null}
        {overrides.length === 0 && priceLists.length > 0 ? (
          <p className="muted">No override for this product yet. Every tier currently pays {product.basePrice}.</p>
        ) : null}
        {overrides.length > 0 ? (
          <ul>
            {overrides.map(({ list, item }) => (
              <li key={list.id}>
                {list.name}
                <span className="muted">
                  {' '}
                  · {tierName(list.customerTierId)} · {list.currency} · {item.price} (selling {product.basePrice})
                </span>
              </li>
            ))}
          </ul>
        ) : null}
      </Panel>

      <Panel
        title="Upsell pairing"
        badge={
          <button className="button" type="button" onClick={() => setModal('upsell')}>
            Add pairing
          </button>
        }
      >
        {pairings.length === 0 ? <p className="muted">No pairings from this product yet.</p> : null}
        {pairings.length > 0 ? (
          <ul>
            {pairings.map((rule) => (
              <li key={rule.id}>
                {productName(rule.suggestedProductId)}
                <span className="muted">
                  {' '}
                  · score {rule.score} · boost {rule.promotionBoost} · min margin {rule.minMarginPct}%
                </span>
              </li>
            ))}
          </ul>
        ) : null}
      </Panel>

      {modal === 'edit' ? (
        <Modal title="Edit product" onClose={() => setModal(null)}>
          <form className="form" onSubmit={onSaveProduct}>
            <label className="field field-full">
              Name
              <input className="input" value={name} onChange={(event) => setName(event.target.value)} required />
            </label>
            <label className="field field-full">
              Description
              <input
                className="input"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
            <label className="field">
              Unit
              <input className="input" value={unit} onChange={(event) => setUnit(event.target.value)} required />
            </label>
            <label className="field">
              Billing type
              <select
                className="input"
                value={billingType}
                onChange={(event) => setBillingType(event.target.value as BillingType)}
              >
                <option value="ONE_TIME">ONE_TIME</option>
                <option value="RECURRING">RECURRING</option>
              </select>
            </label>
            <label className="field">
              Selling price
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={basePrice}
                onChange={(event) => setBasePrice(event.target.value)}
                required
              />
            </label>
            <label className="field">
              Cost price
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={costPrice}
                onChange={(event) => setCostPrice(event.target.value)}
                required
              />
            </label>
            <label className="field field-full">
              Tax percent
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={taxPercent}
                onChange={(event) => setTaxPercent(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={updateProductState.isLoading}>
                {updateProductState.isLoading ? 'Saving…' : 'Save product'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'variant' ? (
        <Modal title="Add variant" onClose={() => setModal(null)}>
          <form className="form" onSubmit={onAddVariant}>
            <label className="field">
              Attribute
              <input
                className="input"
                value={attributeName}
                onChange={(event) => setAttributeName(event.target.value)}
                required
              />
            </label>
            <label className="field">
              Value
              <input
                className="input"
                value={attributeValue}
                onChange={(event) => setAttributeValue(event.target.value)}
                required
              />
            </label>
            <label className="field field-full">
              Extra price
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={extraPrice}
                onChange={(event) => setExtraPrice(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={createVariantState.isLoading}>
                {createVariantState.isLoading ? 'Saving…' : 'Add variant'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'override' ? (
        <Modal title="Save override" onClose={() => setModal(null)}>
          <form className="form" onSubmit={onSaveOverride}>
            <label className="field field-full">
              Price list
              <select
                className="input"
                value={priceListId}
                onChange={(event) => onSelectPriceList(event.target.value)}
                required
              >
                <option value="">Select price list</option>
                {priceLists.map((list) => (
                  <option key={list.id} value={list.id}>
                    {list.name} · {tierName(list.customerTierId)} · {list.currency}
                  </option>
                ))}
              </select>
            </label>
            <label className="field field-full">
              Override price
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={overridePrice}
                onChange={(event) => setOverridePrice(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={upsertPriceState.isLoading}>
                {upsertPriceState.isLoading ? 'Saving…' : 'Save override'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'upsell' ? (
        <Modal title="Add pairing" onClose={() => setModal(null)}>
          <form className="form" onSubmit={onAddUpsell}>
            <label className="field field-full">
              Suggested product
              <select
                className="input"
                value={suggestedProductId}
                onChange={(event) => setSuggestedProductId(event.target.value)}
                required
              >
                <option value="">Select product</option>
                {(productsQuery.data ?? [])
                  .filter((row) => row.id !== productId)
                  .map((row) => (
                    <option key={row.id} value={row.id}>
                      {row.name}
                    </option>
                  ))}
              </select>
            </label>
            <label className="field">
              Score
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={score}
                onChange={(event) => setScore(event.target.value)}
                required
              />
            </label>
            <label className="field">
              Promotion boost
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={promotionBoost}
                onChange={(event) => setPromotionBoost(event.target.value)}
                required
              />
            </label>
            <label className="field field-full">
              Min margin %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={minMarginPct}
                onChange={(event) => setMinMarginPct(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={createUpsellState.isLoading}>
                {createUpsellState.isLoading ? 'Saving…' : 'Add pairing'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
