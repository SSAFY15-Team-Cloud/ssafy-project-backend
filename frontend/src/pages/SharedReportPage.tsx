import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { roomsApi } from '../lib/rooms'
import { Card, Logo } from '../components/ui'
import ReportDocument from '../components/ReportDocument'

/** 공개 공유 회의록 — 로그인 없이 토큰만으로 열람 */
export default function SharedReportPage() {
  const { token } = useParams<{ token: string }>()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['sharedReport', token],
    queryFn: () => roomsApi.sharedReport(token!),
    enabled: !!token,
    retry: false,
  })

  return (
    <div className="min-h-full bg-canvas">
      <header className="border-b border-line bg-card">
        <div className="mx-auto flex h-14 max-w-[860px] items-center justify-between px-6">
          <Logo />
          <span className="rounded-full bg-primary-soft px-3 py-1 text-[11.5px] font-bold text-primary-deep">
            공유된 회의록
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-[860px] px-6 py-8">
        {isLoading && <p className="py-20 text-center text-muted">불러오는 중…</p>}
        {isError && (
          <Card className="p-12 text-center">
            <p className="font-bold text-danger">유효하지 않거나 만료된 공유 링크입니다.</p>
          </Card>
        )}
        {data && (
          <Card className="p-8">
            <ReportDocument title={data.title} content={data.content} createdTime={data.createdTime} />
          </Card>
        )}
      </main>
    </div>
  )
}
