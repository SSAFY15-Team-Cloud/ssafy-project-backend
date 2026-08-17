import { useEffect, useState } from 'react'
import { brailleApi, cellToUnicode, feedbackToSpeech, speak } from '../lib/braille'
import type { BrailleAttemptResult, BrailleProblem } from '../lib/braille'
import BrailleKeypad from './BrailleKeypad'

/**
 * 문제 풀이 흐름: TTS로 문제 낭독 → 키패드 입력 → 제출 → 점 단위 피드백(화면+음성).
 * 회의실 패널(dark)과 복습 페이지(light) 양쪽에서 사용한다.
 */
interface BrailleSolverProps {
  problem: BrailleProblem
  dark?: boolean
  onSolved?: (problemId: number) => void
}

export default function BrailleSolver({ problem, dark = false, onSolved }: BrailleSolverProps) {
  const [result, setResult] = useState<BrailleAttemptResult | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attemptNo, setAttemptNo] = useState(0)

  // 문제가 바뀌면 초기화 + 낭독
  useEffect(() => {
    setResult(null)
    setAttemptNo(0)
    setError(null)
    speak(`문제. ${problem.text}. ${problem.cellCount}칸입니다.`)
    return () => window.speechSynthesis?.cancel()
  }, [problem.problemId, problem.text, problem.cellCount])

  const submit = async (cells: number[]) => {
    setSubmitting(true)
    setError(null)
    try {
      const res = await brailleApi.submitAttempt(problem.problemId, cells)
      setResult(res)
      speak(feedbackToSpeech(res))
      if (res.correct) onSolved?.(problem.problemId)
    } catch (err) {
      setError(err instanceof Error ? err.message : '채점에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const retry = () => {
    setResult(null)
    setAttemptNo((n) => n + 1)
    speak(`다시 풀기. ${problem.text}.`)
  }

  const ink = dark ? 'text-white' : 'text-ink'
  const sub = dark ? 'text-white/50' : 'text-muted'

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2">
        <div>
          <p className={`text-[15px] font-extrabold ${ink}`}>{problem.text}</p>
          <p className={`text-[12px] font-semibold ${sub}`}>
            {problem.cellCount}칸{problem.reviewJamos ? ` · 복습 자모: ${problem.reviewJamos}` : ''}
          </p>
        </div>
        <button
          onClick={() => speak(`문제. ${problem.text}. ${problem.cellCount}칸입니다.`)}
          className={`shrink-0 rounded-full px-3 py-1.5 text-[12px] font-bold transition-colors ${
            dark ? 'bg-white/10 text-white hover:bg-white/20' : 'bg-surface text-ink hover:bg-line'
          }`}
        >
          🔊 문제 듣기
        </button>
      </div>

      {!result && (
        <BrailleKeypad
          cellCount={problem.cellCount}
          onSubmit={(cells) => void submit(cells)}
          submitting={submitting}
          dark={dark}
          resetKey={`${problem.problemId}-${attemptNo}`}
        />
      )}

      {error && <p className="text-[13px] font-bold text-[#e5484d]">{error}</p>}

      {result && (
        <div
          className={`flex flex-col gap-3 rounded-[16px] p-4 ${
            result.correct
              ? 'bg-[#2eb872]/12 ring-1 ring-[#2eb872]/40'
              : 'bg-[#e5484d]/10 ring-1 ring-[#e5484d]/35'
          }`}
        >
          <div className="flex items-center justify-between">
            <p className={`text-[15px] font-extrabold ${result.correct ? 'text-[#2eb872]' : 'text-[#e5484d]'}`}>
              {result.correct ? '🎉 정답입니다!' : '오답입니다'}
            </p>
            <span className={`text-[13px] font-bold ${sub}`}>
              정확도 {Math.round(result.accuracy * 100)}%
            </span>
          </div>

          {/* 점 단위 피드백 — 칸별 정답/입력 비교 */}
          <div className="flex flex-wrap gap-1.5">
            {result.cells.map((cell) => (
              <div
                key={cell.index}
                className={`flex flex-col items-center rounded-[10px] px-2 py-1.5 ${
                  cell.correct
                    ? dark
                      ? 'bg-white/10'
                      : 'bg-card'
                    : 'bg-[#e5484d]/15 ring-1 ring-[#e5484d]/40'
                }`}
                title={
                  cell.correct
                    ? '정답'
                    : [
                        cell.missingDots.length ? `${cell.missingDots.join(',')}점 누락` : '',
                        cell.extraDots.length ? `${cell.extraDots.join(',')}점 초과` : '',
                      ]
                        .filter(Boolean)
                        .join(' · ')
                }
              >
                <span className={`text-[19px] leading-none ${ink}`}>{cellToUnicode(cell.actual)}</span>
                {!cell.correct && (
                  <>
                    <span className="mt-0.5 text-[15px] leading-none text-[#2eb872]">
                      {cellToUnicode(cell.expected)}
                    </span>
                    <span className={`mt-0.5 text-[10px] font-bold ${sub}`}>
                      {cell.sourceJamo ?? ''}
                    </span>
                  </>
                )}
              </div>
            ))}
          </div>
          {!result.correct && (
            <p className={`text-[11px] font-semibold ${sub}`}>
              위: 내 입력 · 아래(초록): 정답 점형 · 누락/초과 점은 칸에 마우스를 올리면 표시됩니다
            </p>
          )}

          <div className="flex gap-2">
            {!result.correct && (
              <button
                onClick={retry}
                className="rounded-full bg-[#5276df] px-5 py-2 text-[13px] font-bold text-white hover:bg-[#4265c9]"
              >
                다시 풀기
              </button>
            )}
            <button
              onClick={() => speak(feedbackToSpeech(result))}
              className={`rounded-full px-4 py-2 text-[13px] font-bold transition-colors ${
                dark ? 'bg-white/10 text-white hover:bg-white/20' : 'bg-surface text-ink hover:bg-line'
              }`}
            >
              🔊 피드백 다시 듣기
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
