import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { brailleApi, speak } from '../lib/braille'
import type { BrailleProblem, BrailleSkill } from '../lib/braille'
import BrailleSolver from '../components/BrailleSolver'
import { Button, Card, PageShell, SectionTitle } from '../components/ui'

/**
 * 점자 복습 페이지.
 * - 자모별 숙련도(EMA) 대시보드: 우선순위 = (1−숙련도) × 오답률 × 최근성
 * - AI 복습: 취약 자모를 겨냥한 단어를 LLM이 생성 → 규칙 엔진 검증 → 솔로 풀이
 */
export default function BrailleReviewPage() {
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<BrailleProblem | null>(null)

  const { data: skills } = useQuery({ queryKey: ['braille-skills'], queryFn: brailleApi.mySkills })
  const { data: review, isLoading: reviewLoading } = useQuery({
    queryKey: ['braille-review'],
    queryFn: brailleApi.myReview,
  })

  const generateMutation = useMutation({
    mutationFn: brailleApi.generateReview,
    onSuccess: (problems) => {
      void queryClient.invalidateQueries({ queryKey: ['braille-review'] })
      speak(`복습 문제 ${problems.length}개가 준비되었습니다.`)
    },
  })

  const markSolved = (problemId: number) => {
    queryClient.setQueryData<BrailleProblem[]>(['braille-review'], (prev) =>
      prev?.map((p) => (p.problemId === problemId ? { ...p, solved: true } : p)),
    )
    void queryClient.invalidateQueries({ queryKey: ['braille-skills'] })
    setSelected((prev) => (prev && prev.problemId === problemId ? { ...prev, solved: true } : prev))
  }

  const weakest = (skills ?? []).slice(0, 3)

  return (
    <PageShell>
      <SectionTitle sub="풀이할 때마다 자모별 숙련도가 갱신되고, AI가 취약한 자모를 겨냥한 복습 문제를 만들어 줍니다.">
        점자 복습
      </SectionTitle>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.2fr]">
        {/* 숙련도 대시보드 */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-[15px] font-extrabold text-ink">자모별 숙련도</h3>
            {weakest.length > 0 && (
              <span className="rounded-full bg-danger/10 px-3 py-1 text-[11px] font-bold text-danger">
                취약: {weakest.map((s) => s.jamo).join(' ')}
              </span>
            )}
          </div>

          {!skills || skills.length === 0 ? (
            <p className="rounded-[12px] bg-surface p-5 text-center text-[13px] text-muted">
              아직 풀이 기록이 없습니다.
              <br />
              수업에서 문제를 풀거나 AI 복습을 생성해 보세요.
            </p>
          ) : (
            <div className="flex flex-col gap-2.5">
              {skills.map((skill) => (
                <SkillBar key={skill.jamo} skill={skill} />
              ))}
            </div>
          )}
        </Card>

        {/* AI 복습 문제 */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h3 className="text-[15px] font-extrabold text-ink">AI 복습 문제</h3>
            <Button
              onClick={() => generateMutation.mutate()}
              disabled={generateMutation.isPending}
              className="!px-4 !py-2 text-[13px]"
            >
              {generateMutation.isPending ? 'AI가 문제를 만드는 중…' : '✨ AI 복습 생성'}
            </Button>
          </div>
          {generateMutation.isError && (
            <p className="mb-3 text-[13px] font-bold text-danger">
              복습 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.
            </p>
          )}

          {selected ? (
            <div className="flex flex-col gap-3">
              <button
                onClick={() => setSelected(null)}
                className="self-start rounded-full bg-surface px-3 py-1.5 text-[12px] font-bold text-ink hover:bg-line"
              >
                ← 문제 목록
              </button>
              <BrailleSolver problem={selected} onSolved={markSolved} />
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {reviewLoading && <p className="text-[13px] text-muted">불러오는 중…</p>}
              {!reviewLoading && (!review || review.length === 0) && (
                <p className="rounded-[12px] bg-surface p-5 text-center text-[13px] text-muted">
                  아직 복습 문제가 없습니다. 「AI 복습 생성」을 눌러 시작하세요.
                </p>
              )}
              {review?.map((problem) => (
                <button
                  key={problem.problemId}
                  onClick={() => setSelected(problem)}
                  className="flex items-center justify-between rounded-[14px] border border-line bg-card px-4 py-3 text-left transition-colors hover:border-primary"
                >
                  <div>
                    <p className="text-[14px] font-extrabold text-ink">{problem.text}</p>
                    <p className="text-[11px] font-semibold text-muted">
                      {problem.cellCount}칸
                      {problem.reviewJamos ? ` · 복습 자모 ${problem.reviewJamos}` : ''}
                    </p>
                  </div>
                  {problem.solved ? (
                    <span className="rounded-full bg-[#2eb872]/15 px-2.5 py-1 text-[11px] font-bold text-[#2eb872]">
                      ✓ 해결
                    </span>
                  ) : (
                    <span className="rounded-full bg-primary-soft px-2.5 py-1 text-[11px] font-bold text-primary-deep">
                      풀기 →
                    </span>
                  )}
                </button>
              ))}
            </div>
          )}
        </Card>
      </div>
    </PageShell>
  )
}

function SkillBar({ skill }: { skill: BrailleSkill }) {
  const percent = Math.round(skill.proficiency * 100)
  const level = percent >= 75 ? 'good' : percent >= 45 ? 'mid' : 'weak'
  const barColor = level === 'good' ? 'bg-[#2eb872]' : level === 'mid' ? 'bg-[#ffb224]' : 'bg-danger'

  return (
    <div className="flex items-center gap-3">
      <span className="w-10 shrink-0 text-center text-[15px] font-extrabold text-ink">{skill.jamo}</span>
      <div className="h-2.5 min-w-0 flex-1 overflow-hidden rounded-full bg-surface">
        <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${percent}%` }} />
      </div>
      <span className="w-11 shrink-0 text-right text-[12px] font-bold text-muted">{percent}%</span>
      <span className="w-20 shrink-0 text-right text-[11px] font-semibold text-faint">
        {skill.attemptCount}회 · 오답 {skill.wrongCount}
      </span>
    </div>
  )
}
