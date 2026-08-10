import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { Button, Card, Input, Logo } from '../components/ui'

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuth((s) => s.login)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const redirect = searchParams.get('redirect') ?? '/dashboard'

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email, password)
      navigate(redirect, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-surface px-6">
      <Card className="w-full max-w-[400px] p-8">
        <div className="mb-8 flex justify-center">
          <Logo />
        </div>
        <h1 className="mb-6 text-center text-xl font-extrabold text-ink">다시 만나서 반가워요</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="이메일"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
          <Input
            label="비밀번호"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="current-password"
            required
          />
          {error && <p className="text-[13px] font-semibold text-danger">{error}</p>}
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? '로그인 중…' : '로그인'}
          </Button>
        </form>
        <p className="mt-6 text-center text-[13px] text-muted">
          아직 계정이 없나요?{' '}
          <Link
            to={`/signup${redirect !== '/dashboard' ? `?redirect=${encodeURIComponent(redirect)}` : ''}`}
            className="font-bold text-primary hover:underline"
          >
            회원가입
          </Link>
        </p>
      </Card>
    </div>
  )
}
