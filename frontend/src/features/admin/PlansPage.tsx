import { useState, type FormEvent } from 'react'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import {
  useCreateSubscriptionPlanMutation,
  useGetSubscriptionPlansQuery,
  useUpdateSubscriptionPlanMutation,
} from '../../stores/api/configApi'
import { apiErrorMessage } from '../../types/api'
import type { CancellationRule, PlanCycle, ProrationRule, SubscriptionPlan } from './types'

export function PlansPage() {
  const plansQuery = useGetSubscriptionPlansQuery()
  const [createPlan, createPlanState] = useCreateSubscriptionPlanMutation()
  const [updatePlan, updatePlanState] = useUpdateSubscriptionPlanMutation()
  const [editing, setEditing] = useState<SubscriptionPlan | null>(null)
  const [modal, setModal] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [cycle, setCycle] = useState<PlanCycle>('MONTHLY')
  const [prorationRule, setProrationRule] = useState<ProrationRule>('PRORATE_DAYS')
  const [cancellationRule, setCancellationRule] = useState<CancellationRule>('CREDIT_NOTE')

  function closeModal() {
    setModal(false)
    setEditing(null)
    setError(null)
  }

  function openAdd() {
    setEditing(null)
    setName('')
    setCycle('MONTHLY')
    setProrationRule('PRORATE_DAYS')
    setCancellationRule('CREDIT_NOTE')
    setError(null)
    setModal(true)
  }

  function openEdit(plan: SubscriptionPlan) {
    setEditing(plan)
    setName(plan.name)
    setCycle(plan.cycle)
    setProrationRule(plan.prorationRule)
    setCancellationRule(plan.cancellationRule)
    setError(null)
    setModal(true)
  }

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const body = { name: name.trim(), cycle, prorationRule, cancellationRule }
    try {
      if (editing) {
        await updatePlan({ id: editing.id, body }).unwrap()
      } else {
        await createPlan(body).unwrap()
      }
      setName('')
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, editing ? 'Could not save plan' : 'Could not create plan'))
    }
  }

  const plans = plansQuery.data ?? []
  const saving = createPlanState.isLoading || updatePlanState.isLoading

  return (
    <div className="stack">
      <Panel
        title="Subscription plans"
        badge={
          <button className="button" type="button" onClick={openAdd}>
            Add plan
          </button>
        }
      >
        <p className="muted">Click a plan to change cycle, proration, or cancellation.</p>
        {plansQuery.isLoading ? <p className="muted">Loading plans…</p> : null}
        {plansQuery.isError ? <p className="error">Could not load plans.</p> : null}
        {plans.length > 0 ? (
          <div className="policy-list-scroll">
            <table className="board-table">
              <thead>
                <tr>
                  <th>Plan</th>
                  <th>Cycle</th>
                  <th>Proration</th>
                  <th>Cancellation</th>
                </tr>
              </thead>
              <tbody>
                {plans.map((plan) => (
                  <tr key={plan.id} className="board-row" onClick={() => openEdit(plan)}>
                    <td>
                      <span className="table-primary">{plan.name}</span>
                    </td>
                    <td>{plan.cycle}</td>
                    <td>{plan.prorationRule}</td>
                    <td>{plan.cancellationRule}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : plansQuery.isSuccess ? (
          <p className="muted">No plans yet.</p>
        ) : null}
      </Panel>

      {modal ? (
        <Modal title={editing ? 'Edit plan' : 'Add plan'} onClose={closeModal}>
          <form className="form" onSubmit={(event) => void onSave(event)}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <label className="field field-full">
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
            <label className="field field-full">
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
              <button className="button" type="submit" disabled={saving}>
                {saving ? 'Saving…' : editing ? 'Save plan' : 'Add plan'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
