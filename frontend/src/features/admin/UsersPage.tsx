import { useState, type FormEvent } from 'react'
import { Modal } from '../../components/common/Modal'
import { Panel } from '../../components/common/Panel'
import {
  useCreateTeamUserMutation,
  useGetTeamUsersQuery,
  usePatchTeamUserMutation,
} from '../../stores/api/adminUsersApi'
import { useAppSelector } from '../../stores/hooks'
import { apiErrorMessage } from '../../types/api'
import { CREATABLE_TEAM_ROLES, teamRoleLabel, type CreatableTeamRole } from './teamUserTypes'

export function UsersPage() {
  const currentUser = useAppSelector((state) => state.auth.user)
  const usersQuery = useGetTeamUsersQuery()
  const [createUser, createUserState] = useCreateTeamUserMutation()
  const [patchUser, patchUserState] = usePatchTeamUserMutation()
  const [modalOpen, setModalOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<CreatableTeamRole>('SALES_REP')

  const users = usersQuery.data ?? []

  function closeModal() {
    setModalOpen(false)
    setError(null)
  }

  async function onCreateUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    try {
      await createUser({ name: name.trim(), email: email.trim(), password, role }).unwrap()
      setName('')
      setEmail('')
      setPassword('')
      setRole('SALES_REP')
      closeModal()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create user'))
    }
  }

  async function onToggleActive(userId: number, active: boolean) {
    setError(null)
    try {
      await patchUser({ id: userId, body: { active } }).unwrap()
    } catch (err) {
      setError(apiErrorMessage(err, active ? 'Could not reactivate user' : 'Could not deactivate user'))
    }
  }

  function canManage(userId: number, userRole: string) {
    return userRole !== 'ADMIN' && currentUser?.id !== userId
  }

  return (
    <div className="stack">
      {error ? <p className="form-error">{error}</p> : null}
      <Panel
        title="Team"
        badge={
          <button className="button" type="button" onClick={() => setModalOpen(true)}>
            Add user
          </button>
        }
      >
        {usersQuery.isLoading ? <p className="muted">Loading team…</p> : null}
        {usersQuery.isError ? <p className="form-error">Could not load team members.</p> : null}
        {users.length === 0 && !usersQuery.isLoading ? <p className="muted">No team members yet.</p> : null}
        {users.length > 0 ? (
          <table className="board-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td>{teamRoleLabel(user.role)}</td>
                  <td>{user.active ? 'Active' : 'Inactive'}</td>
                  <td>
                    {canManage(user.id, user.role) ? (
                      user.active ? (
                        <button
                          className="button button-secondary"
                          type="button"
                          disabled={patchUserState.isLoading}
                          onClick={() => onToggleActive(user.id, false)}
                        >
                          Deactivate
                        </button>
                      ) : (
                        <button
                          className="button button-secondary"
                          type="button"
                          disabled={patchUserState.isLoading}
                          onClick={() => onToggleActive(user.id, true)}
                        >
                          Reactivate
                        </button>
                      )
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Panel>

      {modalOpen ? (
        <Modal title="Add team member" onClose={closeModal}>
          <form className="stack" onSubmit={onCreateUser}>
            <label className="field">
              <span>Name</span>
              <input value={name} onChange={(event) => setName(event.target.value)} required />
            </label>
            <label className="field">
              <span>Email</span>
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            </label>
            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={8}
                required
              />
            </label>
            <label className="field">
              <span>Role</span>
              <select value={role} onChange={(event) => setRole(event.target.value as CreatableTeamRole)}>
                {CREATABLE_TEAM_ROLES.map((option) => (
                  <option key={option} value={option}>
                    {teamRoleLabel(option)}
                  </option>
                ))}
              </select>
            </label>
            <p className="muted">Share the password with the new user so they can sign in.</p>
            {error ? <p className="form-error">{error}</p> : null}
            <div className="form-actions">
              <button className="button button-secondary" type="button" onClick={closeModal}>
                Cancel
              </button>
              <button className="button" type="submit" disabled={createUserState.isLoading}>
                Create user
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </div>
  )
}
