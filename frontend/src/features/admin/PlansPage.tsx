import { useState, type FormEvent } from 'react'
import { Panel } from '../../components/common/Panel'
import { useCreateSubscriptionPlanMutation, useGetSubscriptionPlansQuery } from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { CancellationRule, PlanCycle, ProrationRule } from './types'

export function PlansPage() {
  const plansQuery = useGetSubscriptionPlansQuery()
  const [createPlan, createPlanState] = useCreateSubscriptionPlanMutation()
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [cycle, setCycle] = useState<PlanCycle>('MONTHLY')
  const [prorationRule, setProrationRule] = useState<ProrationRule>('PRORATE_DAYS')
  const [cancellationRule, setCancellationRule] = useState<CancellationRule>('CREDIT_NOTE')

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await createPlan({ name, cycle, prorationRule, cancellationRule }).unwrap()
      setName('')
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create plan'))
    }
  }

  return (
    <div className="stack">
      <Panel title="Subscription plans">
        {plansQuery.isLoading ? <p className="muted">Loading plans…</p> : null}
        {plansQuery.isError ? <p className="error">Could not load plans.</p> : null}
        {(plansQuery.data ?? []).length === 0 ? <p className="muted">No plans yet.</p> : null}
        <ul>
          {(plansQuery.data ?? []).map((plan) => (
            <li key={plan.id}>
              {plan.name}
              <span className="muted">
                {' '}
                · {plan.cycle} · {plan.prorationRule} · {plan.cancellationRule}
              </span>
            </li>
          ))}
        </ul>
        <form className="form" onSubmit={onCreate}>
          {error ? (
            <p className="error" role="alert">
              {error}
            </p>
          ) : null}
          <label className="field">
            Name
            <input className="input" value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="field">
            Cycle
            <select
              className="input"
              value={cycle}
              onChange={(event) => setCycle(event.target.value as PlanCycle)}
            >
              <option value="MONTHLY">MONTHLY</option>
              <option value="QUARTERLY">QUARTERLY</option>
              <option value="YEARLY">YEARLY</option>
            </select>
          </label>
          <label className="field">
            Proration rule
            <select
              className="input"
              value={prorationRule}
              onChange={(event) => setProrationRule(event.target.value as ProrationRule)}
            >
              <option value="PRORATE_DAYS">PRORATE_DAYS</option>
              <option value="CHARGE_FULL">CHARGE_FULL</option>
            </select>
          </label>
          <label className="field">
            Cancellation rule
            <select
              className="input"
              value={cancellationRule}
              onChange={(event) => setCancellationRule(event.target.value as CancellationRule)}
            >
              <option value="CREDIT_NOTE">CREDIT_NOTE</option>
              <option value="REFUND">REFUND</option>
              <option value="FORFEIT">FORFEIT</option>
            </select>
          </label>
          <div className="form-actions">
            <button className="button" type="submit" disabled={createPlanState.isLoading}>
              {createPlanState.isLoading ? 'Saving…' : 'Add plan'}
            </button>
          </div>
        </form>
      </Panel>
    </div>
  )
}
