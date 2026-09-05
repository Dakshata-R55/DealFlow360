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
import type { RiskLevel } from './types'

const RISK_LEVELS: RiskLevel[] = ['NONE', 'MEDIUM', 'HIGH']

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
        policies: RISK_LEVELS.map((riskLevel) => ({
          riskLevel,
          minScore: Number(form.get(`${riskLevel}-min`)),
          maxScore: Number(form.get(`${riskLevel}-max`)),
          requiresManager: form.get(`${riskLevel}-manager`) === 'on',
          requiresFinance: form.get(`${riskLevel}-finance`) === 'on',
          hardLineExcessThreshold: Number(form.get(`${riskLevel}-hard`)),
        })),
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

  function approvalRow(riskLevel: RiskLevel) {
    return approvalQuery.data?.find((row) => row.riskLevel === riskLevel)
  }

  return (
    <div className="stack">
      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}

      <Panel title="Customer tiers">
        {tiersQuery.isLoading ? <p className="muted">Loading tiers…</p> : null}
        <ul>
          {(tiersQuery.data ?? []).map((tier) => (
            <li key={tier.id}>
              {tier.name}
              <span className="muted"> · default limit {tier.defaultDiscountLimit}%</span>
            </li>
          ))}
        </ul>
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
        <p className="muted">Allowed discount later is min(tier limit, category limit).</p>
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
            {(tiersQuery.data ?? []).map((tier) => (
              <label className="field" key={`tier-${tier.id}`}>
                {tier.name} (tier) %
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
            {(categoriesQuery.data ?? []).map((category) => (
              <label className="field" key={`category-${category.id}`}>
                {category.name} (category) %
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

      <Panel title="Approval thresholds">
        {approvalQuery.isLoading ? <p className="muted">Loading approval…</p> : null}
        {approvalQuery.isError ? <p className="error">Could not load approval policy.</p> : null}
        {approvalQuery.isSuccess ? (
          <form
            className="form"
            onSubmit={onSaveApproval}
            key={(approvalQuery.data ?? []).map((row) => row.id).join('-') || 'approval-empty'}
          >
            {RISK_LEVELS.map((riskLevel) => {
              const row = approvalRow(riskLevel)
              return (
                <fieldset key={riskLevel} className="form">
                  <legend>{riskLevel}</legend>
                  <label className="field">
                    Min score
                    <input
                      className="input"
                      type="number"
                      min="0"
                      step="0.01"
                      name={`${riskLevel}-min`}
                      defaultValue={row?.minScore ?? 0}
                      required
                    />
                  </label>
                  <label className="field">
                    Max score
                    <input
                      className="input"
                      type="number"
                      min="0"
                      step="0.01"
                      name={`${riskLevel}-max`}
                      defaultValue={row?.maxScore ?? 0}
                      required
                    />
                  </label>
                  <label className="field">
                    Hard line excess
                    <input
                      className="input"
                      type="number"
                      min="0"
                      step="0.01"
                      name={`${riskLevel}-hard`}
                      defaultValue={row?.hardLineExcessThreshold ?? 0}
                      required
                    />
                  </label>
                  <label className="field">
                    <input
                      type="checkbox"
                      name={`${riskLevel}-manager`}
                      defaultChecked={row?.requiresManager ?? false}
                    />
                    Requires manager
                  </label>
                  <label className="field">
                    <input
                      type="checkbox"
                      name={`${riskLevel}-finance`}
                      defaultChecked={row?.requiresFinance ?? false}
                    />
                    Requires finance
                  </label>
                </fieldset>
              )
            })}
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
