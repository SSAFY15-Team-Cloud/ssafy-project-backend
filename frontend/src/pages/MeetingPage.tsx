import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  GridLayout,
  LiveKitRoom,
  ParticipantTile,
  RoomAudioRenderer,
  useLocalParticipant,
  useRoomContext,
  useTracks,
} from '@livekit/components-react'
import '@livekit/components-styles'
import { RoomEvent, Track } from 'livekit-client'
import type { LocalVideoTrack, RemoteParticipant } from 'livekit-client'
import { aiApi, knowledgeApi, roomsApi } from '../lib/rooms'
import type { CopilotAnswer, RtcToken, SpeakerStat } from '../lib/rooms'
import { createRoomStompClient } from '../lib/stomp'
import type {
  ChatMessagePayload,
  InsightPayload,
  RecommendedDocument,
  TranscriptPayload,
} from '../lib/stomp'
import { startChunkedAudioUpload } from '../lib/audioRecorder'
import { useAuth } from '../lib/auth'
import { CamIcon, MicIcon } from './PreJoinPage'

type PanelTab = 'chat' | 'people' | 'ai'
type CaptionLang = 'ko' | 'en' | 'ja'
type TimedTranscript = TranscriptPayload & { receivedAt: number }
type BackgroundMode = 'none' | 'blur' | string // string = 배경 이미지 URL

type DataMessage =
  | { type: 'reaction'; emoji: string }
  | { type: 'hand'; raised: boolean }

interface FloatingReaction {
  id: number
  emoji: string
  name: string
  left: number
}

const REACTION_EMOJIS = ['👍', '❤️', '😂', '🎉', '👏', '😮']

const BACKGROUNDS: { label: string; url: string; thumb: string }[] = [
  { label: '그라데이션', url: '/backgrounds/bg-gradient.svg', thumb: 'linear-gradient(135deg,#5276df,#263f9c)' },
  { label: '오피스', url: '/backgrounds/bg-office.svg', thumb: 'linear-gradient(135deg,#f5f0e8,#d8cfc0)' },
  { label: '숲', url: '/backgrounds/bg-forest.svg', thumb: 'linear-gradient(135deg,#1d4d3a,#3d8b64)' },
]

const CAPTION_TTL_MS = 8_000

export default function MeetingPage() {
  const { roomId: roomIdParam } = useParams<{ roomId: string }>()
  const roomId = Number(roomIdParam)
  const navigate = useNavigate()

  const [rtc, setRtc] = useState<RtcToken | null>(null)
  const [error, setError] = useState<string | null>(null)

  const prejoin = useMemo(() => {
    try {
      return JSON.parse(sessionStorage.getItem('prejoin') ?? '{"micOn":true,"camOn":true}') as {
        micOn: boolean
        camOn: boolean
      }
    } catch {
      return { micOn: true, camOn: true }
    }
  }, [])

  useEffect(() => {
    roomsApi
      .rtcToken(roomId)
      .then(setRtc)
      .catch((err) => setError(err instanceof Error ? err.message : '회의 연결에 실패했습니다.'))
  }, [roomId])

  if (error) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-4 bg-video text-white">
        <p className="font-bold">{error}</p>
        <button
          onClick={() => navigate('/dashboard')}
          className="rounded-full bg-white/10 px-6 py-2.5 text-sm font-bold hover:bg-white/20"
        >
          대시보드로 돌아가기
        </button>
      </div>
    )
  }

  if (!rtc) {
    return (
      <div className="flex h-full items-center justify-center bg-video text-white/70">
        회의에 연결하는 중…
      </div>
    )
  }

  return (
    <LiveKitRoom
      serverUrl={rtc.serverUrl}
      token={rtc.token}
      connect
      audio={prejoin.micOn}
      video={prejoin.camOn}
      onDisconnected={() => navigate(`/rooms/${roomId}/report`)}
      className="h-full"
      data-lk-theme="default"
    >
      <MeetingRoomInner roomId={roomId} />
    </LiveKitRoom>
  )
}

