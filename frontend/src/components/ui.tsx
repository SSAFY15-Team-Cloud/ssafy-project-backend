import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/auth'

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' | 'danger' | 'outline' }) {
  const base =
    'inline-flex items-center justify-center gap-2 rounded-full px-5 py-2.5 text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50'
  const variants = {
    primary: 'bg-primary text-white hover:bg-primary-hover',
    ghost: 'text-ink hover:bg-surface',
    outline: 'border border-line-strong text-ink hover:border-primary hover:text-primary',
    danger: 'bg-danger text-white hover:opacity-90',
  }
  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />
}

export function Input({
  label,
  error,
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label?: string; error?: string }) {
  return (
    <label className="block">
      {label && <span className="mb-1.5 block text-[13px] font-bold text-ink">{label}</span>}
      <input
        className={`w-full rounded-[12px] border border-line bg-white px-4 py-3 text-sm text-ink outline-none transition-colors placeholder:text-faint focus:border-primary ${className}`}
        {...props}
      />
      {error && <span className="mt-1 block text-xs font-semibold text-danger">{error}</span>}
    </label>
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-[16px] border border-line bg-white shadow-[var(--shadow-card)] ${className}`}>
      {children}
    </div>
  )
}

export function Logo({ dark = false }: { dark?: boolean }) {
  return (
    <Link to="/" className="flex items-center gap-2">
      <svg width="26" height="26" viewBox="0 0 512 512" aria-hidden>
        <defs>
          <linearGradient id="logo-bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#5e7ff0" />
            <stop offset="1" stopColor="#20308a" />
          </linearGradient>
          <radialGradient id="logo-orb" cx="0.35" cy="0.28" r="1">
            <stop offset="0" stopColor="#eef1ff" />
            <stop offset="0.45" stopColor="#a5b4fc" />
            <stop offset="1" stopColor="#4338ca" />
          </radialGradient>
        </defs>
        <rect width="512" height="512" rx="116" fill="url(#logo-bg)" />
        <rect x="96" y="176" width="220" height="176" rx="48" fill="#ffffff" />
        <path d="M312 240 L408 186 v156 L312 288 z" fill="#c3d0ff" />
        <circle cx="164" cy="232" r="18" fill="#5e7ff0" />
        <circle cx="376" cy="150" r="66" fill="url(#logo-orb)" />
        <circle cx="376" cy="150" r="66" fill="none" stroke="#ffffff" strokeOpacity="0.35" strokeWidth="6" />
      </svg>
      <span className={`text-[17px] font-extrabold tracking-tight ${dark ? 'text-white' : 'text-ink'}`}>
        Meetiny
      </span>
    </Link>
  )
}

export function AppHeader() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()

  const navItems = [
    ['/dashboard', '대시보드'],
    ['/knowledge', '지식 위키'],
    ['/search', '검색'],
  ] as const

  return (
    <header className="sticky top-0 z-20 border-b border-line bg-white/85 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-[1200px] items-center justify-between px-6">
        <Logo />
        <nav className="flex items-center gap-1 text-sm font-semibold text-muted">
          {navItems.map(([to, label]) => (
            <Link
              key={to}
              to={to}
              className={`rounded-full px-3 py-1.5 transition-colors ${
                pathname.startsWith(to)
                  ? 'bg-primary-soft font-bold text-primary-deep'
                  : 'hover:bg-surface hover:text-ink'
              }`}
            >
              {label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-3">
          {user ? (
            <>
              <span className="hidden text-sm font-bold text-ink sm:block">{user.nickname}</span>
              <button
                onClick={() => void logout()}
                className="rounded-full border border-line-strong px-4 py-1.5 text-[13px] font-bold text-muted hover:border-danger hover:text-danger"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="rounded-full bg-primary px-4 py-1.5 text-[13px] font-bold text-white hover:bg-primary-hover"
            >
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}

export function PageShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-full flex-col bg-canvas">
      <AppHeader />
      <main className="mx-auto w-full max-w-[1200px] flex-1 px-6 py-8">{children}</main>
    </div>
  )
}

export function SectionTitle({ children, sub }: { children: ReactNode; sub?: string }) {
  return (
    <div className="mb-5">
      <h2 className="text-xl font-extrabold text-ink">{children}</h2>
      {sub && <p className="mt-1 text-sm text-muted">{sub}</p>}
    </div>
  )
}
