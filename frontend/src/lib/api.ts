const BASE_URL = '/api'

let accessToken: string | null = localStorage.getItem('accessToken')

export function setAccessToken(token: string | null) {
  accessToken = token
  if (token) {
    localStorage.setItem('accessToken', token)
  } else {
    localStorage.removeItem('accessToken')
  }
}

export function getAccessToken() {
  return accessToken
}

export class ApiError extends Error {
  status: number
  errorCode?: string

  constructor(status: number, message: string, errorCode?: string) {
    super(message)
    this.status = status
    this.errorCode = errorCode
  }
}

const ERROR_MESSAGES: Record<string, string> = {
  '40110': '이메일 또는 비밀번호가 올바르지 않습니다.',
  '40111': '로그인이 만료되었습니다. 다시 로그인해주세요.',
  '40310': '방장만 할 수 있는 작업입니다.',
  '40311': '회의 참가자만 이용할 수 있습니다.',
  '40411': '존재하지 않는 회의입니다.',
  '40412': '아직 회의록이 생성되지 않았습니다.',
  '40910': '이미 종료된 회의입니다.',
  '40911': '이미 사용 중인 이메일입니다.',
  '40912': '이미 사용 중인 닉네임입니다.',
}

async function tryReissue(): Promise<boolean> {
  try {
    const res = await fetch(`${BASE_URL}/auth/reissue`, {
      method: 'POST',
      credentials: 'include',
    })
    if (!res.ok) return false
    const data = await res.json()
    setAccessToken(data.accessToken)
    return true
  } catch {
    return false
  }
}

export async function api<T = unknown>(
  path: string,
  options: RequestInit & { skipAuth?: boolean; retried?: boolean } = {},
): Promise<T> {
  const { skipAuth, retried, ...init } = options
  const headers = new Headers(init.headers)

  if (!(init.body instanceof FormData) && init.body != null) {
    headers.set('Content-Type', 'application/json')
  }
  if (!skipAuth && accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  })

  if (res.status === 401 && !skipAuth && !retried) {
    const reissued = await tryReissue()
    if (reissued) {
      return api<T>(path, { ...options, retried: true })
    }
    setAccessToken(null)
    window.dispatchEvent(new CustomEvent('auth:expired'))
    throw new ApiError(401, '로그인이 필요합니다.')
  }

  if (!res.ok) {
    let errorCode: string | undefined
    let message = `요청에 실패했습니다. (${res.status})`
    try {
      const body = await res.json()
      errorCode = body.errorCode
      message = (errorCode && ERROR_MESSAGES[errorCode]) || body.message || message
    } catch {
      /* body 없음 */
    }
    throw new ApiError(res.status, message, errorCode)
  }

  if (res.status === 204) {
    return undefined as T
  }

  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}
