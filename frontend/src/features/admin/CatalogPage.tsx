import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import {
  useCreateCategoryMutation,
  useCreatePriceListMutation,
  useCreateProductMutation,
  useGetCategoriesQuery,
  useGetCustomerTiersQuery,
  useGetPriceListsQuery,
  useGetProductsQuery,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { BillingType } from './types'

export function CatalogPage() {
  const categories = useGetCategoriesQuery()
  const products = useGetProductsQuery()
  const tiers = useGetCustomerTiersQuery()
  const priceLists = useGetPriceListsQuery()
  const [createCategory, createCategoryState] = useCreateCategoryMutation()
  const [createProduct, createProductState] = useCreateProductMutation()
  const [createPriceList, createPriceListState] = useCreatePriceListMutation()
  const [categoryError, setCategoryError] = useState<string | null>(null)
  const [productError, setProductError] = useState<string | null>(null)
  const [priceListError, setPriceListError] = useState<string | null>(null)
  const [categoryName, setCategoryName] = useState('')
  const [productName, setProductName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [unit, setUnit] = useState('')
  const [basePrice, setBasePrice] = useState('0')
  const [costPrice, setCostPrice] = useState('0')
  const [taxPercent, setTaxPercent] = useState('18')
  const [billingType, setBillingType] = useState<BillingType>('ONE_TIME')
  const [priceListName, setPriceListName] = useState('')
  const [priceListCurrency, setPriceListCurrency] = useState('INR')
  const [priceListTierId, setPriceListTierId] = useState('')

  async function onCreateCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCategoryError(null)
    try {
      await createCategory({ name: categoryName }).unwrap()
      setCategoryName('')
    } catch (err) {
      setCategoryError(apiErrorMessage(err, 'Could not create category'))
    }
  }

  async function onCreateProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setProductError(null)
    try {
      await createProduct({
        categoryId: Number(categoryId),
        name: productName,
        description,
        unit,
        basePrice: Number(basePrice),
        costPrice: Number(costPrice),
        taxPercent: Number(taxPercent),
        billingType,
      }).unwrap()
      setProductName('')
      setDescription('')
    } catch (err) {
      setProductError(apiErrorMessage(err, 'Could not create product'))
    }
  }

  async function onCreatePriceList(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPriceListError(null)
    const tier = (tiers.data ?? []).find((row) => String(row.id) === priceListTierId)
    const currency = priceListCurrency.trim().toUpperCase()
    try {
      await createPriceList({
        name: priceListName.trim() || (tier ? `${tier.name} ${currency}` : currency),
        currency,
        customerTierId: Number(priceListTierId),
      }).unwrap()
      setPriceListName('')
      setPriceListCurrency('INR')
      setPriceListTierId('')
    } catch (err) {
      setPriceListError(apiErrorMessage(err, 'Could not create price list'))
    }
  }

  const categoryRows = categories.data ?? []
  const productRows = products.data ?? []
  const tierRows = tiers.data ?? []
  const priceListRows = priceLists.data ?? []

  function tierName(id: number) {
    return tierRows.find((tier) => tier.id === id)?.name ?? `tier ${id}`
  }

  return (
    <div className="stack">
      <Panel title="Categories">
        {categories.isLoading ? <p className="muted">Loading categories…</p> : null}
        {categories.isError ? <p className="error">Could not load categories.</p> : null}
        {categoryRows.length > 0 ? (
          <ul>
            {categoryRows.map((category) => (
              <li key={category.id}>
                {category.name}
                <span className="muted"> · {category.active ? 'active' : 'inactive'}</span>
              </li>
            ))}
          </ul>
        ) : null}
        <form className="form" onSubmit={onCreateCategory}>
          <label className="field">
            Name
            <input
              className="input"
              name="categoryName"
              value={categoryName}
              onChange={(event) => setCategoryName(event.target.value)}
              required
            />
          </label>
          {categoryError ? (
            <p className="error" role="alert">
              {categoryError}
            </p>
          ) : null}
          <div className="form-actions">
            <button className="button" type="submit" disabled={createCategoryState.isLoading}>
              {createCategoryState.isLoading ? 'Saving…' : 'Add category'}
            </button>
          </div>
        </form>
      </Panel>

      <Panel title="Products">
        {products.isLoading ? <p className="muted">Loading products…</p> : null}
        {products.isError ? <p className="error">Could not load products.</p> : null}
        {productRows.length > 0 ? (
          <ul>
            {productRows.map((product) => (
              <li key={product.id}>
                <Link className="link" to={`/admin/products/${product.id}`}>
                  {product.name}
                </Link>
                <span className="muted">
                  {' '}
                  · {product.billingType} · {product.basePrice} / {product.unit}
                </span>
              </li>
            ))}
          </ul>
        ) : null}
        <form className="form" onSubmit={onCreateProduct}>
          <label className="field">
            Category
            <select
              className="input"
              name="categoryId"
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
              required
            >
              <option value="">Select category</option>
              {categoryRows.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Name
            <input
              className="input"
              name="productName"
              value={productName}
              onChange={(event) => setProductName(event.target.value)}
              required
            />
          </label>
          <label className="field">
            Description
            <input
              className="input"
              name="description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          <label className="field">
            Unit
            <input
              className="input"
              name="unit"
              value={unit}
              placeholder="piece, hour, pack"
              onChange={(event) => setUnit(event.target.value)}
              required
            />
          </label>
          <label className="field">
            Selling price
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              name="basePrice"
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
              name="costPrice"
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
              name="taxPercent"
              value={taxPercent}
              onChange={(event) => setTaxPercent(event.target.value)}
              required
            />
          </label>
          <label className="field">
            Billing type
            <select
              className="input"
              name="billingType"
              value={billingType}
              onChange={(event) => setBillingType(event.target.value as BillingType)}
            >
              <option value="ONE_TIME">ONE_TIME</option>
              <option value="RECURRING">RECURRING</option>
            </select>
          </label>
          {productError ? (
            <p className="error" role="alert">
              {productError}
            </p>
          ) : null}
          <div className="form-actions">
            <button className="button" type="submit" disabled={createProductState.isLoading}>
              {createProductState.isLoading ? 'Saving…' : 'Add product'}
            </button>
          </div>
        </form>
      </Panel>

      <Panel title="Price lists">
        <p className="muted">
          Each customer tier gets a default INR price list automatically. Extra currencies (for example Gold
          USD) you still add here.
        </p>
        {priceLists.isLoading ? <p className="muted">Loading price lists…</p> : null}
        {priceLists.isError ? <p className="error">Could not load price lists.</p> : null}
        {priceListRows.length === 0 ? <p className="muted">No price lists yet.</p> : null}
        {priceListRows.length > 0 ? (
          <ul>
            {priceListRows.map((list) => (
              <li key={list.id}>
                {list.name}
                <span className="muted">
                  {' '}
                  · {tierName(list.customerTierId)} · {list.currency} · {list.items.length} override
                  {list.items.length === 1 ? '' : 's'}
                </span>
              </li>
            ))}
          </ul>
        ) : null}
        <form className="form" onSubmit={onCreatePriceList}>
          <label className="field">
            Customer tier
            <select
              className="input"
              value={priceListTierId}
              onChange={(event) => setPriceListTierId(event.target.value)}
              required
            >
              <option value="">Select tier</option>
              {tierRows.map((tier) => (
                <option key={tier.id} value={tier.id}>
                  {tier.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Currency
            <input
              className="input"
              value={priceListCurrency}
              onChange={(event) => setPriceListCurrency(event.target.value)}
              placeholder="INR"
              required
            />
          </label>
          <label className="field">
            Name
            <input
              className="input"
              value={priceListName}
              onChange={(event) => setPriceListName(event.target.value)}
              placeholder="Silver INR"
            />
          </label>
          {priceListError ? (
            <p className="error" role="alert">
              {priceListError}
            </p>
          ) : null}
          <div className="form-actions">
            <button className="button" type="submit" disabled={createPriceListState.isLoading}>
              {createPriceListState.isLoading ? 'Saving…' : 'Add price list'}
            </button>
          </div>
        </form>
      </Panel>
    </div>
  )
}
