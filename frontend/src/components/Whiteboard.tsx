import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * 공유 화이트보드 — LiveKit 데이터 채널로 스트로크를 전파한다.
 * 좌표는 0..1 정규화라 참가자마다 화면 크기가 달라도 동일하게 보인다.
 * (히스토리는 없음: 입장 이후의 드로잉만 보인다)
 */

export interface WhiteboardStroke {
  points: [number, number][]
  color: string
  width: number
}

export type WhiteboardMessage =
  | { type: 'wb'; kind: 'stroke'; stroke: WhiteboardStroke }
  | { type: 'wb'; kind: 'clear' }

const CANVAS_W = 1600
const CANVAS_H = 900
const COLORS = ['#ffffff', '#8ea5f8', '#f87171', '#fbbf24']
const ERASER = '#131a3a' // 보드 배경색

export default function Whiteboard({
  onPublish,
  registerRemoteHandler,
  onClose,
}: {
  onPublish: (message: WhiteboardMessage) => void
  registerRemoteHandler: (handler: (message: WhiteboardMessage) => void) => () => void
  onClose: () => void
}) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const drawing = useRef(false)
  const currentPoints = useRef<[number, number][]>([])
  const [color, setColor] = useState(COLORS[1])
  const [eraser, setEraser] = useState(false)

  const strokeColor = eraser ? ERASER : color
  const strokeWidth = eraser ? 36 : 4

  const drawStroke = useCallback((stroke: WhiteboardStroke) => {
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx || stroke.points.length === 0) return
    ctx.strokeStyle = stroke.color
    ctx.lineWidth = stroke.width
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.beginPath()
    ctx.moveTo(stroke.points[0][0] * CANVAS_W, stroke.points[0][1] * CANVAS_H)
    for (const [x, y] of stroke.points.slice(1)) {
      ctx.lineTo(x * CANVAS_W, y * CANVAS_H)
    }
    ctx.stroke()
  }, [])

  const clearCanvas = useCallback(() => {
    const ctx = canvasRef.current?.getContext('2d')
    if (!ctx) return
    ctx.fillStyle = ERASER
    ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)
  }, [])

  // 최초 마운트에서만 캔버스 초기화 (리렌더로 지워지면 안 된다)
  useEffect(() => {
    clearCanvas()
  }, [clearCanvas])

  // 원격 스트로크 수신 등록 (초기화와 분리)
  useEffect(() => {
    return registerRemoteHandler((message) => {
      if (message.kind === 'stroke') drawStroke(message.stroke)
      else if (message.kind === 'clear') clearCanvas()
    })
  }, [registerRemoteHandler, drawStroke, clearCanvas])

  const toNormalized = (e: React.PointerEvent): [number, number] => {
    const rect = canvasRef.current!.getBoundingClientRect()
    return [(e.clientX - rect.left) / rect.width, (e.clientY - rect.top) / rect.height]
  }

  const onPointerDown = (e: React.PointerEvent) => {
    drawing.current = true
    currentPoints.current = [toNormalized(e)]
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onPointerMove = (e: React.PointerEvent) => {
    if (!drawing.current) return
    const point = toNormalized(e)
    const points = currentPoints.current
    points.push(point)
    // 로컬은 즉시 그리기 (마지막 두 점만 이어서)
    if (points.length >= 2) {
      drawStroke({ points: [points[points.length - 2], point], color: strokeColor, width: strokeWidth })
    }
  }

  const onPointerUp = () => {
    if (!drawing.current) return
    drawing.current = false
    const points = currentPoints.current
    currentPoints.current = []
    if (points.length < 2) return
    // 전송량 절약: 점을 절반으로 샘플링 (500포인트 상한)
    const sampled = points.filter((_, i) => i % 2 === 0 || i === points.length - 1).slice(0, 500)
    onPublish({ type: 'wb', kind: 'stroke', stroke: { points: sampled, color: strokeColor, width: strokeWidth } })
  }

  const clearAll = () => {
    clearCanvas()
    onPublish({ type: 'wb', kind: 'clear' })
  }

  return (
    <div className="absolute inset-4 z-20 flex flex-col overflow-hidden rounded-[16px] border border-white/15 bg-[#131a3a] shadow-2xl">
      <div className="flex items-center justify-between border-b border-white/10 px-4 py-2.5">
        <p className="text-[13px] font-extrabold text-white">✏️ 공유 화이트보드</p>
        <div className="flex items-center gap-2">
          {COLORS.map((candidate) => (
            <button
              key={candidate}
              onClick={() => {
                setColor(candidate)
                setEraser(false)
              }}
              className={`h-6 w-6 rounded-full border-2 transition-transform hover:scale-110 ${
                !eraser && color === candidate ? 'border-white' : 'border-transparent'
              }`}
              style={{ background: candidate }}
            />
          ))}
          <button
            onClick={() => setEraser(true)}
            className={`rounded-full px-2.5 py-1 text-[11.5px] font-bold transition-colors ${
              eraser ? 'bg-white text-[#131a3a]' : 'bg-white/10 text-white/70 hover:bg-white/20'
            }`}
          >
            지우개
          </button>
          <button
            onClick={clearAll}
            className="rounded-full bg-white/10 px-2.5 py-1 text-[11.5px] font-bold text-white/70 hover:bg-danger hover:text-white"
          >
            전체 지우기
          </button>
          <button
            onClick={onClose}
            className="ml-1 rounded-full bg-white/10 px-2.5 py-1 text-[11.5px] font-bold text-white/70 hover:bg-white/25"
          >
            닫기 ✕
          </button>
        </div>
      </div>
      <canvas
        ref={canvasRef}
        width={CANVAS_W}
        height={CANVAS_H}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerLeave={onPointerUp}
        className="h-full w-full flex-1 cursor-crosshair touch-none"
      />
    </div>
  )
}
