import { api } from './api'

export interface CreatedRoom {
  roomId: number
  title: string
  hostId: number
  roomCode: string
  createdTime: string
}

export interface RoomInfo {
  roomId: number
  title: string
  status: 'RUNNING' | 'ENDED'
  hostId: number
  createdTime: string
}

export interface Participant {
  userId: number
  email: string
  nickname: string
  name: string
}

export interface RtcToken {
  serverUrl: string
  token: string
  roomName: string
  identity: string
  displayName: string
}

export const roomsApi = {
  create: (title: string) =>
    api<CreatedRoom>('/rooms', { method: 'POST', body: JSON.stringify({ title }) }),

  getByCode: (roomCode: string) => api<RoomInfo>(`/rooms/${roomCode}`),

  join: (roomCode: string) =>
    api<{ roomId: number; title: string; status: string }>(`/rooms/${roomCode}/join`, {
      method: 'POST',
    }),

  leave: (roomId: number) => api(`/rooms/${roomId}/leave`, { method: 'POST' }),

  end: (roomId: number) => api(`/rooms/${roomId}`, { method: 'DELETE' }),

  participants: (roomId: number) =>
    api<{ users: Participant[] }>(`/rooms/${roomId}/participants`),

  rtcToken: (roomId: number) =>
    api<RtcToken>(`/rooms/${roomId}/rtc-token`, { method: 'POST' }),

  messages: (roomId: number) =>
    api<{ messages: { messageId: number; senderId: number; senderNickname: string; message: string; createdTime: string }[] }>(
      `/room/${roomId}/messages`,
    ),

  audioUploadUrl: (roomId: number, extension?: string) =>
    api<{ uploadUrl: string }>(
      `/rooms/${roomId}/audios/upload-url${extension ? `?extension=${extension}` : ''}`,
    ),

  registerAudio: (
    roomId: number,
    payload: {
      path: string
      mimeType: string
      duration?: number
      fileSize?: number
      startTime?: string
      endTime?: string
    },
  ) => api(`/rooms/${roomId}/audios`, { method: 'POST', body: JSON.stringify(payload) }),

  reportStatus: (roomId: number) =>
    api<{ status: 'DONE' | 'PENDING' }>(`/rooms/${roomId}/report/status`),

  speakingStats: (roomId: number) =>
    api<SpeakerStat[]>(`/rooms/${roomId}/speaking-stats`),

  copilotAsk: (roomId: number, question: string) =>
    api<CopilotAnswer>(`/rooms/${roomId}/copilot`, {
      method: 'POST',
      body: JSON.stringify({ question }),
    }),

  myRooms: () => api<MyRoomEntry[]>('/users/me/rooms'),

  report: (roomId: number) =>
    api<{ reportId: number; ownerId: number; roomId: number; content: string; createdTime: string; title: string }>(
      `/rooms/${roomId}/report`,
    ),
}

export interface SpeakerStat {
  userId: number
  name: string
  totalSeconds: number
}

export interface CopilotAnswer {
  answer: string
  sources: { documentId: number; title: string; score: number }[]
}

export interface MyRoomEntry {
  roomId: number
  title: string
  roomCode: string
  status: 'RUNNING' | 'ENDED'
  myRole: 'OWNER' | 'PARTICIPANT'
  joinedTime: string
  endedTime: string | null
  reportStatus: 'DONE' | 'PENDING' | 'NONE'
}

export const aiApi = {
  translate: (texts: string[], targetLang: 'en' | 'ja' | 'zh') =>
    api<{ translations: string[] }>('/ai/translate', {
      method: 'POST',
      body: JSON.stringify({ texts, targetLang }),
    }),
}

export interface SearchResult {
  wiki: {
    documentId: number
    title: string
    filename: string
    snippet: string
    score: number
  }[]
  meetings: {
    roomId: number
    roomTitle: string
    speakerName: string
    snippet: string
    spokeTime: string | null
    score: number
  }[]
}

export const searchApi = {
  search: (query: string) => api<SearchResult>(`/search?q=${encodeURIComponent(query)}`),
}

export interface KnowledgeDocument {
  documentId: number
  ownerId: number
  title: string
  filename: string
  status: 'PROCESSING' | 'READY' | 'FAILED'
  chunkCount: number
  fileSize: number
  createdTime: string
}

export const knowledgeApi = {
  list: () => api<KnowledgeDocument[]>('/knowledge/documents'),

  upload: (file: File, title?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (title) form.append('title', title)
    return api<{ documentId: number }>('/knowledge/documents', { method: 'POST', body: form })
  },

  downloadUrl: (documentId: number) =>
    api<{ downloadUrl: string }>(`/knowledge/documents/${documentId}/download-url`),

  remove: (documentId: number) =>
    api(`/knowledge/documents/${documentId}`, { method: 'DELETE' }),
}
