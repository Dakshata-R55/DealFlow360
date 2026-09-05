import { useEffect, type ReactNode } from 'react'

export function Drawer({ onClose, children }: { onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer" onClick={(event) => event.stopPropagation()}>
        <div className="drawer-head">
          <button className="button button-secondary" type="button" onClick={onClose}>
            Close
          </button>
        </div>
        {children}
      </aside>
    </div>
  )
}
