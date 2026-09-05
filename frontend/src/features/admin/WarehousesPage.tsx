import { useState, type FormEvent } from 'react'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import {
  useCreateWarehouseMutation,
  useGetProductsQuery,
  useGetWarehouseInventoryQuery,
  useGetWarehousesQuery,
  useUpsertInventoryMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'

type WarehouseModal = 'warehouse' | 'stock' | null

export function WarehousesPage() {
  const warehousesQuery = useGetWarehousesQuery()
  const productsQuery = useGetProductsQuery()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const inventoryQuery = useGetWarehouseInventoryQuery(selectedId ?? 0, { skip: selectedId == null })
  const [createWarehouse, createWarehouseState] = useCreateWarehouseMutation()
  const [upsertInventory, upsertInventoryState] = useUpsertInventoryMutation()
  const [modal, setModal] = useState<WarehouseModal>(null)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [location, setLocation] = useState('')
  const [shippingCostWeight, setShippingCostWeight] = useState('1')
  const [productId, setProductId] = useState('')
  const [onHand, setOnHand] = useState('0')
  const [minStock, setMinStock] = useState('0')
  const [reorderQty, setReorderQty] = useState('0')

  const warehouses = warehousesQuery.data ?? []
  const activeId = selectedId ?? warehouses[0]?.id ?? null
  if (selectedId == null && warehouses[0] && activeId != null) {
    setSelectedId(warehouses[0].id)
  }

  const activeWarehouse = warehouses.find((row) => row.id === activeId)

  function closeModal() {
    setModal(null)
    setError(null)
  }

  async function onCreateWarehouse(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      const created = await createWarehouse({
        name,
        location,
        shippingCostWeight: Number(shippingCostWeight),
      }).unwrap()
      setName('')
      setLocation('')
      setSelectedId(created.id)
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create warehouse'))
    }
  }

  async function onSaveStock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (activeId == null) {
      return
    }
    setError(null)
    try {
      await upsertInventory({
        warehouseId: activeId,
        productId: Number(productId),
        body: {
          onHand: Number(onHand),
          reserved: 0,
          minStock: Number(minStock),
          reorderQty: Number(reorderQty),
        },
      }).unwrap()
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save stock'))
    }
  }

  function productName(id: number) {
    return productsQuery.data?.find((product) => product.id === id)?.name ?? `Product ${id}`
  }

  return (
    <div className="stack">
      <Panel
        title="Warehouses"
        badge={
          <button className="button" type="button" onClick={() => setModal('warehouse')}>
            Add warehouse
          </button>
        }
      >
        {warehousesQuery.isLoading ? <p className="muted">Loading warehouses…</p> : null}
        {warehouses.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Warehouse</th>
                <th>Location</th>
                <th>Ship weight</th>
              </tr>
            </thead>
            <tbody>
              {warehouses.map((warehouse) => (
                <tr
                  key={warehouse.id}
                  className={activeId === warehouse.id ? 'board-row board-row-selected' : 'board-row'}
                  onClick={() => setSelectedId(warehouse.id)}
                >
                  <td>
                    <span className="table-primary">{warehouse.name}</span>
                  </td>
                  <td>{warehouse.location}</td>
                  <td>{warehouse.shippingCostWeight}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : warehousesQuery.isSuccess ? (
          <p className="muted">No warehouses yet.</p>
        ) : null}
      </Panel>

      <Panel
        title={activeWarehouse ? `Stock · ${activeWarehouse.name}` : 'Stock'}
        badge={
          activeId != null ? (
            <button className="button" type="button" onClick={() => setModal('stock')}>
              Update stock
            </button>
          ) : undefined
        }
      >
        {activeId == null ? <p className="muted">Create a warehouse first.</p> : null}
        {inventoryQuery.isFetching ? <p className="muted">Loading stock…</p> : null}
        {activeId != null && (inventoryQuery.data ?? []).length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>On hand</th>
                <th>Reserved</th>
                <th>Available</th>
                <th>Min</th>
                <th>Reorder</th>
              </tr>
            </thead>
            <tbody>
              {(inventoryQuery.data ?? []).map((row) => (
                <tr key={`${row.warehouseId}-${row.productId}`}>
                  <td>
                    <span className="table-primary">{productName(row.productId)}</span>
                  </td>
                  <td>{row.onHand}</td>
                  <td>{row.reserved}</td>
                  <td>{row.available}</td>
                  <td>{row.minStock}</td>
                  <td>{row.reorderQty}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : activeId != null && inventoryQuery.isSuccess ? (
          <p className="muted">No stock rows yet.</p>
        ) : null}
      </Panel>

      {modal === 'warehouse' ? (
        <Modal title="Add warehouse" onClose={closeModal}>
          <form className="form" onSubmit={onCreateWarehouse}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <label className="field">
              Name
              <input className="input" value={name} onChange={(event) => setName(event.target.value)} required />
            </label>
            <label className="field">
              Location
              <input
                className="input"
                value={location}
                onChange={(event) => setLocation(event.target.value)}
                required
              />
            </label>
            <label className="field field-full">
              Shipping cost weight
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={shippingCostWeight}
                onChange={(event) => setShippingCostWeight(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={createWarehouseState.isLoading}>
                {createWarehouseState.isLoading ? 'Saving…' : 'Add warehouse'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'stock' && activeId != null ? (
        <Modal title="Update stock" onClose={closeModal}>
          <form className="form" onSubmit={onSaveStock}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <label className="field field-full">
              Product
              <select
                className="input"
                value={productId}
                onChange={(event) => setProductId(event.target.value)}
                required
              >
                <option value="">Select product</option>
                {(productsQuery.data ?? []).map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              On hand
              <input
                className="input"
                type="number"
                min="0"
                step="1"
                value={onHand}
                onChange={(event) => setOnHand(event.target.value)}
                required
              />
            </label>
            <label className="field">
              Min stock
              <input
                className="input"
                type="number"
                min="0"
                step="1"
                value={minStock}
                onChange={(event) => setMinStock(event.target.value)}
                required
              />
            </label>
            <label className="field field-full">
              Reorder qty
              <input
                className="input"
                type="number"
                min="0"
                step="1"
                value={reorderQty}
                onChange={(event) => setReorderQty(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={upsertInventoryState.isLoading}>
                {upsertInventoryState.isLoading ? 'Saving…' : 'Save stock'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
