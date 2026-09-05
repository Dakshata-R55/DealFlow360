import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Panel } from '../../components/common/Panel'
import {
  useCreateCategoryMutation,
  useCreateProductMutation,
  useGetCategoriesQuery,
  useGetProductsQuery,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { BillingType } from './types'

export function CatalogPage() {
  const categories = useGetCategoriesQuery()
  const products = useGetProductsQuery()
  const [createCategory, createCategoryState] = useCreateCategoryMutation()
  const [createProduct, createProductState] = useCreateProductMutation()
  const [categoryError, setCategoryError] = useState<string | null>(null)
  const [productError, setProductError] = useState<string | null>(null)
  const [categoryName, setCategoryName] = useState('')
  const [productName, setProductName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [unit, setUnit] = useState('')
  const [basePrice, setBasePrice] = useState('0')
  const [costPrice, setCostPrice] = useState('0')
  const [taxPercent, setTaxPercent] = useState('18')
  const [billingType, setBillingType] = useState<BillingType>('ONE_TIME')

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

  const categoryRows = categories.data ?? []
  const productRows = products.data ?? []

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
    </div>
  )
}
