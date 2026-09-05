export type StatusTone = 'neutral' | 'info' | 'warning' | 'success' | 'danger'

export function StatusBadge({ label, tone = 'neutral' }: { label: string; tone?: StatusTone }) {
  return <span className={`status-badge status-badge-${tone}`}>{label}</span>
}

export function toneForRequestStatus(status: string): StatusTone {
  switch (status) {
    case 'SUBMITTED':
    case 'UNDER_REVIEW':
    case 'QUOTED':
      return 'info'
    case 'CANCELLED':
      return 'danger'
    case 'DRAFT':
    case 'CLOSED':
    default:
      return 'neutral'
  }
}

export function toneForQuotationStatus(status: string): StatusTone {
  switch (status) {
    case 'PENDING_APPROVAL':
      return 'warning'
    case 'NEGOTIATION':
      return 'info'
    case 'APPROVED':
    case 'CONFIRMED':
      return 'success'
    case 'REJECTED':
    case 'CANCELLED':
      return 'danger'
    case 'DRAFT':
    default:
      return 'neutral'
  }
}

export function toneForTicket(requestStatus: string, quotationStatus?: string | null): StatusTone {
  if (quotationStatus) {
    return toneForQuotationStatus(quotationStatus)
  }
  return toneForRequestStatus(requestStatus)
}
