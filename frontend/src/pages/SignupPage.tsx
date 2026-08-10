import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { Button, Card, Input, Logo } from '../components/ui'

export default function SignupPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { signup, login } = useAuth()

  const [form, setForm] = useState({ email: '', password: '', nickname: '', name: '' })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const redirect = searchParams.get('redirect') ?? '/dashboard'
  const update = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await signup(form)
      await login(form.email, form.password)
      navigate(redirect, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-surface px-6 py-10">
      <Card className="w-full max-w-[400px] p-8">
        <div className="mb-8 flex justify-center">
          <Logo />
        </div>
        <h1 className="mb-6 text-center text-xl font-extrabold text-ink">몇 초면 끝나요</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="이메일" type="email" value={form.email} onChange={update('email')} required />
          <Input
            label="비밀번호"
            type="password"
            value={form.password}
            onChange={update('password')}
            placeholder="8자 이상"
            minLength={8}
            required
          />
          <Input
            label="닉네임"
            value={form.nickname}
            onChange={update('nickname')}
            placeholder="회의에서 보일 이름 (2~15자)"
            minLength={2}
            maxLength={15}
            required
          />
          <Input label="이름" value={form.name} onChange={update('name')} maxLength={15} required />
          {error && <p className="text-[13px] font-semibold text-danger">{error}</p>}
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? '가입 중…' : '회원가입'}
          </Button>
        </form>
        <p className="mt-6 text-center text-[13px] text-muted">
          이미 계정이 있나요?{' '}
          <Link to="/login" className="font-bold text-primary hover:underline">
            로그인
          </Link>
        </p>
      </Card>
    </div>
  )
}
