import ReactMarkdown from 'react-markdown'

/**
 * LLM 회의록([섹션] 구조)을 실제 보고서 문서처럼 렌더링한다.
 * 구조 파싱에 실패하면 마크다운 렌더링으로 폴백.
 */

interface ParsedSections {
  [key: string]: string[]
}

const KNOWN_SECTIONS = ['회의명', '회의 주제', '전체 요약', '발언자별 요약', '주요 결정사항', '액션 아이템', '미해결 쟁점']

function parseSections(content: string): ParsedSections | null {
  const sections: ParsedSections = {}
  let current: string | null = null

  for (const rawLine of content.split('\n')) {
    const headerMatch = rawLine.match(/^\s*\[(.+?)\]\s*$/)
    if (headerMatch) {
      current = headerMatch[1].trim()
      sections[current] = []
      continue
    }
    if (current && rawLine.trim()) {
      sections[current].push(rawLine.trim())
    }
  }

  const matched = KNOWN_SECTIONS.filter((key) => key in sections)
  return matched.length >= 3 ? sections : null
}

function stripBullet(line: string) {
  return line.replace(/^[-*•]\s*/, '').trim()
}

function isEmptyMarker(lines: string[]) {
  if (lines.length === 0) return true
  const joined = lines.map(stripBullet).join('')
  return joined === '없음' || joined === '확인 필요' || joined === ''
}

/** "이름: 내용" 형태 분해 */
function splitSpeaker(line: string): { name: string; body: string } | null {
  const cleaned = stripBullet(line)
  const idx = cleaned.indexOf(':')
  if (idx <= 0 || idx > 20) return null
  return { name: cleaned.slice(0, idx).trim(), body: cleaned.slice(idx + 1).trim() }
}

/** "담당자 | 할 일 | 기한" 분해 */
function splitActionItem(line: string): { assignee: string; task: string; due: string } | null {
  const parts = stripBullet(line)
    .split('|')
    .map((part) => part.trim())
  if (parts.length < 2) return null
  return { assignee: parts[0], task: parts[1] ?? '', due: parts[2] ?? '미정' }
}

export default function ReportDocument({
  title,
  content,
  createdTime,
}: {
  title: string
  content: string
  createdTime: string
}) {
  const sections = parseSections(content)

  if (!sections) {
    return (
      <article className="prose-report">
        <ReactMarkdown>{content}</ReactMarkdown>
      </article>
    )
  }

  const meetingName = sections['회의명']?.map(stripBullet).join(' ') || title || '회의록'
  const topic = sections['회의 주제']?.map(stripBullet).join(' ')
  const summary = sections['전체 요약'] ?? []
  const speakers = (sections['발언자별 요약'] ?? []).map(splitSpeaker).filter(Boolean) as {
    name: string
    body: string
  }[]
  const decisions = sections['주요 결정사항'] ?? []
  const actionItems = (sections['액션 아이템'] ?? []).map(splitActionItem).filter(Boolean) as {
    assignee: string
    task: string
    due: string
  }[]
  const openIssues = sections['미해결 쟁점'] ?? []

  return (
    <article className="report-doc">
      {/* 표지 헤더 */}
      <header className="rounded-[14px] p-7 text-white" style={{ background: 'linear-gradient(120deg,#5e7ff0,#20308a)' }}>
        <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-white/60">Meetiny AI 회의록</p>
        <h1 className="mt-2 text-[24px] font-black leading-snug">{meetingName}</h1>
        {topic && !isEmptyMarker(sections['회의 주제'] ?? []) && (
          <p className="mt-1.5 text-[13.5px] text-white/80">{topic}</p>
        )}
        <p className="mt-4 font-mono text-[11.5px] text-white/55">
          {new Date(createdTime).toLocaleString('ko-KR', { dateStyle: 'full', timeStyle: 'short' })}
        </p>
      </header>

      {/* 전체 요약 */}
      {!isEmptyMarker(summary) && (
        <section className="mt-6">
          <SectionLabel>전체 요약</SectionLabel>
          <div className="rounded-[12px] border-l-[3px] border-primary bg-primary-soft/60 p-5 text-[14px] leading-[1.85] text-ink">
            {summary.map((line, i) => (
              <p key={i} className={i > 0 ? 'mt-2' : ''}>
                {stripBullet(line)}
              </p>
            ))}
          </div>
        </section>
      )}

      {/* 발언자별 요약 */}
      {speakers.length > 0 && (
        <section className="mt-7">
          <SectionLabel>발언자별 요약</SectionLabel>
          <div className="grid gap-3 sm:grid-cols-2">
            {speakers.map((speaker, i) => (
              <div key={i} className="rounded-[12px] border border-line bg-card p-4">
                <p className="flex items-center gap-2 text-[13px] font-extrabold text-primary-deep">
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary-soft text-[11px]">
                    {speaker.name.charAt(0)}
                  </span>
                  {speaker.name}
                </p>
                <p className="mt-2 text-[13px] leading-relaxed text-ink/85">{speaker.body}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 주요 결정사항 */}
      {!isEmptyMarker(decisions) && (
        <section className="mt-7">
          <SectionLabel>주요 결정사항</SectionLabel>
          <ul className="space-y-2">
            {decisions.map((decision, i) => (
              <li key={i} className="flex items-start gap-2.5 rounded-[10px] bg-surface px-4 py-3 text-[13.5px] leading-relaxed text-ink">
                <span className="mt-0.5 flex h-4.5 w-4.5 shrink-0 items-center justify-center rounded-full bg-success text-[10px] font-black text-white">
                  ✓
                </span>
                {stripBullet(decision)}
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* 액션 아이템 */}
      {actionItems.length > 0 && (
        <section className="mt-7">
          <SectionLabel>액션 아이템</SectionLabel>
          <div className="overflow-x-auto rounded-[12px] border border-line">
            <table className="w-full text-[13px]">
              <thead>
                <tr className="bg-surface text-left text-[12px] text-muted">
                  <th className="px-4 py-2.5 font-bold">담당자</th>
                  <th className="px-4 py-2.5 font-bold">할 일</th>
                  <th className="px-4 py-2.5 font-bold">기한</th>
                </tr>
              </thead>
              <tbody>
                {actionItems.map((item, i) => (
                  <tr key={i} className="border-t border-line">
                    <td className="whitespace-nowrap px-4 py-3 font-bold text-primary-deep">{item.assignee}</td>
                    <td className="px-4 py-3 leading-relaxed text-ink">{item.task}</td>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-[12px] text-muted">{item.due}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* 미해결 쟁점 */}
      {!isEmptyMarker(openIssues) && (
        <section className="mt-7">
          <SectionLabel>미해결 쟁점</SectionLabel>
          <ul className="space-y-2">
            {openIssues.map((issue, i) => (
              <li
                key={i}
                className="flex items-start gap-2.5 rounded-[10px] border border-[#f0d9a8] bg-[#fdf6e7] px-4 py-3 text-[13.5px] leading-relaxed text-[#7a5c1e]"
              >
                <span className="mt-0.5 font-black">?</span>
                {stripBullet(issue)}
              </li>
            ))}
          </ul>
        </section>
      )}

      <footer className="mt-8 border-t border-line pt-4 text-center text-[11.5px] text-faint">
        이 문서는 Meetiny AI가 회의 발화 기록을 바탕으로 자동 생성했습니다.
      </footer>
    </article>
  )
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="mb-3 flex items-center gap-2 text-[13px] font-extrabold uppercase tracking-wide text-muted">
      <span className="h-3.5 w-1 rounded-full bg-primary" />
      {children}
    </h2>
  )
}
