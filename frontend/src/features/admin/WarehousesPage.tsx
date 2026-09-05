import { useState, type FormEvent } from 'react'
import { Panel } from '../../components/common/Panel'
import {
  useCreateWarehouseMutation,
  useGetProductsQuery,
  useGetWarehouseInventoryQuery,
  useGetWarehousesQuery,
  useUpsertInventoryMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'

export function WarehousesPage() {
  const warehousesQuery = useGetWarehousesQuery()
  const productsQuery = useGetProductsQuery()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const inventoryQuery = useGetWarehouseInventoryQuery(selectedId ?? 0, { skip: selectedId == null })
  const [createWarehouse, createWarehouseState] = useCreateWarehouseMutation()
  const [upsertInventory, upsertInventoryState] = useUpsertInventoryMutation()
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
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save stock'))
    }
  }

  function productName(id: number) {
    return productsQuery.data?.find((product) => product.id === id)?.name ?? `Product ${id}`
  }

  return (
    <div className="stack">
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}

      <Panel title="Warehouses">
        {warehousesQuery.isLoading ? <p className="muted">Loading warehouses…</p> : null}
        {warehouses.length === 0 ? <p className="muted">No warehouses yet.</p> : null}
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
        ) : null}
        <form className="form" onSubmit={onCreateWarehouse}>
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
          <label className="field">
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
      </Panel>

      <Panel title={activeId == null ? 'Stock' : `Stock · warehouse ${activeId}`}>
        {activeId == null ? <p className="muted">Create a warehouse first.</p> : null}
        {inventoryQuery.isFetching ? <p className="muted">Loading stock…</p> : null}
        <ul>
          {(inventoryQuery.data ?? []).map((row) => (
            <li key={`${row.warehouseId}-${row.productId}`}>
              {productName(row.productId)}
              <span className="muted">
                {' '}
                · on hand {row.onHand} · reserved {row.reserved} · available {row.available} · min{' '}
                {row.minStock} · reorder {row.reorderQty}
              </span>
            </li>
          ))}
        </ul>
        {activeId != null ? (
          <form className="form" onSubmit={onSaveStock}>
            <label className="field">
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
            <label className="field">
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
        ) : null}
      </Panel>
    </div>
  )
}
