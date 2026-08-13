import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Button } from '../components/ui'
import { useAuth } from '../lib/auth'
import { roomsApi } from '../lib/rooms'

/**
 * 회의 입장 전 장치 확인 로비.
 * 여기서 정한 마이크/카메라 상태는 sessionStorage로 회의 화면에 전달된다.
 */
export default function PreJoinPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)

  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const [micOn, setMicOn] = useState(true)
  const [camOn, setCamOn] = useState(true)
  const [deviceError, setDeviceError] = useState<string | null>(null)
  const [devices, setDevices] = useState<{ mic?: string; cam?: string }>({})

  useEffect(() => {
    let cancelled = false

    async function initDevices() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        streamRef.current = stream
        if (videoRef.current) {
          videoRef.current.srcObject = stream
        }
        const infos = await navigator.mediaDevices.enumerateDevices()
        setDevices({
          mic: infos.find((d) => d.kind === 'audioinput')?.label,
          cam: infos.find((d) => d.kind === 'videoinput')?.label,
        })
      } catch {
        if (!cancelled) {
          setDeviceError('카메라/마이크 권한이 필요합니다. 브라우저 권한을 확인해주세요.')
        }
      }
    }

    void initDevices()
    return () => {
      cancelled = true
      streamRef.current?.getTracks().forEach((t) => t.stop())
    }
  }, [])

  useEffect(() => {
    streamRef.current?.getVideoTracks().forEach((t) => (t.enabled = camOn))
  }, [camOn])
  useEffect(() => {
    streamRef.current?.getAudioTracks().forEach((t) => (t.enabled = micOn))
  }, [micOn])

  const enter = () => {
    sessionStorage.setItem('prejoin', JSON.stringify({ micOn, camOn }))
    streamRef.current?.getTracks().forEach((t) => t.stop())
    navigate(`/rooms/${roomId}/meet`)
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-surface px-6 py-10">
      <div className="grid w-full max-w-[880px] gap-6 lg:grid-cols-[1fr_300px]">
        <div>
          <h1 className="mb-4 text-xl font-extrabold text-ink">입장 전 장치 확인</h1>
          <div className="relative aspect-video overflow-hidden rounded-[18px] bg-video">
            <video
              ref={videoRef}
              autoPlay
              muted
              playsInline
              className={`h-full w-full object-cover ${camOn ? '' : 'invisible'}`}
            />
            {!camOn && (
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="flex h-24 w-24 items-center justify-center rounded-full bg-video-soft text-3xl font-black text-white/80">
                  {user?.nickname?.charAt(0) ?? '?'}
                </div>
              </div>
            )}
            <span className="absolute left-4 top-4 flex items-center gap-1.5 rounded-full bg-black/40 px-3 py-1 text-[11px] font-bold text-white">
              <i className="h-2 w-2 rounded-full bg-rec" /> 미리보기
            </span>
          </div>

          <div className="mt-5 flex items-center justify-center gap-4">
            <DeviceToggle on={micOn} onClick={() => setMicOn((v) => !v)} label="마이크" icon="mic" />
            <DeviceToggle on={camOn} onClick={() => setCamOn((v) => !v)} label="카메라" icon="cam" />
            <Button onClick={enter} className="ml-4 px-8">
              회의 입장
            </Button>
          </div>
        </div>

        <aside className="rounded-[16px] border border-line bg-card p-6">
          <h2 className="text-[15px] font-extrabold text-ink">장치 상태</h2>
          <ul className="mt-4 space-y-3 text-[13px]">
            <StatusRow ok={!deviceError} label="카메라" detail={devices.cam} />
            <StatusRow ok={!deviceError} label="마이크" detail={devices.mic} />
            <StatusRow ok label="AI 자막 · 인사이트" detail="입장하면 자동으로 켜집니다" />
          </ul>
          {deviceError && (
            <p className="mt-4 rounded-[10px] bg-danger/5 p-3 text-[12px] font-semibold text-danger">
              {deviceError}
            </p>
          )}
          <p className="mt-4 text-[12px] leading-relaxed text-faint">
            마이크를 켜면 발화가 15초 단위로 분석되어 실시간 자막과 회의 인사이트에 사용됩니다.
          </p>

          <BriefingCard roomId={Number(roomId)} />
        </aside>
      </div>
    </div>
  )
}

/** 입장 전 AI 브리핑: 지난 회의 요약 + 미완료 액션아이템 */
function BriefingCard({ roomId }: { roomId: number }) {
  const { data } = useQuery({
    queryKey: ['briefing', roomId],
    queryFn: () => roomsApi.briefing(roomId),
    staleTime: 5 * 60 * 1000,
  })

  if (!data?.lastMeetingTitle) return null

  return (
    <div className="mt-5 rounded-[12px] border border-primary-pale bg-primary-soft/50 p-4">
      <p className="text-[11px] font-extrabold uppercase tracking-wide text-primary-deep">🧠 지난 회의 브리핑</p>
      <p className="mt-1.5 text-[13px] font-bold text-ink">
        {data.lastMeetingTitle}
        {data.lastMeetingEndedTime && (
          <span className="ml-1.5 font-mono text-[10.5px] font-normal text-faint">
            {new Date(data.lastMeetingEndedTime).toLocaleDateString('ko-KR')}
          </span>
        )}
      </p>
      {data.lastMeetingSummary && (
        <p className="mt-1.5 line-clamp-4 text-[12px] leading-relaxed text-muted">{data.lastMeetingSummary}</p>
      )}
      {data.openActionItems.length > 0 && (
        <div className="mt-2.5">
          <p className="text-[10.5px] font-bold text-faint">미완료 액션아이템 {data.openActionItems.length}건</p>
          <ul className="mt-1 space-y-1">
            {data.openActionItems.slice(0, 3).map((item, i) => (
              <li key={i} className="truncate text-[11.5px] text-ink/80">
                • <span className="font-bold">{item.assignee}</span> — {item.task}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function DeviceToggle({
  on,
  onClick,
  label,
  icon,
}: {
  on: boolean
  onClick: () => void
  label: string
  icon: 'mic' | 'cam'
}) {
  return (
    <button
      onClick={onClick}
      title={label}
      className={`relative flex h-12 w-12 items-center justify-center rounded-full border transition-colors ${
        on
          ? 'border-line-strong bg-card text-ink hover:border-primary'
          : 'border-danger/40 bg-danger/10 text-danger'
      }`}
    >
      {icon === 'mic' ? <MicIcon /> : <CamIcon />}
      {!on && <span className="absolute h-[2px] w-7 rotate-45 rounded bg-danger" />}
    </button>
  )
}

function StatusRow({ ok, label, detail }: { ok: boolean; label: string; detail?: string }) {
  return (
    <li className="flex items-start gap-2.5">
      <span
        className={`mt-0.5 flex h-4 w-4 items-center justify-center rounded-full text-[10px] font-black text-white ${
          ok ? 'bg-success' : 'bg-danger'
        }`}
      >
        {ok ? '✓' : '!'}
      </span>
      <div>
        <p className="font-bold text-ink">{label}</p>
        {detail && <p className="text-[12px] text-muted">{detail}</p>}
      </div>
    </li>
  )
}

export function MicIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="9" y="3" width="6" height="11" rx="3" />
      <path d="M5 11a7 7 0 0 0 14 0M12 18v3" />
    </svg>
  )
}

export function CamIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="2" y="6" width="13" height="12" rx="3" />
      <path d="m15 10 6-3.5v11L15 14" />
    </svg>
  )
}
