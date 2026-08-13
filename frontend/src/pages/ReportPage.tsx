import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { roomsApi } from '../lib/rooms'
import { Card, PageShell, SectionTitle } from '../components/ui'
import ReplayPlayer from '../components/ReplayPlayer'
import ReportDocument from '../components/ReportDocument'
import ActionBoard from '../components/ActionBoard'

type ReportState =
  | { phase: 'polling'; attempts: number }
  | { phase: 'done'; title: string; content: string; createdTime: string }
  | { phase: 'error'; message: string }

const MAX_ATTEMPTS = 60

/** 회의록 공개 공유 링크 생성/복사 버튼 */
function ShareButton({ roomId }: { roomId: number }) {
  const [copied, setCopied] = useState(false)
  const [busy, setBusy] = useState(false)
  const [shareUrl, setShareUrl] = useState<string | null>(null)
  const [error, setError] = useState(false)

  const share = async () => {
    if (busy) return
    setBusy(true)
    setError(false)
    try {
      const { shareToken } = await roomsApi.enableReportShare(roomId)
      const url = `${window.location.origin}/share/${shareToken}`
      setShareUrl(url) // 클립보드 실패해도 링크는 항상 보여준다
      try {
        await navigator.clipboard.writeText(url)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
      } catch {
        /* 클립보드 불가 — URL 표시로 수동 복사 */
      }
    } catch {
      setError(true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <span className="flex items-center gap-2">
      {shareUrl && (
        <input
          readOnly
          value={shareUrl}
          onFocus={(e) => e.currentTarget.select()}
          className="w-[220px] rounded-full border border-line bg-surface px-3 py-1.5 font-mono text-[11px] text-muted outline-none"
        />
      )}
      {error && <span className="text-[12px] font-semibold text-danger">공유 링크 생성 실패</span>}
      <button
        onClick={() => void share()}
        disabled={busy}
        className="rounded-full border border-line-strong px-4 py-2 text-[13px] font-bold text-muted hover:border-primary hover:text-primary disabled:opacity-50"
      >
        {copied ? '링크 복사됨 ✓' : '🔗 공유 링크'}
      </button>
    </span>
  )
}

export default function ReportPage() {
  const { roomId: roomIdParam } = useParams<{ roomId: string }>()
  const roomId = Number(roomIdParam)
  const [state, setState] = useState<ReportState>({ phase: 'polling', attempts: 0 })

  useEffect(() => {
    let cancelled = false
    let attempts = 0

    async function poll() {
      while (!cancelled && attempts < MAX_ATTEMPTS) {
        try {
          const { status } = await roomsApi.reportStatus(roomId)
          if (status === 'DONE') {
            const report = await roomsApi.report(roomId)
            if (!cancelled) {
              setState({
                phase: 'done',
                title: report.title,
                content: report.content,
                createdTime: report.createdTime,
              })
            }
            return
          }
        } catch (err) {
          if (!cancelled) {
            setState({ phase: 'error', message: err instanceof Error ? err.message : '리포트 조회 실패' })
          }
          return
        }
        attempts += 1
        if (!cancelled) setState({ phase: 'polling', attempts })
        await new Promise((resolve) => setTimeout(resolve, 3000))
      }
      if (!cancelled && attempts >= MAX_ATTEMPTS) {
        setState({
          phase: 'error',
          message: '리포트 생성이 오래 걸리고 있어요. 잠시 후 다시 방문해주세요.',
        })
      }
    }

    void poll()
    return () => {
      cancelled = true
    }
  }, [roomId])

  return (
    <PageShell>
      <SectionTitle sub="회의가 끝나면 발화 내용을 바탕으로 AI 회의록이 생성됩니다.">회의록</SectionTitle>

      {state.phase === 'polling' && (
        <Card className="flex flex-col items-center gap-4 p-14 text-center">
          <span className="h-10 w-10 animate-spin rounded-full border-[3px] border-primary-pale border-t-primary" />
          <div>
            <p className="font-bold text-ink">AI가 회의록을 작성하고 있어요</p>
            <p className="mt-1 text-[13px] text-muted">
              발화 분석이 끝나는 대로 여기 표시됩니다 · {state.attempts * 3}초 경과
            </p>
          </div>
        </Card>
      )}

      {state.phase === 'error' && (
        <Card className="p-10 text-center">
          <p className="font-bold text-danger">{state.message}</p>
          <Link
            to="/dashboard"
            className="mt-5 inline-block rounded-full bg-primary px-6 py-2.5 text-sm font-bold text-white hover:bg-primary-hover"
          >
            대시보드로
          </Link>
        </Card>
      )}

      {state.phase === 'done' && <ReplayPlayer roomId={roomId} />}

      {state.phase === 'done' && (
        <>
          <Card className="report-print-area p-8">
            <div className="mb-6 flex items-center justify-end gap-2 print:hidden">
              <ShareButton roomId={roomId} />
              <button
                onClick={() => window.print()}
                className="rounded-full border border-line-strong px-4 py-2 text-[13px] font-bold text-muted hover:border-primary hover:text-primary"
              >
                🖨 인쇄 / PDF
              </button>
              <button
                onClick={() => {
                  const blob = new Blob([state.content], { type: 'text/markdown;charset=utf-8' })
                  const url = URL.createObjectURL(blob)
                  const anchor = document.createElement('a')
                  anchor.href = url
                  anchor.download = `${state.title || 'meeting-report'}.md`
                  anchor.click()
                  URL.revokeObjectURL(url)
                }}
                className="rounded-full border border-line-strong px-4 py-2 text-[13px] font-bold text-muted hover:border-primary hover:text-primary"
              >
                .md 다운로드
              </button>
            </div>
            <ReportDocument title={state.title} content={state.content} createdTime={state.createdTime} />
          </Card>

          <ActionBoard roomId={roomId} />
        </>
      )}
    </PageShell>
  )
}
