import { useState, type FormEvent } from 'react'
import { Panel } from '../../components/common/Panel'
import {
  useCreateCustomerTierMutation,
  useGetApprovalPolicyQuery,
  useGetCategoriesQuery,
  useGetCustomerTiersQuery,
  useGetDiscountPolicyQuery,
  useReplaceApprovalPolicyMutation,
  useReplaceDiscountPolicyMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'

export function PoliciesPage() {
  const tiersQuery = useGetCustomerTiersQuery()
  const categoriesQuery = useGetCategoriesQuery()
  const discountQuery = useGetDiscountPolicyQuery()
  const approvalQuery = useGetApprovalPolicyQuery()
  const [createTier, createTierState] = useCreateCustomerTierMutation()
  const [replaceDiscount, replaceDiscountState] = useReplaceDiscountPolicyMutation()
  const [replaceApproval, replaceApprovalState] = useReplaceApprovalPolicyMutation()
  const [error, setError] = useState<string | null>(null)
  const [tierName, setTierName] = useState('')
  const [tierLimit, setTierLimit] = useState('5')

  async function onCreateTier(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await createTier({ name: tierName, defaultDiscountLimit: Number(tierLimit) }).unwrap()
      setTierName('')
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create tier'))
    }
  }

  async function onSaveDiscount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const form = new FormData(event.currentTarget)
    const policies: Array<{
      customerTierId: number | null
      categoryId: number | null
      maxDiscountPct: number
    }> = []
    for (const tier of tiersQuery.data ?? []) {
      policies.push({
        customerTierId: tier.id,
        categoryId: null,
        maxDiscountPct: Number(form.get(`tier-${tier.id}`)),
      })
    }
    for (const category of categoriesQuery.data ?? []) {
      policies.push({
        customerTierId: null,
        categoryId: category.id,
        maxDiscountPct: Number(form.get(`category-${category.id}`)),
      })
    }
    try {
      await replaceDiscount({ policies }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save discount policy'))
    }
  }

  async function onSaveApproval(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const form = new FormData(event.currentTarget)
    try {
      await replaceApproval({
        managerLineExcessPercent: Number(form.get('managerLine')),
        financeLineExcessPercent: Number(form.get('financeLine')),
        managerQuoteExcessPercent: Number(form.get('managerQuote')),
        financeQuoteExcessPercent: Number(form.get('financeQuote')),
      }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save approval policy'))
    }
  }

  function discountForTier(tierId: number) {
    return discountQuery.data?.find((row) => row.customerTierId === tierId)?.maxDiscountPct ?? 0
  }

  function discountForCategory(categoryId: number) {
    return discountQuery.data?.find((row) => row.categoryId === categoryId)?.maxDiscountPct ?? 0
  }

  const approval = approvalQuery.data

  return (
    <div className="stack">
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}

      <Panel title="Customer tiers">
        <p className="muted">
          Adding a tier also creates a default INR price list. Extra currencies are added on Catalog. Discount
          limits below are a separate policy — save them after you add a tier.
        </p>
        {tiersQuery.isLoading ? <p className="muted">Loading tiers…</p> : null}
        {(tiersQuery.data ?? []).length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Tier</th>
                <th>Default limit</th>
              </tr>
            </thead>
            <tbody>
              {(tiersQuery.data ?? []).map((tier) => (
                <tr key={tier.id}>
                  <td>
                    <span className="table-primary">{tier.name}</span>
                  </td>
                  <td>{tier.defaultDiscountLimit}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
        <form className="form" onSubmit={onCreateTier}>
          <label className="field">
            Name
            <input
              className="input"
              value={tierName}
              onChange={(event) => setTierName(event.target.value)}
              required
            />
          </label>
          <label className="field">
            Default discount limit %
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              value={tierLimit}
              onChange={(event) => setTierLimit(event.target.value)}
              required
            />
          </label>
          <div className="form-actions">
            <button className="button" type="submit" disabled={createTierState.isLoading}>
              {createTierState.isLoading ? 'Saving…' : 'Add tier'}
            </button>
          </div>
        </form>
      </Panel>

      <Panel title="Discount limits">
        <p className="muted">How far a sales rep may discount for each customer standing and product category.</p>
        {discountQuery.isLoading || tiersQuery.isLoading || categoriesQuery.isLoading ? (
          <p className="muted">Loading discount limits…</p>
        ) : null}
        {discountQuery.isError ? <p className="error">Could not load discount policy.</p> : null}
        {discountQuery.isSuccess && tiersQuery.isSuccess && categoriesQuery.isSuccess ? (
          <form
            className="form"
            onSubmit={onSaveDiscount}
            key={(discountQuery.data ?? []).map((row) => row.id).join('-') || 'discount-empty'}
          >
            <p className="muted">Customer tier limits</p>
            {(tiersQuery.data ?? []).map((tier) => (
              <label className="field" key={`tier-${tier.id}`}>
                {tier.name} %
                <input
                  className="input"
                  type="number"
                  min="0"
                  step="0.01"
                  name={`tier-${tier.id}`}
                  defaultValue={discountForTier(tier.id)}
                  required
                />
              </label>
            ))}
            <p className="muted">Category limits</p>
            {(categoriesQuery.data ?? []).map((category) => (
              <label className="field" key={`category-${category.id}`}>
                {category.name} %
                <input
                  className="input"
                  type="number"
                  min="0"
                  step="0.01"
                  name={`category-${category.id}`}
                  defaultValue={discountForCategory(category.id)}
                  required
                />
              </label>
            ))}
            <div className="form-actions">
              <button className="button" type="submit" disabled={replaceDiscountState.isLoading}>
                {replaceDiscountState.isLoading ? 'Saving…' : 'Save discount policy'}
              </button>
            </div>
          </form>
        ) : null}
      </Panel>

      <Panel title="Approval Policy">
        <p className="muted">
          Only Sales Manager and Finance approve quotes. Set how far over the discount ceiling a quote may go
          before each of them is required.
        </p>
        {approvalQuery.isLoading ? <p className="muted">Loading approval…</p> : null}
        {approvalQuery.isError ? <p className="error">Could not load approval policy.</p> : null}
        {approvalQuery.isSuccess && approval ? (
          <form className="form" onSubmit={onSaveApproval} key={approval.id}>
            <p className="muted">Sales Manager</p>
            <label className="field">
              Needed if any product is over its discount ceiling by %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                name="managerLine"
                defaultValue={approval.managerLineExcessPercent}
                required
              />
            </label>
            <label className="field">
              Needed if extra discount on the whole quote reaches %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                name="managerQuote"
                defaultValue={approval.managerQuoteExcessPercent}
                required
              />
            </label>
            <p className="muted">Finance</p>
            <label className="field">
              Needed if any product is over its discount ceiling by %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                name="financeLine"
                defaultValue={approval.financeLineExcessPercent}
                required
              />
            </label>
            <label className="field">
              Needed if extra discount on the whole quote reaches %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                name="financeQuote"
                defaultValue={approval.financeQuoteExcessPercent}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={replaceApprovalState.isLoading}>
                {replaceApprovalState.isLoading ? 'Saving…' : 'Save approval policy'}
              </button>
            </div>
          </form>
        ) : null}
      </Panel>
    </div>
  )
}
