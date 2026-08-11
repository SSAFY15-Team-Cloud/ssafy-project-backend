import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { roomsApi } from '../lib/rooms'
import type { MyRoomEntry } from '../lib/rooms'
import { useAuth } from '../lib/auth'
import { Button, Card, Input, PageShell, SectionTitle } from '../components/ui'

export default function DashboardPage() {
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)

  const [title, setTitle] = useState('')
  const [joinCode, setJoinCode] = useState('')
  const [creating, setCreating] = useState(false)
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [createdInvite, setCreatedInvite] = useState<{ roomId: number; roomCode: string } | null>(null)
  const [copied, setCopied] = useState(false)

  const inviteLink = createdInvite ? `${window.location.origin}/join/${createdInvite.roomCode}` : null

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setCreating(true)
    try {
      const room = await roomsApi.create(title.trim() || `${user?.nickname}님의 회의`)
      setCreatedInvite({ roomId: room.roomId, roomCode: room.roomCode })
    } catch (err) {
      setError(err instanceof Error ? err.message : '회의 생성에 실패했습니다.')
    } finally {
      setCreating(false)
    }
  }

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setJoining(true)
    try {
      const code = joinCode.trim()
      const result = await roomsApi.join(code)
      navigate(`/rooms/${result.roomId}/prejoin`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '회의 참여에 실패했습니다.')
    } finally {
      setJoining(false)
    }
  }

  const copyInvite = async () => {
    if (!inviteLink) return
    await navigator.clipboard.writeText(inviteLink)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <PageShell>
      <SectionTitle sub="회의를 만들거나 초대코드로 참여하세요. AI 자막·요약·문서 추천은 자동으로 켜집니다.">
        {user?.nickname}님, 안녕하세요
      </SectionTitle>

      {error && (
        <div className="mb-5 rounded-[12px] border border-danger/30 bg-danger/5 px-4 py-3 text-sm font-semibold text-danger">
          {error}
        </div>
      )}

      <div className="grid gap-5 lg:grid-cols-2">
        <Card className="p-7">
          <h3 className="text-[16px] font-extrabold text-ink">새 회의 시작</h3>
          <p className="mt-1 text-[13px] text-muted">방을 만들면 초대코드와 딥링크가 발급됩니다.</p>

          {!createdInvite ? (
            <form onSubmit={handleCreate} className="mt-5 space-y-4">
              <Input
                label="회의 이름"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 주간 스프린트 회의"
                maxLength={50}
              />
              <Button type="submit" disabled={creating} className="w-full">
                {creating ? '만드는 중…' : '회의 만들기'}
              </Button>
            </form>
          ) : (
            <div className="mt-5 space-y-4">
              <div className="rounded-[12px] border border-primary-pale bg-primary-soft p-4">
                <p className="text-[12px] font-bold text-primary-deep">초대코드</p>
                <p className="mt-1 font-mono text-[26px] font-extrabold tracking-[0.2em] text-ink">
                  {createdInvite.roomCode}
                </p>
                <p className="mt-2 break-all font-mono text-[12px] text-muted">{inviteLink}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => void copyInvite()} className="flex-1">
                  {copied ? '복사됨 ✓' : '초대링크 복사'}
                </Button>
                <Button
                  onClick={() => navigate(`/rooms/${createdInvite.roomId}/prejoin`)}
                  className="flex-1"
                >
                  입장하기
                </Button>
              </div>
            </div>
          )}
        </Card>

        <Card className="p-7">
          <h3 className="text-[16px] font-extrabold text-ink">초대코드로 참여</h3>
          <p className="mt-1 text-[13px] text-muted">
            8자리 코드를 입력하거나, 받은 초대링크를 브라우저에 붙여넣으세요.
          </p>
          <form onSubmit={handleJoin} className="mt-5 space-y-4">
            <Input
              label="초대코드"
              value={joinCode}
              onChange={(e) => setJoinCode(e.target.value)}
              placeholder="예: a1b2c3d4"
              className="font-mono tracking-[0.15em]"
              required
            />
            <Button type="submit" variant="outline" disabled={joining} className="w-full">
              {joining ? '참여 중…' : '참여하기'}
            </Button>
          </form>
        </Card>
      </div>

      <Card className="mt-5 p-7">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-[16px] font-extrabold text-ink">지식 위키</h3>
            <p className="mt-1 text-[13px] text-muted">
              팀 문서를 올려두면 회의 중 대화 맥락에 맞는 문서를 AI가 실시간으로 추천합니다.
            </p>
          </div>
          <Button variant="outline" onClick={() => navigate('/knowledge')}>
            문서 관리
          </Button>
        </div>
      </Card>

      <MyRoomsSection />
    </PageShell>
  )
}

function MyRoomsSection() {
  const navigate = useNavigate()
  const { data: rooms, isLoading } = useQuery({ queryKey: ['myRooms'], queryFn: roomsApi.myRooms })

  const rejoin = async (room: MyRoomEntry) => {
    try {
      await roomsApi.join(room.roomCode)
      navigate(`/rooms/${room.roomId}/prejoin`)
    } catch {
      /* 종료된 직후 등 — 무시 */
    }
  }

  if (isLoading || !rooms?.length) return null

  return (
    <Card className="mt-5 p-7">
      <h3 className="text-[16px] font-extrabold text-ink">내 회의</h3>
      <p className="mt-1 text-[13px] text-muted">참여했던 회의와 회의록을 모아봤어요.</p>
      <ul className="mt-4 divide-y divide-line">
        {rooms.slice(0, 8).map((room) => (
          <li key={`${room.roomId}-${room.joinedTime}`} className="flex items-center gap-3 py-3">
            <span
              className={`h-2 w-2 shrink-0 rounded-full ${
                room.status === 'RUNNING' ? 'bg-success' : 'bg-faint'
              }`}
              title={room.status === 'RUNNING' ? '진행 중' : '종료됨'}
            />
            <div className="min-w-0 flex-1">
              <p className="truncate text-[14px] font-bold text-ink">
                {room.title}
                {room.myRole === 'OWNER' && (
                  <span className="ml-1.5 rounded-full bg-primary-soft px-2 py-0.5 text-[10.5px] font-bold text-primary-deep">
                    방장
                  </span>
                )}
              </p>
              <p className="mt-0.5 font-mono text-[11.5px] text-faint">
                {room.roomCode} · {new Date(room.joinedTime).toLocaleString('ko-KR')}
              </p>
            </div>
            {room.status === 'RUNNING' ? (
              <button
                onClick={() => void rejoin(room)}
                className="shrink-0 rounded-full bg-primary px-4 py-1.5 text-[12px] font-bold text-white hover:bg-primary-hover"
              >
                입장
              </button>
            ) : room.reportStatus !== 'NONE' ? (
              <button
                onClick={() => navigate(`/rooms/${room.roomId}/report`)}
                className="shrink-0 rounded-full border border-line-strong px-4 py-1.5 text-[12px] font-bold text-muted hover:border-primary hover:text-primary"
              >
                회의록
              </button>
            ) : (
              <span className="shrink-0 text-[12px] text-faint">기록 없음</span>
            )}
          </li>
        ))}
      </ul>
    </Card>
  )
}
