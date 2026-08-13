import { Link } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { Logo } from '../components/ui'

const FEATURES = [
  {
    title: '실시간 AI 자막',
    body: '발화가 끝나면 몇 초 안에 자막이 쌓입니다. 회의가 끝나면 그대로 회의록의 재료가 됩니다.',
  },
  {
    title: '지식 위키 문서 추천',
    body: '팀 위키에 올려둔 문서를 임베딩해두고, 지금 나누는 대화와 관련된 문서를 회의 중에 바로 추천합니다.',
  },
  {
    title: '롤링 요약 · 액션아이템',
    body: '회의가 길어져도 괜찮습니다. AI가 지금까지의 흐름을 계속 요약하고 할 일과 미해결 쟁점을 추립니다.',
  },
  {
    title: '자동 회의록',
    body: '회의가 끝나면 발언자별 요약, 결정사항, 액션아이템이 정리된 회의록이 자동으로 생성됩니다.',
  },
]

export default function LandingPage() {
  const user = useAuth((s) => s.user)

  return (
    <div className="min-h-full bg-canvas">
      <header className="border-b border-line">
        <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between px-6">
          <Logo />
          <div className="flex items-center gap-2">
            {user ? (
              <Link
                to="/dashboard"
                className="rounded-full bg-primary px-5 py-2 text-sm font-bold text-white hover:bg-primary-hover"
              >
                대시보드로
              </Link>
            ) : (
              <>
                <Link to="/login" className="rounded-full px-4 py-2 text-sm font-bold text-muted hover:text-ink">
                  로그인
                </Link>
                <Link
                  to="/signup"
                  className="rounded-full bg-primary px-5 py-2 text-sm font-bold text-white hover:bg-primary-hover"
                >
                  시작하기
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section
        className="relative overflow-hidden"
        style={{
          backgroundImage:
            'linear-gradient(rgba(82,118,223,.055) 1px, transparent 1px), linear-gradient(90deg, rgba(82,118,223,.055) 1px, transparent 1px)',
          backgroundSize: '96px 96px',
        }}
      >
        <div className="mx-auto max-w-[1200px] px-6 py-24 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-primary-pale bg-primary-soft px-4 py-1.5 text-[13px] font-bold text-primary-deep">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-primary" />
            AI가 함께 앉아있는 화상회의
          </span>
          <h1 className="mx-auto mt-6 max-w-[720px] text-[44px] font-black leading-[1.15] tracking-tight text-ink-strong">
            회의는 사람이,
            <br />
            기록과 맥락은 <span className="text-primary">AI</span>가.
          </h1>
          <p className="mx-auto mt-5 max-w-[560px] text-[16px] leading-relaxed text-muted">
            초대코드 하나로 모이고, 말하는 동안 자막·요약·관련 문서가 쌓이고,
            끝나면 회의록이 도착합니다.
          </p>
          <div className="mt-9 flex items-center justify-center gap-3">
            <Link
              to={user ? '/dashboard' : '/signup'}
              className="rounded-full bg-primary px-7 py-3.5 text-[15px] font-bold text-white shadow-[var(--shadow-active)] transition-all hover:-translate-y-0.5 hover:bg-primary-hover hover:shadow-[0_10px_26px_rgba(82,118,223,0.28)] active:translate-y-0"
            >
              무료로 회의 시작하기
            </Link>
            <Link
              to="/login"
              className="rounded-full border border-line-strong bg-card px-7 py-3.5 text-[15px] font-bold text-ink transition-all hover:-translate-y-0.5 hover:border-primary hover:text-primary active:translate-y-0"
            >
              초대코드로 참여
            </Link>
          </div>
        </div>
      </section>

      <section className="border-t border-line bg-canvas">
        <div className="mx-auto max-w-[1200px] px-6 py-20">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {FEATURES.map((feature, i) => (
              <div
                key={feature.title}
                className="group rounded-[16px] border border-line bg-card p-6 transition-all hover:-translate-y-1 hover:border-primary-pale hover:shadow-[var(--shadow-active)]"
              >
                <span className="font-mono text-[12px] font-bold text-faint transition-colors group-hover:text-primary">
                  0{i + 1}
                </span>
                <h3 className="mt-3 text-[16px] font-extrabold text-ink">{feature.title}</h3>
                <p className="mt-2 text-[13.5px] leading-relaxed text-muted">{feature.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-line bg-card">
        <div className="mx-auto flex max-w-[1200px] items-center justify-between px-6 py-6 text-[13px] text-faint">
          <span>© 2026 Meetiny</span>
          <span className="font-mono">local dev build</span>
        </div>
      </footer>
    </div>
  )
}