function MeetingRoomInner({ roomId }: { roomId: number }) {
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)
  const room = useRoomContext()

  const [tab, setTab] = useState<PanelTab>('ai')
  const [panelOpen, setPanelOpen] = useState(true)
  const [chatMessages, setChatMessages] = useState<ChatMessagePayload[]>([])
  const [transcripts, setTranscripts] = useState<TimedTranscript[]>([])
  const [insight, setInsight] = useState<InsightPayload | null>(null)
  const [recommendations, setRecommendations] = useState<RecommendedDocument[]>([])
  const [participants, setParticipants] = useState<{ userId: number; nickname: string }[]>([])
  const [chatInput, setChatInput] = useState('')
  const [unread, setUnread] = useState(0)
  const [leaving, setLeaving] = useState(false)

  // 신규 기능 상태
  const [reactions, setReactions] = useState<FloatingReaction[]>([])
  const [reactionPickerOpen, setReactionPickerOpen] = useState(false)
  const [raisedHands, setRaisedHands] = useState<Record<string, string>>({}) // identity → name
  const [myHandRaised, setMyHandRaised] = useState(false)
  const [bgMode, setBgMode] = useState<BackgroundMode>('none')
  const [bgBusy, setBgBusy] = useState(false)
  const [bgPickerOpen, setBgPickerOpen] = useState(false)
  const [ccOn, setCcOn] = useState(true)
  const [, setCaptionTick] = useState(0) // 자막 만료 재렌더용

  const stompRef = useRef<ReturnType<typeof createRoomStompClient> | null>(null)
  const reactionSeq = useRef(0)
  const tabRef = useRef(tab)
  tabRef.current = tab

  const { localParticipant, isMicrophoneEnabled, isCameraEnabled, isScreenShareEnabled } =
    useLocalParticipant()

  // 채팅 히스토리 + 참가자 로드
  useEffect(() => {
    roomsApi
      .messages(roomId)
      .then((res) => setChatMessages(res.messages))
      .catch(() => {})
    const loadParticipants = () =>
      roomsApi
        .participants(roomId)
        .then((res) => setParticipants(res.users.map((u) => ({ userId: u.userId, nickname: u.nickname }))))
        .catch(() => {})
    loadParticipants()
    const interval = setInterval(loadParticipants, 10_000)
    return () => clearInterval(interval)
  }, [roomId])

  // STOMP 연결 (채팅 + 자막 + AI 이벤트)
  useEffect(() => {
    const client = createRoomStompClient(roomId, {
      onChatMessage: (payload) => {
        setChatMessages((prev) => [...prev, payload])
        if (tabRef.current !== 'chat') setUnread((n) => n + 1)
      },
      onChatDeleted: ({ messageId }) =>
        setChatMessages((prev) => prev.filter((m) => m.messageId !== messageId)),
      onTranscript: (payload) =>
        setTranscripts((prev) => [...prev.slice(-99), { ...payload, receivedAt: Date.now() }]),
      onInsight: setInsight,
      onRecommendations: (payload) => setRecommendations(payload.documents),
    })
    stompRef.current = client
    return () => client.deactivate()
  }, [roomId])

  // LiveKit 데이터 채널 수신 (리액션 / 손들기)
  const spawnReaction = useCallback((emoji: string, name: string) => {
    const id = ++reactionSeq.current
    setReactions((prev) => [...prev, { id, emoji, name, left: 12 + Math.random() * 70 }])
    setTimeout(() => setReactions((prev) => prev.filter((r) => r.id !== id)), 2800)
  }, [])

  useEffect(() => {
    const onData = (payload: Uint8Array, participant?: RemoteParticipant) => {
      try {
        const message = JSON.parse(new TextDecoder().decode(payload)) as DataMessage
        const name = participant?.name || participant?.identity || '익명'
        if (message.type === 'reaction') {
          spawnReaction(message.emoji, name)
        }
        if (message.type === 'hand' && participant) {
          setRaisedHands((prev) => {
            const next = { ...prev }
            if (message.raised) next[participant.identity] = name
            else delete next[participant.identity]
            return next
          })
        }
      } catch {
        /* 알 수 없는 데이터 무시 */
      }
    }
    const onLeft = (participant: RemoteParticipant) => {
      setRaisedHands((prev) => {
        const next = { ...prev }
        delete next[participant.identity]
        return next
      })
    }
    room.on(RoomEvent.DataReceived, onData)
    room.on(RoomEvent.ParticipantDisconnected, onLeft)
    return () => {
      room.off(RoomEvent.DataReceived, onData)
      room.off(RoomEvent.ParticipantDisconnected, onLeft)
    }
  }, [room, spawnReaction])

  const publishData = useCallback(
    (message: DataMessage) => {
      void localParticipant.publishData(new TextEncoder().encode(JSON.stringify(message)), {
        reliable: true,
      })
    },
    [localParticipant],
  )

  const sendReaction = (emoji: string) => {
    publishData({ type: 'reaction', emoji })
    spawnReaction(emoji, user?.nickname ?? '나') // 내 화면에도 표시
    setReactionPickerOpen(false)
  }

  const toggleHand = () => {
    const next = !myHandRaised
    setMyHandRaised(next)
    publishData({ type: 'hand', raised: next })
    setRaisedHands((prev) => {
      const map = { ...prev }
      const identity = String(user?.userId ?? 'me')
      if (next) map[identity] = user?.nickname ?? '나'
      else delete map[identity]
      return map
    })
  }

  // 가상 배경: 없음 / 블러 / 이미지 (Chromium 계열만 지원)
  const bgSupported = typeof window !== 'undefined' && 'MediaStreamTrackProcessor' in window
  const applyBackground = async (mode: BackgroundMode) => {
    if (bgBusy || !isCameraEnabled) return
    setBgBusy(true)
    setBgPickerOpen(false)
    try {
      const publication = localParticipant.getTrackPublication(Track.Source.Camera)
      const track = publication?.track as LocalVideoTrack | undefined
      if (!track) return
      if (mode === 'none') {
        await track.stopProcessor()
      } else if (mode === 'blur') {
        const { BackgroundBlur } = await import('@livekit/track-processors')
        await track.stopProcessor()
        await track.setProcessor(BackgroundBlur(10))
      } else {
        const { VirtualBackground } = await import('@livekit/track-processors')
        await track.stopProcessor()
        await track.setProcessor(VirtualBackground(mode))
      }
      setBgMode(mode)
    } catch (e) {
      console.warn('virtual background failed', e)
    } finally {
      setBgBusy(false)
    }
  }

  // CC 오버레이: 만료 자막 정리를 위해 켜져있는 동안 주기 재렌더
  useEffect(() => {
    if (!ccOn) return
    const interval = setInterval(() => setCaptionTick((n) => n + 1), 2000)
    return () => clearInterval(interval)
  }, [ccOn])

  const visibleCaptions = ccOn
    ? transcripts.filter((t) => Date.now() - t.receivedAt < CAPTION_TTL_MS).slice(-3)
    : []

  // 마이크가 켜져 있는 동안 15초 청크 STT 업로드
  useEffect(() => {
    if (!isMicrophoneEnabled) return
    const publication = localParticipant.getTrackPublication(Track.Source.Microphone)
    const mediaTrack = publication?.track?.mediaStreamTrack
    if (!mediaTrack) return
    const stop = startChunkedAudioUpload(roomId, mediaTrack)
    return stop
  }, [roomId, isMicrophoneEnabled, localParticipant])

  const tracks = useTracks(
    [
      { source: Track.Source.Camera, withPlaceholder: true },
      { source: Track.Source.ScreenShare, withPlaceholder: false },
    ],
    { onlySubscribed: false },
  )

  const sendChat = useCallback(() => {
    const message = chatInput.trim()
    if (!message) return
    stompRef.current?.sendChat(message)
    setChatInput('')
  }, [chatInput])

  const leave = async () => {
    if (leaving) return
    setLeaving(true)
    try {
      await roomsApi.leave(roomId)
    } catch {
      /* 이미 종료된 방 등 — 이동은 계속한다 */
    }
    navigate(`/rooms/${roomId}/report`)
  }

  const openTab = (next: PanelTab) => {
    setTab(next)
    setPanelOpen(true)
    if (next === 'chat') setUnread(0)
  }

  const raisedHandNames = Object.values(raisedHands)

  return (
    <div className="flex h-full flex-col bg-video">
      <div className="flex min-h-0 flex-1">
        {/* 비디오 그리드 + 리액션 오버레이 */}
        <div className="relative min-w-0 flex-1 p-4">
          <GridLayout tracks={tracks} className="h-full">
            <ParticipantTile />
          </GridLayout>

          {/* 플로팅 이모지 리액션 */}
          <div className="pointer-events-none absolute inset-0 overflow-hidden">
            {reactions.map((r) => (
              <div
                key={r.id}
                className="reaction-float absolute bottom-16 flex flex-col items-center"
                style={{ left: `${r.left}%` }}
              >
                <span className="text-4xl">{r.emoji}</span>
                <span className="mt-1 rounded-full bg-black/50 px-2 py-0.5 text-[10px] font-bold text-white">
                  {r.name}
                </span>
              </div>
            ))}
          </div>

          {/* 손들기 배너 */}
          {raisedHandNames.length > 0 && (
            <div className="absolute left-1/2 top-6 -translate-x-1/2 rounded-full bg-[#f59e0b]/90 px-4 py-1.5 text-[13px] font-bold text-white shadow-lg">
              ✋ {raisedHandNames.join(', ')}
            </div>
          )}

          {/* CC 자막 오버레이 */}
          {visibleCaptions.length > 0 && (
            <div className="pointer-events-none absolute bottom-8 left-1/2 flex w-full max-w-[720px] -translate-x-1/2 flex-col items-center gap-1.5 px-6">
              {visibleCaptions.map((caption) => (
                <p
                  key={caption.audioId}
                  className="rounded-[10px] bg-black/65 px-4 py-1.5 text-center text-[15px] font-semibold leading-relaxed text-white shadow-lg backdrop-blur-sm"
                >
                  <span className="mr-1.5 text-[#8ea5f8]">{caption.speakerName}</span>
                  {caption.text}
                </p>
              ))}
            </div>
          )}
        </div>

        {/* 사이드 패널 */}
        {panelOpen && (
          <aside className="flex w-[340px] flex-col border-l border-white/10 bg-[#131a3a]">
            <div className="flex border-b border-white/10">
              {(
                [
                  ['ai', 'AI'],
                  ['chat', unread > 0 ? `채팅 (${unread})` : '채팅'],
                  ['people', `참가자 ${participants.length}`],
                ] as [PanelTab, string][]
              ).map(([key, label]) => (
                <button
                  key={key}
                  onClick={() => openTab(key)}
                  className={`flex-1 py-3 text-[13px] font-bold transition-colors ${
                    tab === key
                      ? 'border-b-2 border-[#8ea5f8] text-white'
                      : 'text-white/50 hover:text-white/80'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto">
              {tab === 'ai' && (
                <AiPanel
                  roomId={roomId}
                  transcripts={transcripts}
                  insight={insight}
                  recommendations={recommendations}
                />
              )}
              {tab === 'chat' && <ChatPanel messages={chatMessages} myUserId={user?.userId} />}
              {tab === 'people' && (
                <PeoplePanel roomId={roomId} participants={participants} raisedHands={raisedHands} />
              )}
            </div>

            {tab === 'chat' && (
              <div className="border-t border-white/10 p-3">
                <div className="flex gap-2">
                  <input
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.nativeEvent.isComposing) sendChat()
                    }}
                    placeholder="메시지 보내기"
                    className="min-w-0 flex-1 rounded-full bg-white/10 px-4 py-2.5 text-[13px] text-white outline-none placeholder:text-white/40 focus:bg-white/15"
                  />
                  <button
                    onClick={sendChat}
                    className="rounded-full bg-[#5276df] px-4 text-[13px] font-bold text-white hover:bg-[#4265c9]"
                  >
                    전송
                  </button>
                </div>
              </div>
            )}
          </aside>
        )}
      </div>

      {/* 컨트롤 바 */}
      <div className="flex items-center justify-between border-t border-white/10 bg-[#0b1129] px-6 py-3">
        <span className="hidden font-mono text-[12px] text-white/40 sm:block">room #{roomId}</span>

        <div className="flex items-center gap-3">
          <ControlButton
            on={isMicrophoneEnabled}
            onClick={() => void localParticipant.setMicrophoneEnabled(!isMicrophoneEnabled)}
            label="마이크"
          >
            <MicIcon />
          </ControlButton>
          <ControlButton
            on={isCameraEnabled}
            onClick={() => void localParticipant.setCameraEnabled(!isCameraEnabled)}
            label="카메라"
          >
            <CamIcon />
          </ControlButton>
          <ControlButton
            on={isScreenShareEnabled}
            activeStyle="share"
            onClick={() => void localParticipant.setScreenShareEnabled(!isScreenShareEnabled)}
            label="화면공유"
          >
            <ShareIcon />
          </ControlButton>

          <ControlButton on={ccOn} activeStyle="share" onClick={() => setCcOn((v) => !v)} label="자막 오버레이">
            <span className="text-[12px] font-black leading-none">CC</span>
          </ControlButton>

          {bgSupported && (
            <div className="relative">
              <ControlButton
                on={bgMode !== 'none'}
                activeStyle="share"
                onClick={() => setBgPickerOpen((v) => !v)}
                label="가상 배경"
              >
                <BlurIcon />
              </ControlButton>
              {bgPickerOpen && (
                <div className="absolute bottom-14 left-1/2 flex -translate-x-1/2 items-center gap-2 rounded-[14px] bg-[#1c2547] px-3 py-2.5 shadow-xl">
                  <BgOption
                    label="없음"
                    active={bgMode === 'none'}
                    disabled={bgBusy}
                    onClick={() => void applyBackground('none')}
                  >
                    <span className="text-[11px] font-bold text-white/70">없음</span>
                  </BgOption>
                  <BgOption
                    label="블러"
                    active={bgMode === 'blur'}
                    disabled={bgBusy}
                    onClick={() => void applyBackground('blur')}
                  >
                    <span
                      className="h-full w-full rounded-[8px]"
                      style={{ background: 'linear-gradient(135deg,#9aa7c7,#5a6788)', filter: 'blur(1.5px)' }}
                    />
                  </BgOption>
                  {BACKGROUNDS.map((bg) => (
                    <BgOption
                      key={bg.url}
                      label={bg.label}
                      active={bgMode === bg.url}
                      disabled={bgBusy}
                      onClick={() => void applyBackground(bg.url)}
                    >
                      <span className="h-full w-full rounded-[8px]" style={{ background: bg.thumb }} />
                    </BgOption>
                  ))}
                </div>
              )}
            </div>
          )}

          <ControlButton on={myHandRaised} activeStyle="hand" onClick={toggleHand} label="손들기">
            <span className="text-[17px] leading-none">✋</span>
          </ControlButton>

          {/* 이모지 리액션 */}
          <div className="relative">
            <ControlButton
              on={reactionPickerOpen}
              activeStyle="share"
              onClick={() => setReactionPickerOpen((v) => !v)}
              label="리액션"
            >
              <span className="text-[17px] leading-none">😀</span>
            </ControlButton>
            {reactionPickerOpen && (
              <div className="absolute bottom-14 left-1/2 flex -translate-x-1/2 gap-1 rounded-full bg-[#1c2547] px-3 py-2 shadow-xl">
                {REACTION_EMOJIS.map((emoji) => (
                  <button
                    key={emoji}
                    onClick={() => sendReaction(emoji)}
                    className="rounded-full p-1.5 text-[20px] transition-transform hover:scale-125"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            )}
          </div>

          <button
            onClick={() => void leave()}
            disabled={leaving}
            className="ml-2 rounded-full bg-danger px-6 py-3 text-[13px] font-bold text-white hover:opacity-90 disabled:opacity-60"
          >
            {leaving ? '나가는 중…' : '나가기'}
          </button>
        </div>

        <button
          onClick={() => setPanelOpen((v) => !v)}
          className="rounded-full border border-white/15 px-4 py-2 text-[12px] font-bold text-white/70 hover:bg-white/10"
        >
          {panelOpen ? '패널 접기' : '패널 열기'}
        </button>
      </div>

      <RoomAudioRenderer />
    </div>
  )
}

function ControlButton({
  on,
  onClick,
  label,
  children,
  activeStyle = 'device',
}: {
  on: boolean
  onClick: () => void
  label: string
  children: React.ReactNode
  activeStyle?: 'device' | 'share' | 'hand'
}) {
  let activeClass: string
  if (activeStyle === 'device') {
    activeClass = on ? 'bg-white/10 text-white hover:bg-white/20' : 'bg-danger/20 text-danger'
  } else if (activeStyle === 'hand') {
    activeClass = on ? 'bg-[#f59e0b] text-white' : 'bg-white/10 text-white hover:bg-white/20'
  } else {
    activeClass = on ? 'bg-[#5276df] text-white' : 'bg-white/10 text-white hover:bg-white/20'
  }
  return (
    <button
      onClick={onClick}
      title={label}
      className={`relative flex h-11 w-11 items-center justify-center rounded-full transition-colors ${activeClass}`}
    >
      {children}
      {activeStyle === 'device' && !on && (
        <span className="absolute h-[2px] w-6 rotate-45 rounded bg-danger" />
      )}
    </button>
  )
}

function BgOption({
  label,
  active,
  disabled,
  onClick,
  children,
}: {
  label: string
  active: boolean
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={label}
      className={`flex h-12 w-16 items-center justify-center overflow-hidden rounded-[10px] border-2 transition-colors disabled:opacity-50 ${
        active ? 'border-[#8ea5f8]' : 'border-white/10 hover:border-white/40'
      } bg-white/5`}
    >
      {children}
    </button>
  )
}

function ShareIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="3" y="4" width="18" height="13" rx="2" />
      <path d="M12 21v-4M12 13V8m0 0-3 3m3-3 3 3" />
    </svg>
  )
}

function BlurIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <circle cx="12" cy="9" r="3.5" />
      <path d="M5.5 20c1.2-3 3.6-4.5 6.5-4.5s5.3 1.5 6.5 4.5" />
      <path d="M2 5h3M2 9h2M2 13h3M19 5h3M20 9h2M19 13h3" strokeDasharray="1 3" />
    </svg>
  )
}

function AiPanel({
  roomId,
  transcripts,
  insight,
  recommendations,
}: {
  roomId: number
  transcripts: TranscriptPayload[]
  insight: InsightPayload | null
  recommendations: RecommendedDocument[]
}) {
  const transcriptEndRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [transcripts.length])

  // 자막 번역
  const [captionLang, setCaptionLang] = useState<CaptionLang>('ko')
  const [translations, setTranslations] = useState<Record<string, string>>({}) // `${lang}:${audioId}` → text
  const translating = useRef(false)

  useEffect(() => {
    if (captionLang === 'ko' || translating.current) return
    const pending = transcripts
      .filter((t) => !translations[`${captionLang}:${t.audioId}`])
      .slice(-10)
    if (pending.length === 0) return

    translating.current = true
    aiApi
      .translate(pending.map((t) => t.text), captionLang)
      .then((res) => {
        setTranslations((prev) => {
          const next = { ...prev }
          pending.forEach((t, i) => {
            next[`${captionLang}:${t.audioId}`] = res.translations[i]
          })
          return next
        })
      })
      .catch(() => {})
      .finally(() => {
        translating.current = false
      })
  }, [captionLang, transcripts, translations])

  // 코파일럿
  const [question, setQuestion] = useState('')
  const [asking, setAsking] = useState(false)
  const [qaList, setQaList] = useState<{ q: string; a: CopilotAnswer | null }[]>([])

  const ask = async () => {
    const q = question.trim()
    if (!q || asking) return
    setAsking(true)
    setQuestion('')
    setQaList((prev) => [...prev, { q, a: null }])
    try {
      const answer = await roomsApi.copilotAsk(roomId, q)
      setQaList((prev) => prev.map((item, i) => (i === prev.length - 1 ? { ...item, a: answer } : item)))
    } catch {
      setQaList((prev) =>
        prev.map((item, i) =>
          i === prev.length - 1
            ? { ...item, a: { answer: '답변에 실패했어요. 잠시 후 다시 시도해주세요.', sources: [] } }
            : item,
        ),
      )
    } finally {
      setAsking(false)
    }
  }

  const downloadSource = async (documentId: number) => {
    try {
      const { downloadUrl } = await knowledgeApi.downloadUrl(documentId)
      window.open(downloadUrl, '_blank')
    } catch {
      /* ignore */
    }
  }

  return (
    <div className="space-y-5 p-4">
      {/* 코파일럿 */}
      <section>
        <PanelHeading live={asking}>AI 코파일럿</PanelHeading>
        <div className="space-y-2.5">
          {qaList.length === 0 && (
            <p className="text-[12px] leading-relaxed text-white/40">
              "지금까지 결정된 게 뭐야?", "OO 문서에 뭐라고 써있어?" 처럼 물어보세요. 회의 내용과
              지식 위키를 함께 검색해 답합니다.
            </p>
          )}
          {qaList.map((item, i) => (
            <div key={i} className="rounded-[12px] bg-white/5 p-3">
              <p className="text-[12px] font-bold text-[#a9bcff]">Q. {item.q}</p>
              {item.a === null ? (
                <p className="mt-1.5 animate-pulse text-[12px] text-white/40">생각하는 중…</p>
              ) : (
                <>
                  <p className="mt-1.5 whitespace-pre-wrap text-[12.5px] leading-relaxed text-white/85">
                    {item.a.answer}
                  </p>
                  {item.a.sources.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {item.a.sources.map((s) => (
                        <button
                          key={s.documentId}
                          onClick={() => void downloadSource(s.documentId)}
                          className="rounded-full bg-[#5276df]/25 px-2.5 py-1 text-[10.5px] font-bold text-[#a9bcff] hover:bg-[#5276df]/40"
                          title="문서 다운로드"
                        >
                          📄 {s.title}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          ))}
          <div className="flex gap-2">
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.nativeEvent.isComposing) void ask()
              }}
              placeholder="회의에 대해 질문하기…"
              className="min-w-0 flex-1 rounded-full bg-white/10 px-3.5 py-2 text-[12.5px] text-white outline-none placeholder:text-white/35 focus:bg-white/15"
            />
            <button
              onClick={() => void ask()}
              disabled={asking}
              className="rounded-full bg-[#5276df] px-3.5 text-[12px] font-bold text-white hover:bg-[#4265c9] disabled:opacity-50"
            >
              질문
            </button>
          </div>
        </div>
      </section>

      {/* 추천 문서 */}
      <section>
        <PanelHeading live={recommendations.length > 0}>관련 문서 추천</PanelHeading>
        {recommendations.length === 0 ? (
          <p className="text-[12px] leading-relaxed text-white/40">
            대화가 쌓이면 지식 위키에서 관련 문서를 찾아 여기 보여드려요.
          </p>
        ) : (
          <ul className="space-y-2">
            {recommendations.map((doc) => (
              <li key={doc.documentId} className="rounded-[12px] bg-white/5 p-3">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-[13px] font-bold text-white">{doc.title}</p>
                  <span className="shrink-0 rounded-full bg-[#5276df]/30 px-2 py-0.5 font-mono text-[10px] text-[#a9bcff]">
                    {(doc.score * 100).toFixed(0)}%
                  </span>
                </div>
                <p className="mt-1 line-clamp-2 text-[12px] leading-relaxed text-white/50">{doc.snippet}</p>
                <a
                  href={doc.downloadUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-2 inline-block text-[12px] font-bold text-[#8ea5f8] hover:underline"
                >
                  문서 다운로드 ↓
                </a>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 롤링 인사이트 */}
      <section>
        <div className="mb-2 flex items-center justify-between">
          <PanelHeading live={!!insight} noMargin>
            회의 인사이트
          </PanelHeading>
          {insight?.insight.mood && (
            <span className="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-bold text-white/80">
              {insight.insight.mood.emoji} {insight.insight.mood.label}
            </span>
          )}
        </div>
        {!insight ? (
          <p className="text-[12px] leading-relaxed text-white/40">
            발화가 쌓이면 AI가 요약 · 액션아이템 · 논점을 계속 갱신합니다.
          </p>
        ) : (
          <div className="space-y-3 text-[12.5px] leading-relaxed">
            {insight.insight.summary && <p className="text-white/85">{insight.insight.summary}</p>}
            {!!insight.insight.keyPoints?.length && (
              <div>
                <p className="mb-1 text-[11px] font-bold text-white/50">핵심 논점</p>
                <ul className="space-y-1">
                  {insight.insight.keyPoints.map((point, i) => (
                    <li key={i} className="flex gap-1.5 text-white/75">
                      <span className="text-[#8ea5f8]">•</span> {point}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {!!insight.insight.actionItems?.length && (
              <div>
                <p className="mb-1 text-[11px] font-bold text-white/50">액션 아이템</p>
                <ul className="space-y-1.5">
                  {insight.insight.actionItems.map((item, i) => (
                    <li key={i} className="rounded-[10px] bg-white/5 px-2.5 py-1.5 text-white/80">
                      <span className="font-bold text-[#a9bcff]">{item.assignee}</span> — {item.task}
                      {item.due && item.due !== '미정' && (
                        <span className="ml-1 text-white/40">({item.due})</span>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {!!insight.insight.openQuestions?.length && (
              <div>
                <p className="mb-1 text-[11px] font-bold text-white/50">미해결 질문</p>
                <ul className="space-y-1">
                  {insight.insight.openQuestions.map((q, i) => (
                    <li key={i} className="flex gap-1.5 text-white/70">
                      <span className="text-[#e0526b]">?</span> {q}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </section>

      {/* 실시간 자막 + 번역 */}
      <section>
        <div className="mb-2 flex items-center justify-between">
          <PanelHeading live={transcripts.length > 0} noMargin>
            실시간 자막
          </PanelHeading>
          <select
            value={captionLang}
            onChange={(e) => setCaptionLang(e.target.value as CaptionLang)}
            className="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-bold text-white/80 outline-none"
          >
            <option value="ko">원문</option>
            <option value="en">English</option>
            <option value="ja">日本語</option>
          </select>
        </div>
        {transcripts.length === 0 ? (
          <p className="text-[12px] leading-relaxed text-white/40">
            마이크를 켜고 말하면 잠시 후 자막이 이곳에 표시됩니다.
          </p>
        ) : (
          <ul className="space-y-2">
            {transcripts.map((t) => {
              const translated =
                captionLang !== 'ko' ? translations[`${captionLang}:${t.audioId}`] : undefined
              return (
                <li key={t.audioId} className="text-[12.5px] leading-relaxed">
                  <span className="font-bold text-[#8ea5f8]">{t.speakerName}</span>{' '}
                  <span className="text-white/80">{translated ?? t.text}</span>
                  {translated && (
                    <p className="mt-0.5 pl-1 text-[11px] italic text-white/35">{t.text}</p>
                  )}
                </li>
              )
            })}
            <div ref={transcriptEndRef} />
          </ul>
        )}
      </section>
    </div>
  )
}

function PanelHeading({
  children,
  live,
  noMargin,
}: {
  children: React.ReactNode
  live?: boolean
  noMargin?: boolean
}) {
  return (
    <h3
      className={`flex items-center gap-2 text-[12px] font-extrabold uppercase tracking-wide text-white/60 ${
        noMargin ? '' : 'mb-2'
      }`}
    >
      {children}
      {live && <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-[#4ade80]" />}
    </h3>
  )
}

function ChatPanel({
  messages,
  myUserId,
}: {
  messages: ChatMessagePayload[]
  myUserId?: number
}) {
  const endRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length])

  return (
    <div className="space-y-3 p-4">
      {messages.length === 0 && (
        <p className="text-[12px] text-white/40">아직 메시지가 없습니다. 첫 메시지를 보내보세요.</p>
      )}
      {messages.map((m) => {
        const mine = m.senderId === myUserId
        return (
          <div key={m.messageId} className={`flex flex-col ${mine ? 'items-end' : 'items-start'}`}>
            {!mine && <span className="mb-0.5 text-[11px] font-bold text-white/50">{m.senderNickname}</span>}
            <div
              className={`max-w-[85%] rounded-[14px] px-3 py-2 text-[13px] leading-relaxed ${
                mine ? 'bg-[#5276df] text-white' : 'bg-white/10 text-white/90'
              }`}
            >
              {m.message}
            </div>
          </div>
        )
      })}
      <div ref={endRef} />
    </div>
  )
}

function PeoplePanel({
  roomId,
  participants,
  raisedHands,
}: {
  roomId: number
  participants: { userId: number; nickname: string }[]
  raisedHands: Record<string, string>
}) {
  const [stats, setStats] = useState<SpeakerStat[]>([])

  useEffect(() => {
    const load = () =>
      roomsApi
        .speakingStats(roomId)
        .then(setStats)
        .catch(() => {})
    load()
    const interval = setInterval(load, 10_000)
    return () => clearInterval(interval)
  }, [roomId])

  const maxSeconds = Math.max(1, ...stats.map((s) => s.totalSeconds))
  const statByUser = new Map(stats.map((s) => [s.userId, s.totalSeconds]))
  const totalSeconds = stats.reduce((acc, s) => acc + s.totalSeconds, 0)

  return (
    <div className="p-4">
      <ul className="space-y-2">
        {participants.map((p) => {
          const seconds = statByUser.get(p.userId) ?? 0
          const handUp = raisedHands[String(p.userId)] !== undefined
          return (
            <li key={p.userId} className="rounded-[12px] px-2 py-2 hover:bg-white/5">
              <div className="flex items-center gap-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#5276df]/40 text-[13px] font-black text-white">
                  {p.nickname.charAt(0)}
                </span>
                <span className="min-w-0 flex-1 truncate text-[13px] font-bold text-white/85">
                  {p.nickname} {handUp && <span title="손들기">✋</span>}
                </span>
                <span className="shrink-0 font-mono text-[11px] text-white/40">
                  {formatDuration(seconds)}
                </span>
              </div>
              {/* 발언 점유율 바 */}
              <div className="ml-12 mt-1.5 h-1 overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full rounded-full bg-[#8ea5f8] transition-all duration-700"
                  style={{ width: `${(seconds / maxSeconds) * 100}%` }}
                />
              </div>
            </li>
          )
        })}
      </ul>
      {totalSeconds > 0 && (
        <p className="mt-4 text-center font-mono text-[11px] text-white/35">
          총 발언 {formatDuration(totalSeconds)}
        </p>
      )}
    </div>
  )
}

function formatDuration(seconds: number) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}분 ${s}초` : `${s}초`
}
