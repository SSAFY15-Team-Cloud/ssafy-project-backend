import { useCallback, useEffect, useRef, useState } from 'react'
import {
  cellToUnicode,
  connectSerial,
  disconnectSerial,
  serialConnected,
  serialSetCell,
  serialSupported,
  speak,
} from '../lib/braille'

/**
 * 6점 점자 키패드.
 * - 화면: 2열×3행 (왼쪽 1·2·3점, 오른쪽 4·5·6점) — 실제 점자 셀과 동일 배치
 * - 키보드: 퍼킨스 자판 매핑 (F=1 D=2 S=3 / J=4 K=5 L=6, Space=다음 칸, Enter=제출)
 * - 접근성: 점 토글/칸 이동을 TTS로 안내, 모바일 진동
 * - 아두이노: Web Serial로 현재 칸 점형을 촉각 디스플레이에 출력 (SET n)
 */

const PERKINS_KEYS: Record<string, number> = { f: 1, d: 2, s: 3, j: 4, k: 5, l: 6 }
const DOT_LAYOUT = [
  [1, 4],
  [2, 5],
  [3, 6],
]

interface BrailleKeypadProps {
  cellCount: number
  onSubmit: (cells: number[]) => void
  submitting?: boolean
  tts?: boolean
  dark?: boolean
  /** 값이 바뀌면 입력이 초기화된다 (문제 전환용) */
  resetKey?: string | number
}

