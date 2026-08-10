import { create } from 'zustand'
import { api, setAccessToken, getAccessToken } from './api'

export interface MyInfo {
  userId: number
  email: string
  nickname: string
  name: string
  profileImageUrl: string
}

interface AuthState {
  user: MyInfo | null
  initialized: boolean
  login: (email: string, password: string) => Promise<void>
  signup: (payload: { email: string; password: string; nickname: string; name: string }) => Promise<void>
  logout: () => Promise<void>
  loadMe: () => Promise<void>
  clear: () => void
}

export const useAuth = create<AuthState>((set, get) => ({
  user: null,
  initialized: false,

  async login(email, password) {
    const res = await api<{ accessToken: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
      skipAuth: true,
    })
    setAccessToken(res.accessToken)
    await get().loadMe()
  },

  async signup(payload) {
    await api('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(payload),
      skipAuth: true,
    })
  },

  async logout() {
    try {
      await api('/auth/logout', { method: 'POST' })
    } catch {
      /* 이미 만료된 경우 무시 */
    }
    setAccessToken(null)
    set({ user: null })
  },

  async loadMe() {
    if (!getAccessToken()) {
      set({ user: null, initialized: true })
      return
    }
    try {
      const me = await api<MyInfo>('/users/me')
      set({ user: me, initialized: true })
    } catch {
      set({ user: null, initialized: true })
    }
  },

  clear() {
    setAccessToken(null)
    set({ user: null })
  },
}))

window.addEventListener('auth:expired', () => {
  useAuth.getState().clear()
})
