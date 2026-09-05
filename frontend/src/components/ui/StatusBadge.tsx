import type { HealthState } from '../../types/health'

export function StatusBadge({ state }: { state: HealthState }) {
  const label =
    state.status === 'loading'
      ? 'Checking API'
      : state.status === 'ok'
        ? 'API reachable'
        : 'API unreachable'

  return <span className={`badge badge-${state.status}`}>{label}</span>
}
