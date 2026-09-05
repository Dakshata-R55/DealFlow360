import { useCallback, useEffect, useRef, type PointerEvent as ReactPointerEvent, type WheelEvent as ReactWheelEvent } from 'react'

const MIN = 1
const FLOOR_MAX = 1.75

function reducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function layoutRect(node: HTMLElement) {
  const prev = node.style.transform
  node.style.transform = 'none'
  const rect = node.getBoundingClientRect()
  node.style.transform = prev
  return rect
}

function maxScaleFor(node: HTMLElement) {
  const rect = layoutRect(node)
  if (!rect.width || !rect.height) {
    return FLOOR_MAX
  }
  const cover = Math.max(window.innerWidth / rect.width, window.innerHeight / rect.height)
  return Math.max(FLOOR_MAX, cover)
}

export function useScreenPinch() {
  const deviceRef = useRef<HTMLDivElement>(null)
  const scale = useRef(1)
  const x = useRef(0)
  const y = useRef(0)
  const pointers = useRef(new Map<number, { x: number; y: number }>())
  const pinchStart = useRef<{ dist: number; scale: number } | null>(null)
  const panStart = useRef<{ x: number; y: number; ox: number; oy: number } | null>(null)

  const clamp = useCallback(() => {
    const node = deviceRef.current
    if (!node || scale.current <= MIN) {
      x.current = 0
      y.current = 0
      return
    }
    const rect = layoutRect(node)
    const s = scale.current
    const vw = window.innerWidth
    const vh = window.innerHeight
    const scaledW = rect.width * s
    const scaledH = rect.height * s
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2

    function axis(tx: number, center: number, scaled: number, view: number) {
      if (scaled <= view) {
        const min = scaled / 2 - center
        const max = view - scaled / 2 - center
        return Math.min(max, Math.max(min, tx))
      }
      const min = view - scaled / 2 - center
      const max = scaled / 2 - center
      return Math.min(max, Math.max(min, tx))
    }

    x.current = axis(x.current, cx, scaledW, vw)
    y.current = axis(y.current, cy, scaledH, vh)
  }, [])

  const apply = useCallback(() => {
    const node = deviceRef.current
    if (!node) {
      return
    }
    clamp()
    node.style.transform = `translate(${x.current}px, ${y.current}px) scale(${scale.current})`
  }, [clamp])

  const reset = useCallback(() => {
    scale.current = 1
    x.current = 0
    y.current = 0
    apply()
  }, [apply])

  function onPointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (reducedMotion()) {
      return
    }
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
    if (pointers.current.size === 2) {
      event.currentTarget.setPointerCapture(event.pointerId)
      const pts = [...pointers.current.values()]
      const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y)
      pinchStart.current = { dist, scale: scale.current }
      panStart.current = null
      return
    }
    const onControl = (event.target as HTMLElement).closest('input, button, a, textarea, select')
    if (!onControl && scale.current > MIN) {
      event.currentTarget.setPointerCapture(event.pointerId)
      panStart.current = { x: event.clientX, y: event.clientY, ox: x.current, oy: y.current }
    }
  }

  function onPointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    if (!pointers.current.has(event.pointerId)) {
      return
    }
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
    const node = deviceRef.current
    if (pointers.current.size === 2 && pinchStart.current && node) {
      const pts = [...pointers.current.values()]
      const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y)
      const cap = maxScaleFor(node)
      scale.current = Math.min(cap, Math.max(MIN, pinchStart.current.scale * (dist / pinchStart.current.dist)))
      apply()
      return
    }
    if (panStart.current && scale.current > MIN) {
      x.current = panStart.current.ox + (event.clientX - panStart.current.x)
      y.current = panStart.current.oy + (event.clientY - panStart.current.y)
      apply()
    }
  }

  function onPointerUp(event: ReactPointerEvent<HTMLDivElement>) {
    pointers.current.delete(event.pointerId)
    pinchStart.current = null
    panStart.current = null
  }

  function onWheel(event: ReactWheelEvent<HTMLDivElement>) {
    if (reducedMotion() || (!event.ctrlKey && !event.metaKey)) {
      return
    }
    event.preventDefault()
    const node = deviceRef.current
    if (!node) {
      return
    }
    const cap = maxScaleFor(node)
    scale.current = Math.min(cap, Math.max(MIN, scale.current - event.deltaY * 0.008))
    apply()
  }

  function onDoubleClick() {
    reset()
  }

  useEffect(() => {
    const node = deviceRef.current
    if (!node) {
      return
    }
    const wheel = (event: WheelEvent) => {
      if (reducedMotion() || (!event.ctrlKey && !event.metaKey)) {
        return
      }
      event.preventDefault()
    }
    node.addEventListener('wheel', wheel, { passive: false })
    return () => node.removeEventListener('wheel', wheel)
  }, [])

  return { deviceRef, onPointerDown, onPointerMove, onPointerUp, onWheel, onDoubleClick }
}
