import { useEffect, useState } from 'react'
import { brailleApi } from '../lib/braille'
import type { BrailleClassContext, BrailleProblem, BrailleProblemBoard } from '../lib/braille'
import type { BrailleBoardEvent } from '../lib/stomp'
import BrailleSolver from './BrailleSolver'

/**
 * 회의실 '점자' 탭.
 * - 강사(호스트): 문제 출제 + 학습자별 실시간 채점 보드
 * - 학습자: 문제 목록(TTS 낭독) → 6점 키패드 풀이 → 점 단위 피드백
 */
interface BraillePanelProps {
  roomId: number
  problems: BrailleProblem[]
  onLocalUpdate: (problem: BrailleProblem) => void
  lastBoardEvent: (BrailleBoardEvent & { seq: number }) | null
}

export default function BraillePanel({ roomId, problems, onLocalUpdate, lastBoardEvent }: BraillePanelProps) {
  const [ctx, setCtx] = useState<BrailleClassContext | null>(null)
  const [text, setText] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [board, setBoard] = useState<BrailleProblemBoard[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)

  useEffect(() => {
    brailleApi
      .context(roomId)
      .then(setCtx)
      .catch(() => setCtx({ hostId: -1, host: false })) // 실패 시 학습자 뷰 폴백
  }, [roomId])

  const isHost = ctx?.host ?? false

  // 강사: 초기 보드 로드 — 스냅샷이 실시간 이벤트보다 늦게 도착해도 병합분을 잃지 않도록 max/OR 병합
  useEffect(() => {
    if (!isHost) return
    brailleApi
      .board(roomId)
      .then((fetched) =>
        setBoard((prev) => {
          const prevById = new Map(prev.map((pb) => [pb.problemId, pb]))
          const merged = fetched.map((pb) => {
            const local = prevById.get(pb.problemId)
            if (!local) return pb
            const localEntries = new Map(local.entries.map((e) => [e.userId, e]))
            return {
              ...pb,
              entries: pb.entries.map((e) => {
                const le = localEntries.get(e.userId)
                return le
                  ? {
                      ...e,
                      attempts: Math.max(e.attempts, le.attempts),
                      solved: e.solved || le.solved,
                      bestAccuracy: Math.max(e.bestAccuracy, le.bestAccuracy),
                    }
                  : e
              }),
            }
          })
          const fetchedIds = new Set(fetched.map((pb) => pb.problemId))
          return [...prev.filter((pb) => !fetchedIds.has(pb.problemId)), ...merged]
        }),
      )
      .catch(() => {})
  }, [isHost, roomId])

  // 강사: 실시간 채점 이벤트를 보드에 반영 — 이벤트가 절대값(누적치)이라 멱등 병합
  useEffect(() => {
    if (!lastBoardEvent || !isHost) return
    const eventEntry = {
      userId: lastBoardEvent.userId,
      name: lastBoardEvent.name,
      attempts: lastBoardEvent.attempts,
      solved: lastBoardEvent.solved,
      bestAccuracy: lastBoardEvent.bestAccuracy,
      firstCorrect: lastBoardEvent.firstCorrect,
      firstAccuracy: lastBoardEvent.attempts === 1 ? lastBoardEvent.accuracy : -1, // -1 = 유지
    }
    setBoard((prev) => {
      const next = prev.map((problemBoard) => {
        if (problemBoard.problemId !== lastBoardEvent.problemId) return problemBoard
        const existing = problemBoard.entries.find((entry) => entry.userId === lastBoardEvent.userId)
        const entries = existing
          ? problemBoard.entries.map((entry) =>
              entry.userId === lastBoardEvent.userId
                ? {
                    ...entry,
                    attempts: Math.max(entry.attempts, eventEntry.attempts),
                    solved: entry.solved || eventEntry.solved,
                    bestAccuracy: Math.max(entry.bestAccuracy, eventEntry.bestAccuracy),
                    firstCorrect: eventEntry.firstCorrect,
                    firstAccuracy:
                      eventEntry.firstAccuracy >= 0 ? eventEntry.firstAccuracy : entry.firstAccuracy,
                  }
                : entry,
            )
          : [
              ...problemBoard.entries,
              { ...eventEntry, firstAccuracy: Math.max(eventEntry.firstAccuracy, 0) },
            ]
        return { ...problemBoard, entries }
      })
      // 새 문제가 보드에 아직 없으면 추가
      if (!next.some((pb) => pb.problemId === lastBoardEvent.problemId)) {
        const problem = problems.find((p) => p.problemId === lastBoardEvent.problemId)
        next.unshift({
          problemId: lastBoardEvent.problemId,
          text: problem?.text ?? `문제 #${lastBoardEvent.problemId}`,
          entries: [{ ...eventEntry, firstAccuracy: Math.max(eventEntry.firstAccuracy, 0) }],
        })
      }
      return next
    })
  }, [lastBoardEvent?.seq]) // eslint-disable-line react-hooks/exhaustive-deps

  const createProblem = async () => {
    const trimmed = text.trim()
    if (!trimmed || creating) return
    setCreating(true)
    setCreateError(null)
    try {
      const problem = await brailleApi.createProblem(roomId, trimmed)
      onLocalUpdate(problem)
      setBoard((prev) => [{ problemId: problem.problemId, text: problem.text, entries: [] }, ...prev])
      setText('')
    } catch (err) {
      setCreateError(
        err instanceof Error && err.message.includes('40011')
          ? '한글·숫자·기본 문장부호만 출제할 수 있습니다.'
          : err instanceof Error
            ? err.message
            : '출제에 실패했습니다.',
      )
    } finally {
      setCreating(false)
    }
  }

  const selected = problems.find((p) => p.problemId === selectedId) ?? null

  // 역할 확인 전에는 잘못된 뷰가 깜빡이지 않도록 로딩만 표시
  if (ctx === null) {
    return <p className="p-6 text-center text-[12px] text-white/40">불러오는 중…</p>
  }

  // ===== 강사 뷰 =====
  if (isHost) {
    return (
      <div className="flex flex-col gap-4 p-4">
        <div>
          <p className="mb-2 text-[13px] font-extrabold text-white">점자 문제 출제</p>
          <div className="flex gap-2">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.nativeEvent.isComposing) void createProblem()
              }}
              maxLength={30}
              placeholder="예: 사과 (한글·숫자·.,?!)"
              className="min-w-0 flex-1 rounded-full bg-white/10 px-4 py-2.5 text-[13px] text-white outline-none placeholder:text-white/40 focus:bg-white/15"
            />
            <button
              onClick={() => void createProblem()}
              disabled={creating || !text.trim()}
              className="rounded-full bg-[#5276df] px-4 text-[13px] font-bold text-white hover:bg-[#4265c9] disabled:opacity-50"
            >
              {creating ? '변환 중…' : '출제'}
            </button>
          </div>
          {createError && <p className="mt-1.5 text-[12px] font-bold text-[#ff8589]">{createError}</p>}
        </div>

        <div className="flex flex-col gap-3">
          <p className="text-[13px] font-extrabold text-white">
            실시간 채점 보드 <span className="font-semibold text-white/40">— 제출 즉시 갱신</span>
          </p>
          {board.length === 0 && (
            <p className="rounded-[12px] bg-white/5 p-4 text-center text-[12px] text-white/40">
              아직 출제한 문제가 없습니다
            </p>
          )}
          {board.map((problemBoard) => {
            const problem = problems.find((p) => p.problemId === problemBoard.problemId)
            return (
              <div key={problemBoard.problemId} className="rounded-[14px] bg-white/5 p-3">
                <div className="mb-2 flex items-baseline justify-between gap-2">
                  <p className="text-[14px] font-extrabold text-white">{problemBoard.text}</p>
                  {problem?.answerUnicode && (
                    <span className="text-[18px] leading-none text-[#8ea5f8]" title="정답 점형">
                      {problem.answerUnicode}
                    </span>
                  )}
                </div>
                {problemBoard.entries.length === 0 ? (
                  <p className="text-[12px] text-white/35">아직 제출 없음</p>
                ) : (
                  <div className="flex flex-col gap-1.5">
                    {problemBoard.entries.map((entry) => (
                      <div
                        key={entry.userId}
                        className="flex items-center gap-2 text-[12px]"
                        title={`첫 시도 ${Math.round(entry.firstAccuracy * 100)}% · 최고 ${Math.round(entry.bestAccuracy * 100)}%`}
                      >
                        <span
                          className={
                            entry.firstCorrect
                              ? 'text-[#2eb872]'
                              : entry.solved
                                ? 'text-[#ffb224]'
                                : 'text-white/40'
                          }
                        >
                          {entry.firstCorrect ? '✓' : entry.solved ? '◔' : '…'}
                        </span>
                        <span className="w-20 truncate font-bold text-white/80">{entry.name}</span>
                        <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-white/10">
                          <div
                            className={`h-full rounded-full transition-all ${
                              entry.firstCorrect ? 'bg-[#2eb872]' : 'bg-[#ffb224]'
                            }`}
                            style={{ width: `${Math.round(entry.firstAccuracy * 100)}%` }}
                          />
                        </div>
                        <span className="w-20 shrink-0 text-right font-semibold text-white/50">
                          첫 {Math.round(entry.firstAccuracy * 100)}% · {entry.attempts}회
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  // ===== 학습자 뷰 =====
  return (
    <div className="flex flex-col gap-3 p-4">
      {selected ? (
        <>
          <button
            onClick={() => setSelectedId(null)}
            className="self-start rounded-full bg-white/10 px-3 py-1.5 text-[12px] font-bold text-white hover:bg-white/20"
          >
            ← 문제 목록
          </button>
          <BrailleSolver
            problem={selected}
            dark
            onSolved={(problemId) => {
              const problem = problems.find((p) => p.problemId === problemId)
              if (problem) onLocalUpdate({ ...problem, solved: true })
            }}
          />
        </>
      ) : (
        <>
          <p className="text-[13px] font-extrabold text-white">
            점자 문제 <span className="font-semibold text-white/40">— 선택하면 음성으로 읽어드립니다</span>
          </p>
          {problems.length === 0 && (
            <p className="rounded-[12px] bg-white/5 p-4 text-center text-[12px] text-white/40">
              강사가 문제를 출제하면 여기에 나타납니다
            </p>
          )}
          {problems.map((problem) => (
            <button
              key={problem.problemId}
              onClick={() => setSelectedId(problem.problemId)}
              className="flex items-center justify-between rounded-[14px] bg-white/5 px-4 py-3 text-left transition-colors hover:bg-white/10"
            >
              <div>
                <p className="text-[14px] font-extrabold text-white">{problem.text}</p>
                <p className="text-[11px] font-semibold text-white/40">{problem.cellCount}칸</p>
              </div>
              {problem.solved ? (
                <span className="rounded-full bg-[#2eb872]/20 px-2.5 py-1 text-[11px] font-bold text-[#2eb872]">
                  ✓ 해결
                </span>
              ) : (
                <span className="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-bold text-white/50">
                  풀기 →
                </span>
              )}
            </button>
          ))}
        </>
      )}
    </div>
  )
}
