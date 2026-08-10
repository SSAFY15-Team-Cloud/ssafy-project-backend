import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  GridLayout,
  LiveKitRoom,
  ParticipantTile,
  RoomAudioRenderer,
  useLocalParticipant,
  useTracks,
} from '@livekit/components-react'
import '@livekit/components-styles'
import { Track } from 'livekit-client'
import { roomsApi } from '../lib/rooms'
import type { RtcToken } from '../lib/rooms'
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

  const [tab, setTab] = useState<PanelTab>('ai')
  const [panelOpen, setPanelOpen] = useState(true)
  const [chatMessages, setChatMessages] = useState<ChatMessagePayload[]>([])
  const [transcripts, setTranscripts] = useState<TranscriptPayload[]>([])
  const [insight, setInsight] = useState<InsightPayload | null>(null)
  const [recommendations, setRecommendations] = useState<RecommendedDocument[]>([])
  const [participants, setParticipants] = useState<{ userId: number; nickname: string }[]>([])
  const [chatInput, setChatInput] = useState('')
  const [unread, setUnread] = useState(0)
  const [leaving, setLeaving] = useState(false)

  const stompRef = useRef<ReturnType<typeof createRoomStompClient> | null>(null)
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
      onTranscript: (payload) => setTranscripts((prev) => [...prev.slice(-99), payload]),
      onInsight: setInsight,
      onRecommendations: (payload) => setRecommendations(payload.documents),
    })
    stompRef.current = client
    return () => client.deactivate()
  }, [roomId])

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

  return (
    <div className="flex h-full flex-col bg-video">
      <div className="flex min-h-0 flex-1">
        {/* 비디오 그리드 */}
        <div className="min-w-0 flex-1 p-4">
          <GridLayout tracks={tracks} className="h-full">
            <ParticipantTile />
          </GridLayout>
        </div>

        {/* 사이드 패널 */}
        {panelOpen && (
          <aside className="flex w-[340px] flex-col border-l border-white/10 bg-[#131a3a]">
            <div className="flex border-b border-white/10">
              {(
                [
                  ['ai', 'AI 어시스턴트'],
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
                <AiPanel transcripts={transcripts} insight={insight} recommendations={recommendations} />
              )}
              {tab === 'chat' && (
                <ChatPanel messages={chatMessages} myUserId={user?.userId} />
              )}
              {tab === 'people' && <PeoplePanel participants={participants} />}
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
  activeStyle?: 'device' | 'share'
}) {
  const activeClass =
    activeStyle === 'share'
      ? on
        ? 'bg-[#5276df] text-white'
        : 'bg-white/10 text-white hover:bg-white/20'
      : on
        ? 'bg-white/10 text-white hover:bg-white/20'
        : 'bg-danger/20 text-danger'
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

function ShareIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="3" y="4" width="18" height="13" rx="2" />
      <path d="M12 21v-4M12 13V8m0 0-3 3m3-3 3 3" />
    </svg>
  )
}

function AiPanel({
  transcripts,
  insight,
  recommendations,
}: {
  transcripts: TranscriptPayload[]
  insight: InsightPayload | null
  recommendations: RecommendedDocument[]
}) {
  const transcriptEndRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [transcripts.length])

  return (
    <div className="space-y-5 p-4">
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
        <PanelHeading live={!!insight}>회의 인사이트</PanelHeading>
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

      {/* 실시간 자막 */}
      <section>
        <PanelHeading live={transcripts.length > 0}>실시간 자막</PanelHeading>
        {transcripts.length === 0 ? (
          <p className="text-[12px] leading-relaxed text-white/40">
            마이크를 켜고 말하면 잠시 후 자막이 이곳에 표시됩니다.
          </p>
        ) : (
          <ul className="space-y-2">
            {transcripts.map((t) => (
              <li key={t.audioId} className="text-[12.5px] leading-relaxed">
                <span className="font-bold text-[#8ea5f8]">{t.speakerName}</span>{' '}
                <span className="text-white/80">{t.text}</span>
              </li>
            ))}
            <div ref={transcriptEndRef} />
          </ul>
        )}
      </section>
    </div>
  )
}

function PanelHeading({ children, live }: { children: React.ReactNode; live?: boolean }) {
  return (
    <h3 className="mb-2 flex items-center gap-2 text-[12px] font-extrabold uppercase tracking-wide text-white/60">
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

function PeoplePanel({ participants }: { participants: { userId: number; nickname: string }[] }) {
  return (
    <ul className="space-y-1 p-4">
      {participants.map((p) => (
        <li key={p.userId} className="flex items-center gap-3 rounded-[12px] px-2 py-2 hover:bg-white/5">
          <span className="flex h-9 w-9 items-center justify-center rounded-full bg-[#5276df]/40 text-[13px] font-black text-white">
            {p.nickname.charAt(0)}
          </span>
          <span className="text-[13px] font-bold text-white/85">{p.nickname}</span>
        </li>
      ))}
    </ul>
  )
}
