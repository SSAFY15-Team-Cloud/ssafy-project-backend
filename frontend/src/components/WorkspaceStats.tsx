import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { roomsApi } from '../lib/rooms'
import type { ActionItem } from '../lib/rooms'
import { Card } from './ui'

/**
 * 워크스페이스 홈 상단: 활동 통계 + 주간 차트 + 내 할 일 위젯.
 */
export default function WorkspaceStats() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data } = useQuery({ queryKey: ['overview'], queryFn: roomsApi.overview })

  const completeMutation = useMutation({
    mutationFn: (itemId: number) => roomsApi.updateActionItemStatus(itemId, 'DONE' as ActionItem['status']),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['overview'] }),
  })

  if (!data) return null

  const completionRate =
    data.totalActionItems > 0 ? Math.round((data.doneActionItems / data.totalActionItems) * 100) : null
  const thisWeekMeetings = data.weeklyActivity.reduce((acc, day) => acc + day.meetings, 0)
  const maxDay = Math.max(1, ...data.weeklyActivity.map((day) => day.meetings))
  const speakingMinutes = Math.round(data.totalSpeakingSeconds / 60)

  return (
    <div className="mb-6 grid gap-4 lg:grid-cols-[1fr_320px]">
      {/* 통계 + 주간 차트 */}
      <Card className="p-6">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Stat label="참여한 회의" value={String(data.totalMeetings)} suffix="회" />
          <Stat label="총 발언 시간" value={String(speakingMinutes)} suffix="분" />
          <Stat label="이번 주 회의" value={String(thisWeekMeetings)} suffix="회" />
          <Stat
            label="할 일 완료율"
            value={completionRate !== null ? String(completionRate) : '—'}
            suffix={completionRate !== null ? '%' : ''}
          />
        </div>

        {/* 주간 활동 미니 차트 */}
        <div className="mt-6 border-t border-line pt-4">
          <p className="mb-3 text-[11px] font-bold uppercase tracking-wide text-faint">최근 7일 활동</p>
          <div className="flex items-end gap-2" style={{ height: 64 }}>
            {data.weeklyActivity.map((day, index) => {
              // 로컬 자정 기준으로 파싱해야 요일 라벨이 타임존과 무관하게 정확하다
              const [year, month, dayOfMonth] = day.date.split('-').map(Number)
              const date = new Date(year, month - 1, dayOfMonth)
              // 백엔드가 오래된 날짜부터 정렬해 보내므로 마지막 항목이 '오늘'
              const isToday = index === data.weeklyActivity.length - 1
              return (
                <div key={day.date} className="flex flex-1 flex-col items-center gap-1.5" title={`${day.date} · ${day.meetings}회`}>
                  <div className="flex w-full flex-1 items-end">
                    <div
                      className={`w-full rounded-t-[4px] transition-all ${
                        day.meetings > 0 ? 'bg-primary' : 'bg-line'
                      } ${isToday ? 'opacity-100' : 'opacity-70'}`}
                      style={{ height: `${Math.max(8, (day.meetings / maxDay) * 100)}%` }}
                    />
                  </div>
                  <span className={`text-[10px] font-bold ${isToday ? 'text-primary-deep' : 'text-faint'}`}>
                    {['일', '월', '화', '수', '목', '금', '토'][date.getDay()]}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      </Card>

      {/* 내 할 일 위젯 */}
      <Card className="flex flex-col p-6">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-[14px] font-extrabold text-ink">✅ 내 할 일</h3>
          <span className="font-mono text-[11px] text-faint">{data.pendingActionItems.length}건 남음</span>
        </div>
        {data.pendingActionItems.length === 0 ? (
          <p className="flex flex-1 items-center justify-center py-6 text-center text-[12.5px] text-faint">
            남은 할 일이 없어요 🎉
          </p>
        ) : (
          <ul className="max-h-[210px] space-y-1.5 overflow-y-auto">
            {data.pendingActionItems.map((item) => (
              <li key={item.id} className="group flex items-start gap-2.5 rounded-[10px] px-2 py-1.5 transition-colors hover:bg-surface">
                <button
                  onClick={() => completeMutation.mutate(item.id)}
                  title="완료로 표시"
                  className="mt-0.5 h-4 w-4 shrink-0 rounded-[5px] border-2 border-line-strong transition-colors hover:border-success hover:bg-success/20"
                />
                <button
                  onClick={() => navigate(`/rooms/${item.roomId}/report`)}
                  className="min-w-0 flex-1 text-left"
                >
                  <p className="truncate text-[12.5px] font-semibold leading-snug text-ink group-hover:text-primary-deep">
                    {item.task}
                  </p>
                  <p className="mt-0.5 truncate text-[11px] text-faint">
                    {item.roomTitle} · {item.assignee}
                    {item.due && item.due !== '미정' && ` · ~${item.due}`}
                  </p>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}

function Stat({ label, value, suffix }: { label: string; value: string; suffix: string }) {
  return (
    <div>
      <p className="text-[11px] font-bold uppercase tracking-wide text-faint">{label}</p>
      <p className="mt-1 text-[26px] font-black leading-none text-ink">
        {value}
        <span className="ml-0.5 text-[13px] font-bold text-muted">{suffix}</span>
      </p>
    </div>
  )
}
