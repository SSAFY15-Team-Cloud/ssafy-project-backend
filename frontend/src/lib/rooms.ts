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

  copilotAsk: (roomId: number, question: string, history?: { question: string; answer: string }[]) =>
    api<CopilotAnswer>(`/rooms/${roomId}/copilot`, {
      method: 'POST',
      body: JSON.stringify({ question, history }),
    }),

  briefing: (roomId: number) => api<Briefing>(`/rooms/${roomId}/briefing`),

  overview: () => api<WorkspaceOverview>('/users/me/overview'),

  createPoll: (roomId: number, question: string, options: string[]) =>
    api<Poll>(`/rooms/${roomId}/polls`, {
      method: 'POST',
      body: JSON.stringify({ question, options }),
    }),

  getPolls: (roomId: number) => api<Poll[]>(`/rooms/${roomId}/polls`),

  votePoll: (pollId: number, optionIndex: number) =>
    api<Poll>(`/polls/${pollId}/vote`, {
      method: 'POST',
      body: JSON.stringify({ optionIndex }),
    }),

  closePoll: (pollId: number) => api<Poll>(`/polls/${pollId}/close`, { method: 'POST' }),

  enableReportShare: (roomId: number) =>
    api<{ shareToken: string }>(`/rooms/${roomId}/report/share`, { method: 'POST' }),

  sharedReport: (token: string) =>
    api<{ title: string; content: string; createdTime: string }>(`/shared/reports/${token}`, {
      skipAuth: true,
    }),

  myRooms: () => api<MyRoomEntry[]>('/users/me/rooms'),

  replay: (roomId: number) => api<ReplayResult>(`/rooms/${roomId}/replay`),

  actionItems: (roomId: number) => api<ActionItem[]>(`/rooms/${roomId}/action-items`),

  updateActionItemStatus: (itemId: number, status: ActionItem['status']) =>
    api<ActionItem>(`/action-items/${itemId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),

  voiceAsk: (roomId: number, audio: Blob, filename: string) => {
    const form = new FormData()
    form.append('audio', audio, filename)
    return api(`/rooms/${roomId}/voice-ask`, { method: 'POST', body: form })
  },

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

export interface Briefing {
  lastRoomId: number | null
  lastMeetingTitle: string | null
  lastMeetingEndedTime: string | null
  lastMeetingSummary: string | null
  openActionItems: { assignee: string; task: string; due: string; status: string }[]
}

export interface WorkspaceOverview {
  totalMeetings: number
  totalSpeakingSeconds: number
  totalActionItems: number
  doneActionItems: number
  weeklyActivity: { date: string; meetings: number }[]
  pendingActionItems: {
    id: number
    roomId: number
    roomTitle: string
    assignee: string
    task: string
    due: string
    status: string
  }[]
}

export interface Poll {
  pollId: number
  creatorId: number
  question: string
  options: string[]
  counts: number[]
  totalVotes: number
  closed: boolean
  myVote: number | null
}

export interface ActionItem {
  id: number
  roomId: number
  assignee: string
  task: string
  due: string
  status: 'TODO' | 'DOING' | 'DONE'
}

export interface ReplaySegment {
  audioId: number
  speakerId: number
  speakerName: string
  text: string | null
  mimeType: string
  duration: number
  startTime: string | null
  url: string
}

export interface ReplayResult {
  roomTitle: string
  roomStatus: 'RUNNING' | 'ENDED'
  segments: ReplaySegment[]
  chapters: { title: string; segmentIndex: number }[]
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
