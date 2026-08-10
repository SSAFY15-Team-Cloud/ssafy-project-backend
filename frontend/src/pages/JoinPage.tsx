import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { roomsApi } from '../lib/rooms'
import { useAuth } from '../lib/auth'
import { Card, Logo } from '../components/ui'

/**
 * 초대 딥링크 진입점: /join/:roomCode
 * 로그인 안 됐으면 로그인으로 보내고, 로그인 후 다시 여기로 돌아와 자동 참여한다.
 */
export default function JoinPage() {
  const { roomCode } = useParams<{ roomCode: string }>()
  const navigate = useNavigate()
  const { user, initialized } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const attempted = useRef(false)

  useEffect(() => {
    if (!initialized || !roomCode) return

    if (!user) {
      navigate(`/login?redirect=${encodeURIComponent(`/join/${roomCode}`)}`, { replace: true })
      return
    }

    if (attempted.current) return
    attempted.current = true

    roomsApi
      .join(roomCode)
      .then((result) => navigate(`/rooms/${result.roomId}/prejoin`, { replace: true }))
      .catch((err) => setError(err instanceof Error ? err.message : '회의 참여에 실패했습니다.'))
  }, [initialized, user, roomCode, navigate])

  return (
    <div className="flex min-h-full items-center justify-center bg-surface px-6">
      <Card className="w-full max-w-[380px] p-8 text-center">
        <div className="mb-6 flex justify-center">
          <Logo />
        </div>
        {error ? (
          <>
            <p className="text-[15px] font-bold text-danger">{error}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="mt-5 rounded-full bg-primary px-6 py-2.5 text-sm font-bold text-white hover:bg-primary-hover"
            >
              대시보드로
            </button>
          </>
        ) : (
          <>
            <p className="text-[15px] font-bold text-ink">회의에 참여하는 중…</p>
            <p className="mt-2 font-mono text-[13px] text-muted">코드: {roomCode}</p>
          </>
        )}
      </Card>
    </div>
  )
}
