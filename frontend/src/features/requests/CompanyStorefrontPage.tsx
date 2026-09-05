import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import { useGetCompanyProductsQuery, useGetCompanyQuery } from '../../stores/api/marketplaceApi'
import {
  useAddCustomerRequestLineMutation,
  useCreateCustomerRequestMutation,
  useGetCustomerRequestsQuery,
} from '../../stores/api/quoteRequestApi'
import { apiErrorMessage } from '../../types/api'
import { percentLabel, priceAmountLabel, type PublicProduct, type QuoteRequest } from './types'

export function CompanyStorefrontPage() {
  const { companyId } = useParams()
  const id = Number(companyId)
  const company = useGetCompanyQuery(id, { skip: !Number.isFinite(id) })
  const products = useGetCompanyProductsQuery(id, { skip: !Number.isFinite(id) })
  const requests = useGetCustomerRequestsQuery()
  const [createRequest] = useCreateCustomerRequestMutation()
  const [addLine] = useAddCustomerRequestLineMutation()
  const [qty, setQty] = useState<Record<number, number>>({})
  const [error, setError] = useState<string | null>(null)
  const [pendingProduct, setPendingProduct] = useState<PublicProduct | null>(null)

  const otherDraft = useMemo(() => {
    if (!Number.isFinite(id)) {
      return undefined
    }
    return (requests.data ?? []).find((row) => row.status === 'DRAFT' && row.sellerCompanyId !== id)
  }, [id, requests.data])

  const thisDraft = useMemo(() => {
    if (!Number.isFinite(id)) {
      return undefined
    }
    return (requests.data ?? []).find((row) => row.status === 'DRAFT' && row.sellerCompanyId === id)
  }, [id, requests.data])

  async function addProduct(product: PublicProduct, ignoreOtherDraft: boolean) {
    setError(null)
    if (!ignoreOtherDraft && otherDraft) {
      setPendingProduct(product)
      return
    }
    try {
      const draft = thisDraft ?? (await createRequest({ sellerCompanyId: id }).unwrap())
      const quantity = qty[product.id] ?? 1
      await addLine({ requestId: draft.id, body: { productId: product.id, quantity } }).unwrap()
      setPendingProduct(null)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add to request'))
    }
  }

  if (!Number.isFinite(id)) {
    return <p className="error">Invalid company.</p>
  }
  if (company.isLoading) {
    return <p className="muted">Loading company…</p>
  }
  if (company.isError || !company.data) {
    return <p className="error">Could not load company.</p>
  }

  return (
    <div className="stack">
      <Panel title={company.data.name}>
        <p>{company.data.description}</p>
        {thisDraft ? (
          <p>
            Request ({thisDraft.lines.reduce((sum, line) => sum + line.quantity, 0)}){' '}
            <Link className="link" to={`/customer/requests?request=${thisDraft.id}`}>
              Open request
            </Link>
          </p>
        ) : null}
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
      </Panel>
      {pendingProduct && otherDraft ? (
        <SwitchSellerModal
          other={otherDraft}
          companyName={company.data.name}
          onCancel={() => setPendingProduct(null)}
          onStartNew={() => void addProduct(pendingProduct, true)}
        />
      ) : null}
      <Panel title="Catalog">
        {products.isLoading ? <p className="muted">Loading products…</p> : null}
        {(products.data ?? []).length === 0 && products.isSuccess ? (
          <p className="muted">No products in this catalog yet.</p>
        ) : null}
        <div className="catalog-grid">
          {(products.data ?? []).map((product) => (
            <article key={product.id} className="catalog-card">
              <h3>{product.name}</h3>
              <p className="catalog-meta">
                {product.categoryName} · {product.unit}
              </p>
              <p className="catalog-desc">{product.description}</p>
              <p className="catalog-mrp">
                <strong>MRP</strong> {priceAmountLabel(product)}
              </p>
              {product.categoryDiscountPercent > 0 ? (
                <p className="ok-text">
                  Up to {percentLabel(product.categoryDiscountPercent)} off on {product.categoryName}
                </p>
              ) : null}
              <div className="catalog-qty">
                <button
                  className="catalog-qty-btn"
                  type="button"
                  aria-label={`Decrease ${product.name} quantity`}
                  onClick={() =>
                    setQty((current) => ({ ...current, [product.id]: Math.max(1, (current[product.id] ?? 1) - 1) }))
                  }
                >
                  −
                </button>
                <span className="catalog-qty-value">{qty[product.id] ?? 1}</span>
                <button
                  className="catalog-qty-btn"
                  type="button"
                  aria-label={`Increase ${product.name} quantity`}
                  onClick={() => setQty((current) => ({ ...current, [product.id]: (current[product.id] ?? 1) + 1 }))}
                >
                  +
                </button>
              </div>
              <button className="catalog-add" type="button" onClick={() => void addProduct(product, false)}>
                Add to Request
              </button>
            </article>
          ))}
        </div>
      </Panel>
    </div>
  )
}

function SwitchSellerModal({
  other,
  companyName,
  onCancel,
  onStartNew,
}: {
  other: QuoteRequest
  companyName: string
  onCancel: () => void
  onStartNew: () => void
}) {
  return (
    <Modal title="Start a new request?" onClose={onCancel}>
      <p>
        You currently have a request for {other.sellerCompanyName}. Start a new request for {companyName}?
      </p>
      <div className="form-actions">
        <button className="button" type="button" onClick={onStartNew}>
          Start New Request
        </button>
      </div>
    </Modal>
  )
}
