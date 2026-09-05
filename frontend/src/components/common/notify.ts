import { toast } from 'sonner'

export function notifyMoved(label: string) {
  toast.success(`Moved to ${label}`)
}

export function notifyBlocked(label: string) {
  toast.error(`Cannot move this ticket to ${label}`)
}

export function notifyError(message: string) {
  toast.error(message)
}

export function notifyOk(message: string) {
  toast.success(message)
}
