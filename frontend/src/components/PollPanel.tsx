import { useState } from 'react'
import { roomsApi } from '../lib/rooms'
import type { Poll } from '../lib/rooms'

/**
 * 회의 중 실시간 투표 패널 (회의실 사이드 패널 탭).
 * 결과는 STOMP 브로드캐스트로 모든 참가자에게 실시간 반영된다.
 */
export default function PollPanel({
  roomId,
  myUserId,
  polls,
  onLocalUpdate,
}: {
  roomId: number
  myUserId?: number
  polls: Poll[]
  onLocalUpdate: (poll: Poll) => void
}) {
  const [creating, setCreating] = useState(false)
  const [question, setQuestion] = useState('')
  const [options, setOptions] = useState(['', ''])
  const [busy, setBusy] = useState(false)

  const resetForm = () => {
    setCreating(false)
    setQuestion('')
    setOptions(['', ''])
  }

  const submit = async () => {
    const trimmedQuestion = question.trim()
    const trimmedOptions = options.map((option) => option.trim()).filter(Boolean)
    if (!trimmedQuestion || trimmedOptions.length < 2 || busy) return
    setBusy(true)
    try {
      const created = await roomsApi.createPoll(roomId, trimmedQuestion, trimmedOptions)
      onLocalUpdate(created)
      resetForm()
    } catch {
      /* ignore */
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-4 p-4">
      {!creating ? (
        <button
          onClick={() => setCreating(true)}
          className="w-full rounded-full border border-dashed border-white/25 py-2.5 text-[12.5px] font-bold text-white/60 transition-colors hover:border-[#8ea5f8] hover:text-white"
        >
          + 새 투표 만들기
        </button>
      ) : (
        <div className="rounded-[12px] bg-white/5 p-3.5">
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="질문 (예: 다음 회의 요일은?)"
            maxLength={200}
            autoFocus
            className="w-full rounded-[9px] bg-white/10 px-3 py-2 text-[13px] font-bold text-white outline-none placeholder:text-white/35 focus:bg-white/15"
          />
          <div className="mt-2 space-y-1.5">
            {options.map((option, i) => (
              <input
                key={i}
                value={option}
                onChange={(e) => setOptions(options.map((prev, j) => (j === i ? e.target.value : prev)))}
                placeholder={`선택지 ${i + 1}`}
                maxLength={100}
                className="w-full rounded-[9px] bg-white/10 px-3 py-1.5 text-[12.5px] text-white outline-none placeholder:text-white/35 focus:bg-white/15"
              />
            ))}
          </div>
          <div className="mt-2.5 flex items-center gap-2">
            {options.length < 6 && (
              <button
                onClick={() => setOptions([...options, ''])}
                className="text-[11.5px] font-bold text-white/50 hover:text-white"
              >
                + 선택지 추가
              </button>
            )}
            <div className="ml-auto flex gap-1.5">
              <button
                onClick={resetForm}
                className="rounded-full px-3 py-1.5 text-[11.5px] font-bold text-white/50 hover:text-white"
              >
                취소
              </button>
              <button
                onClick={() => void submit()}
                disabled={busy || !question.trim() || options.filter((o) => o.trim()).length < 2}
                className="rounded-full bg-[#5276df] px-4 py-1.5 text-[11.5px] font-bold text-white hover:bg-[#4265c9] disabled:opacity-40"
              >
                {busy ? '만드는 중…' : '투표 시작'}
              </button>
            </div>
          </div>
        </div>
      )}

      {polls.length === 0 && !creating && (
        <p className="text-[12px] leading-relaxed text-white/40">
          아직 투표가 없어요. 회의 중 빠르게 의견을 모아보세요.
        </p>
      )}

      {polls.map((poll) => (
        <PollCard key={poll.pollId} poll={poll} myUserId={myUserId} onLocalUpdate={onLocalUpdate} />
      ))}
    </div>
  )
}

function PollCard({
  poll,
  myUserId,
  onLocalUpdate,
}: {
  poll: Poll
  myUserId?: number
  onLocalUpdate: (poll: Poll) => void
}) {
  const [voting, setVoting] = useState(false)

  const vote = async (index: number) => {
    if (poll.closed || voting) return
    setVoting(true)
    try {
      const updated = await roomsApi.votePoll(poll.pollId, index)
      onLocalUpdate(updated)
    } catch {
      /* ignore */
    } finally {
      setVoting(false)
    }
  }

  const close = async () => {
    try {
      onLocalUpdate(await roomsApi.closePoll(poll.pollId))
    } catch {
      /* ignore */
    }
  }

  return (
    <div className="rounded-[12px] bg-white/5 p-3.5">
      <div className="flex items-start justify-between gap-2">
        <p className="text-[13px] font-bold leading-snug text-white">{poll.question}</p>
        {poll.closed && (
          <span className="shrink-0 rounded-full bg-white/10 px-2 py-0.5 text-[10px] font-bold text-white/60">
            마감
          </span>
        )}
      </div>
      <div className="mt-2.5 space-y-1.5">
        {poll.options.map((option, i) => {
          const count = poll.counts[i] ?? 0
          const pct = poll.totalVotes > 0 ? Math.round((count / poll.totalVotes) * 100) : 0
          const mine = poll.myVote === i
          return (
            <button
              key={i}
              onClick={() => void vote(i)}
              disabled={poll.closed}
              className={`relative w-full overflow-hidden rounded-[9px] border px-3 py-2 text-left transition-colors ${
                mine ? 'border-[#8ea5f8]' : 'border-white/10 hover:border-white/30'
              } disabled:cursor-default`}
            >
              <span
                className="absolute inset-y-0 left-0 bg-[#5276df]/30 transition-all duration-500"
                style={{ width: `${pct}%` }}
              />
              <span className="relative flex items-center justify-between gap-2 text-[12.5px]">
                <span className={mine ? 'font-bold text-white' : 'text-white/80'}>
                  {mine && '✓ '}
                  {option}
                </span>
                <span className="shrink-0 font-mono text-[11px] text-white/50">
                  {count}표 · {pct}%
                </span>
              </span>
            </button>
          )
        })}
      </div>
      <div className="mt-2 flex items-center justify-between">
        {!poll.closed && myUserId === poll.creatorId ? (
          <button
            onClick={() => void close()}
            className="text-[11px] font-bold text-white/45 hover:text-danger"
          >
            투표 마감
          </button>
        ) : (
          <span />
        )}
        <p className="font-mono text-[10.5px] text-white/35">총 {poll.totalVotes}명 참여</p>
      </div>
    </div>
  )
}