export default function BrailleKeypad({
  cellCount,
  onSubmit,
  submitting = false,
  tts = true,
  dark = false,
  resetKey,
}: BrailleKeypadProps) {
  const [cells, setCells] = useState<number[]>(() => Array(cellCount).fill(0))
  const [cursor, setCursor] = useState(0)
  const [serialOn, setSerialOn] = useState(serialConnected())
  const padRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setCells(Array(Math.max(cellCount, 1)).fill(0))
    setCursor(0)
  }, [cellCount, resetKey])

  const announce = useCallback(
    (text: string) => {
      if (tts) speak(text)
    },
    [tts],
  )

  // 부수효과(TTS/진동/시리얼)는 상태 업데이터 밖에서 — StrictMode 이중 호출로 중복 실행되지 않도록
  const toggleDot = useCallback(
    (dot: number) => {
      const bit = 1 << (dot - 1)
      const nextValue = (cells[cursor] ?? 0) ^ bit
      setCells((prev) => {
        const next = [...prev]
        next[cursor] = nextValue
        return next
      })
      const on = (nextValue & bit) !== 0
      announce(on ? `${dot}점` : `${dot}점 해제`)
      if ('vibrate' in navigator) navigator.vibrate?.(on ? 30 : 15)
      void serialSetCell(nextValue)
    },
    [cells, cursor, announce],
  )

  const moveCursor = useCallback(
    (to: number) => {
      const clamped = Math.max(0, Math.min(cells.length - 1, to))
      setCursor(clamped)
      announce(`${clamped + 1}번째 칸`)
      void serialSetCell(cells[clamped] ?? 0)
    },
    [cells, announce],
  )

  const clearCell = useCallback(() => {
    setCells((prev) => {
      const next = [...prev]
      next[cursor] = 0
      return next
    })
    announce('지움')
    void serialSetCell(0)
  }, [cursor, announce])

  const submit = useCallback(() => {
    if (!submitting) onSubmit(cells)
  }, [cells, onSubmit, submitting])

  // 퍼킨스 자판 — 회의실 전역 단축키(M/C/S/H)와 충돌하지 않도록 전파를 막는다
  const onKeyDown = (e: React.KeyboardEvent) => {
    e.stopPropagation()
    const key = e.key.toLowerCase()
    if (key in PERKINS_KEYS) {
      e.preventDefault()
      toggleDot(PERKINS_KEYS[key])
    } else if (key === ' ' || key === 'space' || key === 'arrowright') {
      e.preventDefault()
      moveCursor(cursor + 1)
    } else if (key === 'arrowleft') {
      e.preventDefault()
      moveCursor(cursor - 1)
    } else if (key === 'backspace') {
      e.preventDefault()
      if (cells[cursor] !== 0) clearCell()
      else moveCursor(cursor - 1)
    } else if (key === 'enter') {
      e.preventDefault()
      submit()
    }
  }

  const toggleSerial = async () => {
    if (serialOn) {
      await disconnectSerial()
      setSerialOn(false)
      announce('촉각 디스플레이 연결 해제')
    } else {
      const ok = await connectSerial()
      setSerialOn(ok)
      announce(ok ? '촉각 디스플레이 연결됨' : '연결에 실패했습니다')
    }
  }

  const ink = dark ? 'text-white' : 'text-ink'
  const sub = dark ? 'text-white/50' : 'text-muted'
  const chipBg = dark ? 'bg-white/10' : 'bg-surface'

  return (
    <div
      ref={padRef}
      tabIndex={0}
      onKeyDown={onKeyDown}
      role="application"
      aria-label="점자 입력 키패드. F D S 키는 1 2 3점, J K L 키는 4 5 6점, 스페이스는 다음 칸, 엔터는 제출."
      className={`flex flex-col items-center gap-4 rounded-[16px] p-4 outline-none ring-offset-2 focus:ring-2 focus:ring-[#8ea5f8] ${
        dark ? 'bg-white/5' : 'bg-surface'
      }`}
    >
      {/* 셀 스트립 — 입력된 유니코드 점자 미리보기 */}
      <div className="flex max-w-full flex-wrap items-center justify-center gap-1.5" aria-hidden>
        {cells.map((value, i) => (
          <button
            key={i}
            onClick={() => moveCursor(i)}
            className={`flex h-11 w-9 items-center justify-center rounded-[8px] text-[22px] leading-none transition-all ${
              i === cursor
                ? 'bg-[#5276df] text-white shadow-md'
                : `${chipBg} ${value === 0 ? sub : ink}`
            }`}
          >
            {cellToUnicode(value)}
          </button>
        ))}
      </div>
      <p className={`text-[12px] font-semibold ${sub}`} aria-live="polite">
        {cursor + 1} / {cells.length} 칸
      </p>

      {/* 6점 패드 (햅틱 시뮬레이터 겸용) */}
      <div className="grid grid-cols-2 gap-3" aria-hidden>
        {DOT_LAYOUT.flat().map((dot) => {
          const on = ((cells[cursor] ?? 0) & (1 << (dot - 1))) !== 0
          return (
            <button
              key={dot}
              onClick={() => toggleDot(dot)}
              className={`flex h-16 w-16 items-center justify-center rounded-full text-[15px] font-black transition-all active:scale-90 ${
                on
                  ? 'scale-105 bg-[#5276df] text-white shadow-[0_0_16px_rgba(82,118,223,0.55)]'
                  : dark
                    ? 'bg-white/10 text-white/40 hover:bg-white/20'
                    : 'border border-line-strong bg-card text-faint hover:border-primary'
              }`}
            >
              {dot}
            </button>
          )
        })}
      </div>

      {/* 조작 버튼 */}
      <div className="flex flex-wrap items-center justify-center gap-2">
        <PadButton dark={dark} onClick={() => moveCursor(cursor - 1)} disabled={cursor === 0}>
          ← 이전 칸
        </PadButton>
        <PadButton dark={dark} onClick={clearCell}>
          지우기
        </PadButton>
        <PadButton dark={dark} onClick={() => moveCursor(cursor + 1)} disabled={cursor >= cells.length - 1}>
          다음 칸 →
        </PadButton>
        <button
          onClick={submit}
          disabled={submitting}
          className="rounded-full bg-[#2eb872] px-6 py-2 text-[13px] font-bold text-white transition-colors hover:bg-[#27a465] disabled:opacity-50"
        >
          {submitting ? '채점 중…' : '제출 (Enter)'}
        </button>
      </div>

      <div className={`flex items-center gap-3 text-[11px] font-semibold ${sub}`}>
        <span>⌨️ F·D·S = 1·2·3점 / J·K·L = 4·5·6점</span>
        {serialSupported() && (
          <button
            onClick={() => void toggleSerial()}
            className={`rounded-full px-2.5 py-1 transition-colors ${
              serialOn ? 'bg-[#2eb872]/20 text-[#2eb872]' : `${chipBg} hover:opacity-80`
            }`}
          >
            {serialOn ? '🔌 촉각 디스플레이 연결됨' : '🔌 아두이노 연결'}
          </button>
        )}
      </div>
    </div>
  )
}

function PadButton({
  dark,
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { dark?: boolean }) {
  return (
    <button
      className={`rounded-full px-4 py-2 text-[13px] font-bold transition-colors disabled:opacity-40 ${
        dark ? 'bg-white/10 text-white hover:bg-white/20' : 'bg-surface text-ink hover:bg-line'
      }`}
      {...props}
    >
      {children}
    </button>
  )
}
