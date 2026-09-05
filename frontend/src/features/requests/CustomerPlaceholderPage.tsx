import { Panel } from '../../components/common/Panel'

export function CustomerPlaceholderPage({ title }: { title: string }) {
  return (
    <Panel title={title}>
      <p className="muted">This screen will be added in a later slice. Nothing is stored here yet.</p>
      <p>0</p>
    </Panel>
  )
}
