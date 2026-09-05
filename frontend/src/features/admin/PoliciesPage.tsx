import { useState, type FormEvent } from 'react'
import { Modal } from '../../components/common/Modal'
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
type PolicyModal = 'standing-add' | 'standing-edit' | 'category' | 'approval' | null

type PolicyRow = {
  customerTierId: number | null
  categoryId: number | null
  maxDiscountPct: number
}

export function PoliciesPage() {
  const tiersQuery = useGetCustomerTiersQuery()
  const categoriesQuery = useGetCategoriesQuery()
  const discountQuery = useGetDiscountPolicyQuery()
  const approvalQuery = useGetApprovalPolicyQuery()
  const [createTier, createTierState] = useCreateCustomerTierMutation()
  const [replaceDiscount, replaceDiscountState] = useReplaceDiscountPolicyMutation()
  const [replaceApproval, replaceApprovalState] = useReplaceApprovalPolicyMutation()
  const [modal, setModal] = useState<PolicyModal>(null)
  const [error, setError] = useState<string | null>(null)
  const [tierName, setTierName] = useState('')
  const [tierLimit, setTierLimit] = useState('5')
  const [editStandingId, setEditStandingId] = useState<number | null>(null)
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [categoryLimit, setCategoryLimit] = useState('')

  const approval = approvalQuery.data
  const tiers = tiersQuery.data ?? []
  const categories = categoriesQuery.data ?? []
  const policies = discountQuery.data ?? []
  const selectedCategory = categories.find((row) => row.id === categoryId) ?? categories[0] ?? null

  function closeModal() {
    setModal(null)
    setError(null)
    setEditStandingId(null)
  }

  function ceilingForStanding(tierId: number, fallback: number) {
    return policies.find((row) => row.customerTierId === tierId)?.maxDiscountPct ?? fallback
  }

  function ceilingForCategory(id: number) {
    return policies.find((row) => row.categoryId === id)?.maxDiscountPct ?? 0
  }

  function buildPolicies(patch: PolicyRow): PolicyRow[] {
    const rows: PolicyRow[] = []
    let standingPatched = false
    for (const tier of tiers) {
      const pct =
        patch.customerTierId === tier.id ? patch.maxDiscountPct : ceilingForStanding(tier.id, tier.defaultDiscountLimit)
      if (patch.customerTierId === tier.id) {
        standingPatched = true
      }
      rows.push({ customerTierId: tier.id, categoryId: null, maxDiscountPct: pct })
    }
    if (patch.customerTierId != null && !standingPatched) {
      rows.push({ customerTierId: patch.customerTierId, categoryId: null, maxDiscountPct: patch.maxDiscountPct })
    }
    for (const category of categories) {
      const pct = patch.categoryId === category.id ? patch.maxDiscountPct : ceilingForCategory(category.id)
      rows.push({ customerTierId: null, categoryId: category.id, maxDiscountPct: pct })
    }
    return rows
  }

  async function onCreateStanding(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const pct = Number(tierLimit)
    try {
      const created = await createTier({ name: tierName.trim(), defaultDiscountLimit: pct }).unwrap()
      await replaceDiscount({
        policies: buildPolicies({ customerTierId: created.id, categoryId: null, maxDiscountPct: pct }),
      }).unwrap()
      setTierName('')
      setTierLimit('5')
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not add standing'))
    }
  }

  async function onSaveStanding(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (editStandingId == null) {
      return
    }
    setError(null)
    try {
      await replaceDiscount({
        policies: buildPolicies({
          customerTierId: editStandingId,
          categoryId: null,
          maxDiscountPct: Number(tierLimit),
        }),
      }).unwrap()
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save ceiling'))
    }
  }

  async function onSaveCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (selectedCategory == null) {
      return
    }
    setError(null)
    try {
      await replaceDiscount({
        policies: buildPolicies({
          customerTierId: null,
          categoryId: selectedCategory.id,
          maxDiscountPct: Number(categoryLimit),
        }),
      }).unwrap()
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save category ceiling'))
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
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save who must approve'))
    }
  }

  const editStanding = tiers.find((tier) => tier.id === editStandingId)
  const savingDiscount = replaceDiscountState.isLoading

  return (
    <div className="stack">
      <Panel
        title="Customer standing"
        badge={
          <button className="button" type="button" onClick={() => setModal('standing-add')}>
            Add standing
          </button>
        }
      >
        <p className="muted">How far a sales rep may discount for this customer standing. Extra currencies are added on Catalog.</p>
        {tiersQuery.isLoading ? <p className="muted">Loading standing…</p> : null}
        {tiers.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Standing</th>
                <th>Discount ceiling</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {tiers.map((tier) => (
                <tr key={tier.id}>
                  <td>
                    <span className="table-primary">{tier.name}</span>
                  </td>
                  <td>{ceilingForStanding(tier.id, tier.defaultDiscountLimit)}%</td>
                  <td>
                    <button
                      className="button button-secondary"
                      type="button"
                      onClick={() => {
                        setEditStandingId(tier.id)
                        setTierLimit(String(ceilingForStanding(tier.id, tier.defaultDiscountLimit)))
                        setModal('standing-edit')
                      }}
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : tiersQuery.isSuccess ? (
          <p className="muted">No standing yet.</p>
        ) : null}
      </Panel>

      <Panel title="Category ceilings">
        <p className="muted">Pick a product category to see or change its discount ceiling. Standings are not listed here.</p>
        {categoriesQuery.isLoading || discountQuery.isLoading ? <p className="muted">Loading categories…</p> : null}
        {categoriesQuery.isError ? <p className="error">Could not load categories.</p> : null}
        {categories.length === 0 && categoriesQuery.isSuccess ? (
          <p className="muted">Add categories on Catalog first.</p>
        ) : null}
        {selectedCategory ? (
          <div className="policy-picker">
            <label className="field">
              Category
              <select
                className="input"
                value={selectedCategory.id}
                onChange={(event) => setCategoryId(Number(event.target.value))}
              >
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>
            <p className="policy-picker-value">
              Ceiling <strong>{ceilingForCategory(selectedCategory.id)}%</strong>
            </p>
            <button
              className="button"
              type="button"
              onClick={() => {
                setCategoryLimit(String(ceilingForCategory(selectedCategory.id)))
                setModal('category')
              }}
            >
              Edit
            </button>
          </div>
        ) : null}
      </Panel>

      <Panel
        title="Who must approve"
        badge={
          approvalQuery.isSuccess && approval ? (
            <button className="button" type="button" onClick={() => setModal('approval')}>
              Edit
            </button>
          ) : undefined
        }
      >
        <p className="muted">
          Sales Manager and Finance step in only when a quote goes past the ceilings above.
        </p>
        {approvalQuery.isLoading ? <p className="muted">Loading…</p> : null}
        {approvalQuery.isError ? <p className="error">Could not load approval settings.</p> : null}
        {approvalQuery.isSuccess && approval ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Role</th>
                <th>One product over ceiling</th>
                <th>Whole quote over ceiling</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <span className="table-primary">Sales Manager</span>
                </td>
                <td>{approval.managerLineExcessPercent}%</td>
                <td>{approval.managerQuoteExcessPercent}%</td>
              </tr>
              <tr>
                <td>
                  <span className="table-primary">Finance</span>
                </td>
                <td>{approval.financeLineExcessPercent}%</td>
                <td>{approval.financeQuoteExcessPercent}%</td>
              </tr>
            </tbody>
          </table>
        ) : null}
      </Panel>

      {modal === 'standing-add' ? (
        <Modal title="Add standing" onClose={closeModal}>
          <form className="form" onSubmit={onCreateStanding}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
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
              Discount ceiling %
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
              <button className="button" type="submit" disabled={createTierState.isLoading || savingDiscount}>
                {createTierState.isLoading || savingDiscount ? 'Saving…' : 'Add standing'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'standing-edit' && editStanding ? (
        <Modal title={`Edit ${editStanding.name}`} onClose={closeModal}>
          <form className="form" onSubmit={onSaveStanding}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <label className="field field-full">
              Discount ceiling %
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
              <button className="button" type="submit" disabled={savingDiscount}>
                {savingDiscount ? 'Saving…' : 'Save ceiling'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'category' && selectedCategory ? (
        <Modal title={`${selectedCategory.name} ceiling`} onClose={closeModal}>
          <form className="form" onSubmit={onSaveCategory}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <label className="field field-full">
              Discount ceiling %
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={categoryLimit}
                onChange={(event) => setCategoryLimit(event.target.value)}
                required
              />
            </label>
            <div className="form-actions">
              <button className="button" type="submit" disabled={savingDiscount}>
                {savingDiscount ? 'Saving…' : 'Save ceiling'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {modal === 'approval' && approval ? (
        <Modal title="Who must approve" onClose={closeModal}>
          <form className="form" onSubmit={onSaveApproval} key={approval.id}>
            {error ? (
              <p className="error field-full" role="alert">
                {error}
              </p>
            ) : null}
            <p className="muted field-full">Sales Manager</p>
            <label className="field">
              One product over ceiling %
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
              Whole quote over ceiling %
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
            <p className="muted field-full">Finance</p>
            <label className="field">
              One product over ceiling %
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
              Whole quote over ceiling %
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
                {replaceApprovalState.isLoading ? 'Saving…' : 'Save'}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
