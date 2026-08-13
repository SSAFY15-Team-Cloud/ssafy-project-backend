import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { knowledgeApi, searchApi } from '../lib/rooms'
import type { SearchResult } from '../lib/rooms'
import { Card, PageShell, SectionTitle } from '../components/ui'

/**
 * 통합 시맨틱 검색: 지식 위키 + 내가 참여한 회의의 발화 기록을 의미 기반으로 검색.
 */
export default function SearchPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') ?? '')
  const [result, setResult] = useState<SearchResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 공유된 검색 링크(?q=...)로 진입하면 자동 검색
  const autoSearched = useRef(false)
  useEffect(() => {
    const initial = searchParams.get('q')
    if (initial && initial.trim().length >= 2 && !autoSearched.current) {
      autoSearched.current = true
      void runSearch(initial)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const runSearch = async (q: string) => {
    const trimmed = q.trim()
    if (trimmed.length < 2 || loading) return
    setLoading(true)
    setError(null)
    setSearchParams({ q: trimmed }, { replace: true })
    try {
      setResult(await searchApi.search(trimmed))
    } catch (err) {
      setError(err instanceof Error ? err.message : '검색에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const downloadDoc = async (documentId: number) => {
    const { downloadUrl } = await knowledgeApi.downloadUrl(documentId)
    window.open(downloadUrl, '_blank')
  }

  const empty = result && result.wiki.length === 0 && result.meetings.length === 0

  return (
    <PageShell>
      <SectionTitle sub="키워드가 아니라 의미로 찾습니다. 회의에서 나눈 대화와 지식 위키를 한 번에 검색해요.">
        통합 검색
      </SectionTitle>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          void runSearch(query)
        }}
        className="mb-8 flex gap-2"
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder='예: "DB 포트를 왜 바꿨더라?", "WebRTC 미디어 서버 결정"'
          autoFocus
          className="min-w-0 flex-1 rounded-full border border-line bg-white px-5 py-3.5 text-[15px] text-ink outline-none transition-colors placeholder:text-faint focus:border-primary"
        />
        <button
          type="submit"
          disabled={loading || query.trim().length < 2}
          className="rounded-full bg-primary px-7 text-[14px] font-bold text-white transition-colors hover:bg-primary-hover disabled:opacity-50"
        >
          {loading ? '검색 중…' : '검색'}
        </button>
      </form>

      {error && (
        <p className="mb-6 rounded-[12px] border border-danger/30 bg-danger/5 px-4 py-3 text-sm font-semibold text-danger">
          {error}
        </p>
      )}

      {empty && (
        <p className="py-16 text-center text-[14px] text-muted">
          관련된 문서나 발언을 찾지 못했어요. 다른 표현으로 검색해보세요.
        </p>
      )}

      {result && !empty && (
        <div className="grid gap-6 lg:grid-cols-2">
          {/* 회의 발언 */}
          <section>
            <h3 className="mb-3 text-[14px] font-extrabold text-ink">
              💬 회의 발언 <span className="text-muted">({result.meetings.length})</span>
            </h3>
            <div className="space-y-3">
              {result.meetings.length === 0 && (
                <p className="text-[13px] text-muted">일치하는 발언이 없습니다.</p>
              )}
              {result.meetings.map((hit, i) => (
                <Card key={i} className="p-5">
                  <div className="flex items-center justify-between gap-2">
                    <p className="min-w-0 truncate text-[13px] font-extrabold text-ink">{hit.roomTitle}</p>
                    <ScoreBadge score={hit.score} />
                  </div>
                  <p className="mt-2 text-[13.5px] leading-relaxed text-ink/85">
                    <span className="font-bold text-primary">{hit.speakerName}:</span> {hit.snippet}
                  </p>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="font-mono text-[11.5px] text-faint">
                      {hit.spokeTime ? new Date(hit.spokeTime).toLocaleString('ko-KR') : ''}
                    </span>
                    <button
                      onClick={() => navigate(`/rooms/${hit.roomId}/report`)}
                      className="rounded-full border border-line-strong px-3.5 py-1 text-[12px] font-bold text-muted hover:border-primary hover:text-primary"
                    >
                      회의록 보기
                    </button>
                  </div>
                </Card>
              ))}
            </div>
          </section>

          {/* 위키 문서 */}
          <section>
            <h3 className="mb-3 text-[14px] font-extrabold text-ink">
              📄 위키 문서 <span className="text-muted">({result.wiki.length})</span>
            </h3>
            <div className="space-y-3">
              {result.wiki.length === 0 && (
                <p className="text-[13px] text-muted">일치하는 문서가 없습니다.</p>
              )}
              {result.wiki.map((hit) => (
                <Card key={hit.documentId} className="p-5">
                  <div className="flex items-center justify-between gap-2">
                    <p className="min-w-0 truncate text-[13px] font-extrabold text-ink">{hit.title}</p>
                    <ScoreBadge score={hit.score} />
                  </div>
                  <p className="mt-2 line-clamp-3 text-[13.5px] leading-relaxed text-ink/85">{hit.snippet}</p>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="truncate font-mono text-[11.5px] text-faint">{hit.filename}</span>
                    <button
                      onClick={() => void downloadDoc(hit.documentId)}
                      className="rounded-full border border-line-strong px-3.5 py-1 text-[12px] font-bold text-muted hover:border-primary hover:text-primary"
                    >
                      다운로드
                    </button>
                  </div>
                </Card>
              ))}
            </div>
          </section>
        </div>
      )}

      {!result && !loading && (
        <p className="py-16 text-center text-[14px] text-faint">
          검색어를 입력하면 의미가 비슷한 발언과 문서를 찾아드립니다.
        </p>
      )}
    </PageShell>
  )
}

function ScoreBadge({ score }: { score: number }) {
  return (
    <span className="shrink-0 rounded-full bg-primary-soft px-2 py-0.5 font-mono text-[10.5px] font-bold text-primary-deep">
      {(score * 100).toFixed(0)}%
    </span>
  )
}
