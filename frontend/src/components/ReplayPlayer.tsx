import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { roomsApi } from '../lib/rooms'
import { Card } from './ui'

const RATES = [1, 1.5, 2] as const

/**
 * 회의 다시듣기 플레이어.
 * 별도 녹화 파일 없이, STT용으로 저장된 발화 청크를 시간순으로 이어 재생한다
 * (무음 구간은 자연스럽게 건너뛰는 팟캐스트식 압축 재생).
 */
export default function ReplayPlayer({ roomId }: { roomId: number }) {
  const { data } = useQuery({
    queryKey: ['replay', roomId],
    queryFn: () => roomsApi.replay(roomId),
    staleTime: 60 * 60 * 1000,
  })

  const audioRef = useRef<HTMLAudioElement>(null)
  const [index, setIndex] = useState(0)
  const [playing, setPlaying] = useState(false)
  const [rate, setRate] = useState<(typeof RATES)[number]>(1)
  const [segmentTime, setSegmentTime] = useState(0)

  const segments = useMemo(() => data?.segments ?? [], [data])
  const chapters = data?.chapters ?? []

  // 세그먼트별 누적 시작 시간 (압축 타임라인)
  const cumulative = useMemo(() => {
    const acc: number[] = []
    let sum = 0
    for (const segment of segments) {
      acc.push(sum)
      sum += segment.duration
    }
    return { starts: acc, total: sum }
  }, [segments])

  const current = segments[index]

  useEffect(() => {
    const audio = audioRef.current
    if (!audio || !current) return
    audio.src = current.url
    audio.playbackRate = rate
    setSegmentTime(0)
    if (playing) {
      void audio.play().catch(() => setPlaying(false))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, current?.url])

  useEffect(() => {
    if (audioRef.current) audioRef.current.playbackRate = rate
  }, [rate])

  if (!data || segments.length === 0) {
    return null
  }

  const togglePlay = () => {
    const audio = audioRef.current
    if (!audio) return
    if (playing) {
      audio.pause()
      setPlaying(false)
    } else {
      void audio
        .play()
        .then(() => setPlaying(true))
        .catch(() => setPlaying(false))
    }
  }

  const jumpTo = (segmentIndex: number) => {
    setIndex(Math.min(segmentIndex, segments.length - 1))
    setPlaying(true)
  }

  const onEnded = () => {
    if (index < segments.length - 1) {
      setIndex((i) => i + 1)
    } else {
      setPlaying(false)
    }
  }

  const elapsed = (cumulative.starts[index] ?? 0) + segmentTime
  const currentChapterIndex = chapters.reduce(
    (acc, chapter, i) => (chapter.segmentIndex <= index ? i : acc),
    -1,
  )

  return (
    <Card className="mb-6 p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-[15px] font-extrabold text-ink">🎧 회의 다시듣기</h2>
        <span className="font-mono text-[11.5px] text-faint">
          발화 {segments.length}개 · 총 {formatTime(cumulative.total)}
        </span>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1fr_260px]">
        {/* 플레이어 */}
        <div>
          <div className="flex items-center gap-4">
            <button
              onClick={togglePlay}
              className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary text-white transition-colors hover:bg-primary-hover"
              title={playing ? '일시정지' : '재생'}
            >
              {playing ? <PauseIcon /> : <PlayIcon />}
            </button>
            <div className="min-w-0 flex-1">
              <div className="flex justify-between font-mono text-[11px] text-muted">
                <span>{formatTime(elapsed)}</span>
                <span>{formatTime(cumulative.total)}</span>
              </div>
              {/* 진행 바 (세그먼트 클릭 점프) */}
              <div className="mt-1 flex h-2 gap-[2px] overflow-hidden rounded-full">
                {segments.map((segment, i) => (
                  <button
                    key={segment.audioId}
                    onClick={() => jumpTo(i)}
                    title={`${segment.speakerName} · ${formatTime(cumulative.starts[i])}`}
                    className={`h-full transition-colors ${
                      i < index ? 'bg-primary' : i === index ? 'bg-primary-deep' : 'bg-line'
                    } hover:bg-primary-hover`}
                    style={{ width: `${(segment.duration / cumulative.total) * 100}%` }}
                  />
                ))}
              </div>
            </div>
            <div className="flex shrink-0 gap-1">
              {RATES.map((r) => (
                <button
                  key={r}
                  onClick={() => setRate(r)}
                  className={`rounded-full px-2.5 py-1 font-mono text-[11px] font-bold transition-colors ${
                    rate === r ? 'bg-primary text-white' : 'bg-surface text-muted hover:text-ink'
                  }`}
                >
                  {r}x
                </button>
              ))}
            </div>
          </div>

          {/* 현재 발화 */}
          {current && (
            <div className="mt-4 rounded-[12px] bg-surface p-4">
              <p className="text-[12px] font-bold text-primary">
                {current.speakerName}
                {current.startTime && (
                  <span className="ml-2 font-mono text-[10.5px] font-normal text-faint">
                    {new Date(current.startTime).toLocaleTimeString('ko-KR')}
                  </span>
                )}
              </p>
              <p className="mt-1 text-[13.5px] leading-relaxed text-ink/85">
                {current.text ?? '(전사되지 않은 구간)'}
              </p>
            </div>
          )}

          <audio
            ref={audioRef}
            onEnded={onEnded}
            onTimeUpdate={(e) => setSegmentTime(e.currentTarget.currentTime)}
            className="hidden"
          />
        </div>

        {/* AI 챕터 */}
        <aside>
          <h3 className="mb-2 text-[12px] font-extrabold uppercase tracking-wide text-muted">AI 챕터</h3>
          {chapters.length === 0 ? (
            <p className="text-[12.5px] text-faint">
              {data.roomStatus === 'ENDED'
                ? '챕터를 만들 발화 기록이 부족해요.'
                : '회의가 끝나면 챕터가 생성됩니다.'}
            </p>
          ) : (
            <ol className="space-y-1">
              {chapters.map((chapter, i) => (
                <li key={i}>
                  <button
                    onClick={() => jumpTo(chapter.segmentIndex)}
                    className={`flex w-full items-baseline gap-2 rounded-[10px] px-2.5 py-2 text-left transition-colors hover:bg-primary-soft ${
                      i === currentChapterIndex ? 'bg-primary-soft' : ''
                    }`}
                  >
                    <span className="shrink-0 font-mono text-[10.5px] text-faint">
                      {formatTime(cumulative.starts[chapter.segmentIndex] ?? 0)}
                    </span>
                    <span
                      className={`text-[13px] leading-snug ${
                        i === currentChapterIndex ? 'font-extrabold text-primary-deep' : 'font-semibold text-ink'
                      }`}
                    >
                      {chapter.title}
                    </span>
                  </button>
                </li>
              ))}
            </ol>
          )}
        </aside>
      </div>
    </Card>
  )
}

function formatTime(totalSeconds: number) {
  const m = Math.floor(totalSeconds / 60)
  const s = Math.floor(totalSeconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

function PlayIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
      <path d="M8 5.5v13l11-6.5z" />
    </svg>
  )
}

function PauseIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
      <rect x="6" y="5" width="4" height="14" rx="1" />
      <rect x="14" y="5" width="4" height="14" rx="1" />
    </svg>
  )
}
