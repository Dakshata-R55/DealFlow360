import { Panel } from '../components/common/Panel'
import { StatusBadge } from '../components/ui/StatusBadge'
import { useHealth } from '../hooks/useHealth'
import type { HealthData } from '../types/health'

function HealthFacts({ data, timestamp }: { data: HealthData; timestamp?: string }) {
  return (
    <dl className="facts">
      <div>
        <dt>Backend</dt>
        <dd>{data.backend}</dd>
      </div>
      <div>
        <dt>Database</dt>
        <dd>{data.database}</dd>
      </div>
      {timestamp ? (
        <div>
          <dt>Timestamp</dt>
          <dd>{timestamp}</dd>
        </div>
      ) : null}
    </dl>
  )
}

export function HomePage() {
  const { state, reload } = useHealth()

  return (
    <Panel title="API health">
      <StatusBadge state={state} />
      {state.status === 'loading' ? (
        <p className="muted">Requesting GET /api/health…</p>
      ) : null}
      {state.status === 'ok' ? <HealthFacts data={state.data} timestamp={state.timestamp} /> : null}
      {state.status === 'error' ? (
        <>
          {state.data ? <HealthFacts data={state.data} timestamp={state.timestamp} /> : null}
          <p className="error" role="alert">
            {state.message}
          </p>
        </>
      ) : null}
      <button type="button" className="button" onClick={reload}>
        Recheck
      </button>
    </Panel>
  )
}
