import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Modal } from '../../components/common/Modal'
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

type CatalogModal = 'category' | 'product' | 'priceList' | null

export function CatalogPage() {
  const categories = useGetCategoriesQuery()
  const products = useGetProductsQuery()
  const tiers = useGetCustomerTiersQuery()
  const priceLists = useGetPriceListsQuery()
  const [createCategory, createCategoryState] = useCreateCategoryMutation()
  const [createProduct, createProductState] = useCreateProductMutation()
  const [createPriceList, createPriceListState] = useCreatePriceListMutation()
  const [modal, setModal] = useState<CatalogModal>(null)
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

  function closeModal() {
    setModal(null)
    setCategoryError(null)
    setProductError(null)
    setPriceListError(null)
  }

  async function onCreateCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCategoryError(null)
    try {
      await createCategory({ name: categoryName }).unwrap()
      setCategoryName('')
      closeModal()
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
      closeModal()
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
      closeModal()
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
      <Panel
        title="Categories"
        badge={
          <button className="button" type="button" onClick={() => setModal('category')}>
            Add category
          </button>
        }
      >
        {categories.isLoading ? <p className="muted">Loading categories…</p> : null}
        {categories.isError ? <p className="error">Could not load categories.</p> : null}
        {categoryRows.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {categoryRows.map((category) => (
                <tr key={category.id}>
                  <td>
                    <span className="table-primary">{category.name}</span>
                  </td>
                  <td>{category.active ? 'Active' : 'Inactive'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">No categories yet.</p>
        )}
      </Panel>

      <Panel
        title="Products"
        badge={
          <button className="button" type="button" onClick={() => setModal('product')}>
            Add product
          </button>
        }
      >
        {products.isLoading ? <p className="muted">Loading products…</p> : null}
        {products.isError ? <p className="error">Could not load products.</p> : null}
        {productRows.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Billing</th>
                <th>Price</th>
              </tr>
            </thead>
            <tbody>
              {productRows.map((product) => (
                <tr key={product.id}>
                  <td>
                    <Link className="table-primary" to={`/admin/products/${product.id}`}>
                      {product.name}
                    </Link>
                  </td>
                  <td>{product.billingType}</td>
                  <td>
                    {product.basePrice} / {product.unit}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">No products yet.</p>
        )}
      </Panel>

      <Panel
        title="Price lists"
        badge={
          <button className="button" type="button" onClick={() => setModal('priceList')}>
            Add price list
          </button>
        }
      >
        <p className="muted">
          Each customer tier gets a default INR price list automatically. Extra currencies (for example Gold
          USD) you still add here.
        </p>
        {priceLists.isLoading ? <p className="muted">Loading price lists…</p> : null}
        {priceLists.isError ? <p className="error">Could not load price lists.</p> : null}
        {priceListRows.length === 0 ? <p className="muted">No price lists yet.</p> : null}
        {priceListRows.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>List</th>
                <th>Tier</th>
                <th>Currency</th>
                <th>Overrides</th>
              </tr>
            </thead>
            <tbody>
              {priceListRows.map((list) => (
                <tr key={list.id}>
                  <td>
                    <span className="table-primary">{list.name}</span>
                  </td>
                  <td>{tierName(list.customerTierId)}</td>
                  <td>{list.currency}</td>
                  <td>
                    {list.items.length} override{list.items.length === 1 ? '' : 's'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Panel>

      {modal === 'category' ? (
        <Modal title="Add category" onClose={closeModal}>
          <form className="form" onSubmit={onCreateCategory}>
            <label className="field field-full" htmlFor="add-category-name">
              Name
              <input
                id="add-category-name"
                className="input"
                name="categoryName"
                value={categoryName}
                onChange={(event) => setCategoryName(event.target.value)}
                required
              />
            </label>
            {categoryError ? (
              <p className="error field-full" role="alert">
                {categoryError}
              </p>
            ) : null}
            <div className="form-actions">
              <button className="button" type="submit" disabled={createCategoryState.isLoading}>
                {createCategoryState.isLoading ? 'Saving…' : 'Add category'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'product' ? (
        <Modal title="Add product" onClose={closeModal}>
          <form className="form" onSubmit={onCreateProduct}>
            <label className="field field-full" htmlFor="add-product-category">
              Category
              <select
                id="add-product-category"
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
            <label className="field field-full" htmlFor="add-product-name">
              Name
              <input
                id="add-product-name"
                className="input"
                name="productName"
                value={productName}
                onChange={(event) => setProductName(event.target.value)}
                required
              />
            </label>
            <label className="field field-full" htmlFor="add-product-description">
              Description
              <input
                id="add-product-description"
                className="input"
                name="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
            <label className="field" htmlFor="add-product-unit">
              Unit
              <input
                id="add-product-unit"
                className="input"
                name="unit"
                value={unit}
                placeholder="piece, hour, pack"
                onChange={(event) => setUnit(event.target.value)}
                required
              />
            </label>
            <label className="field" htmlFor="add-product-billing">
              Billing type
              <select
                id="add-product-billing"
                className="input"
                name="billingType"
                value={billingType}
                onChange={(event) => setBillingType(event.target.value as BillingType)}
              >
                <option value="ONE_TIME">ONE_TIME</option>
                <option value="RECURRING">RECURRING</option>
              </select>
            </label>
            <label className="field" htmlFor="add-product-base">
              Selling price
              <input
                id="add-product-base"
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
            <label className="field" htmlFor="add-product-cost">
              Cost price
              <input
                id="add-product-cost"
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
            <label className="field field-full" htmlFor="add-product-tax">
              Tax percent
              <input
                id="add-product-tax"
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
            {productError ? (
              <p className="error field-full" role="alert">
                {productError}
              </p>
            ) : null}
            <div className="form-actions">
              <button className="button" type="submit" disabled={createProductState.isLoading}>
                {createProductState.isLoading ? 'Saving…' : 'Add product'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'priceList' ? (
        <Modal title="Add price list" onClose={closeModal}>
          <form className="form" onSubmit={onCreatePriceList}>
            <label className="field field-full" htmlFor="add-list-tier">
              Customer tier
              <select
                id="add-list-tier"
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
            <label className="field" htmlFor="add-list-currency">
              Currency
              <input
                id="add-list-currency"
                className="input"
                value={priceListCurrency}
                onChange={(event) => setPriceListCurrency(event.target.value)}
                placeholder="INR"
                required
              />
            </label>
            <label className="field" htmlFor="add-list-name">
              Name
              <input
                id="add-list-name"
                className="input"
                value={priceListName}
                onChange={(event) => setPriceListName(event.target.value)}
                placeholder="Silver INR"
              />
            </label>
            {priceListError ? (
              <p className="error field-full" role="alert">
                {priceListError}
              </p>
            ) : null}
            <div className="form-actions">
              <button className="button" type="submit" disabled={createPriceListState.isLoading}>
                {createPriceListState.isLoading ? 'Saving…' : 'Add price list'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
