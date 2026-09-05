import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import {
  useCreateUpsellRuleMutation,
  useCreateVariantMutation,
  useGetPriceListsQuery,
  useGetProductQuery,
  useGetProductsQuery,
  useGetUpsellRulesQuery,
  useUpdateProductMutation,
  useUpsertPriceListItemMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { BillingType } from './types'

export function ProductDetailPage() {
  const params = useParams()
  const productId = Number(params.id)
  const productQuery = useGetProductQuery(productId, { skip: !Number.isFinite(productId) })
  const productsQuery = useGetProductsQuery()
  const priceListsQuery = useGetPriceListsQuery()
  const upsellQuery = useGetUpsellRulesQuery()
  const [updateProduct, updateProductState] = useUpdateProductMutation()
  const [createVariant, createVariantState] = useCreateVariantMutation()
  const [upsertPrice, upsertPriceState] = useUpsertPriceListItemMutation()
  const [createUpsell, createUpsellState] = useCreateUpsellRuleMutation()
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
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save price override'))
    }
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

      <Panel title={`Product · ${product.name}`}>
        <form className="form" onSubmit={onSaveProduct}>
          <label className="field">
            Name
            <input className="input" value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="field">
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
          <label className="field">
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
          <div className="form-actions">
            <button className="button" type="submit" disabled={updateProductState.isLoading}>
              {updateProductState.isLoading ? 'Saving…' : 'Save product'}
            </button>
          </div>
        </form>
      </Panel>

      <Panel title="Variants">
        {product.variants.length === 0 ? <p className="muted">No variants yet.</p> : null}
        <ul>
          {product.variants.map((variant) => (
            <li key={variant.id}>
              {variant.attributeName}: {variant.attributeValue}
              <span className="muted"> · extra {variant.extraPrice}</span>
            </li>
          ))}
        </ul>
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
          <label className="field">
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
      </Panel>

      <Panel title="Price list override">
        <p className="muted">Falls back to base selling price when no override exists.</p>
        <form className="form" onSubmit={onSaveOverride}>
          <label className="field">
            Price list
            <select
              className="input"
              value={priceListId}
              onChange={(event) => setPriceListId(event.target.value)}
              required
            >
              <option value="">Select price list</option>
              {(priceListsQuery.data ?? []).map((list) => (
                <option key={list.id} value={list.id}>
                  {list.name} ({list.currency})
                </option>
              ))}
            </select>
          </label>
          <label className="field">
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
      </Panel>

      <Panel title="Upsell pairing">
        {pairings.length === 0 ? <p className="muted">No pairings from this product yet.</p> : null}
        <ul>
          {pairings.map((rule) => (
            <li key={rule.id}>
              suggests product {rule.suggestedProductId}
              <span className="muted">
                {' '}
                · score {rule.score} · boost {rule.promotionBoost} · min margin {rule.minMarginPct}%
              </span>
            </li>
          ))}
        </ul>
        <form className="form" onSubmit={onAddUpsell}>
          <label className="field">
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
          <label className="field">
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
      </Panel>
    </div>
  )
}
