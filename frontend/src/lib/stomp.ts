import { Client } from '@stomp/stompjs'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { getAccessToken } from './api'

export interface RoomStompHandlers {
  onChatMessage?: (payload: ChatMessagePayload) => void
  onChatDeleted?: (payload: { messageId: number }) => void
  onTranscript?: (payload: TranscriptPayload) => void
  onInsight?: (payload: InsightPayload) => void
  onRecommendations?: (payload: RecommendationPayload) => void
  onVoiceAnswer?: (payload: VoiceAnswerPayload) => void
  onPoll?: (payload: PollBroadcast) => void
  onBrailleProblem?: (payload: BrailleProblemBroadcast) => void
  onBrailleBoard?: (payload: BrailleBoardEvent) => void
  onConnected?: () => void
  onDisconnected?: () => void
}

export interface BrailleProblemBroadcast {
  problemId: number
  roomId: number | null
  creatorId: number
  text: string
  cellCount: number
  reviewJamos: string | null
  solved: boolean
  answerUnicode: string | null
  createdTime: string
}

/** 절대값 누적치(멱등 병합용). 강사 세션에만 전달된다. */
export interface BrailleBoardEvent {
  problemId: number
  userId: number
  name: string
  correct: boolean
  accuracy: number
  attempts: number
  solved: boolean
  bestAccuracy: number
  firstCorrect: boolean
}

export interface ChatMessagePayload {
  messageId: number
  senderId: number
  senderNickname: string
  message: string
  createdTime: string
}

export interface TranscriptPayload {
  audioId: number
  speakerId: number
  speakerName: string
  text: string
  startTime: string | null
  endTime: string | null
}

export interface MeetingInsight {
  summary?: string
  keyPoints?: string[]
  actionItems?: { assignee: string; task: string; due: string }[]
  openQuestions?: string[]
  mood?: { emoji: string; label: string }
}

export interface InsightPayload {
  insight: MeetingInsight
  generatedTime: string
}

export interface RecommendedDocument {
  documentId: number
  title: string
  filename: string
  snippet: string
  score: number
  downloadUrl: string
}

export interface RecommendationPayload {
  documents: RecommendedDocument[]
}

export interface PollBroadcast {
  pollId: number
  creatorId: number
  question: string
  options: string[]
  counts: number[]
  totalVotes: number
  closed: boolean
  myVote: number | null
}

export interface VoiceAnswerPayload {
  question: string
  answer: string
  sources: { documentId: number; title: string; score: number }[]
  audioUrl: string
  askedBy: string
  answeredTime: string
}

export function createRoomStompClient(roomId: number, handlers: RoomStompHandlers) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/ws`,
    connectHeaders: {
      Authorization: `Bearer ${getAccessToken()}`,
    },
    reconnectDelay: 3000,
    onConnect: () => {
      const subscriptions: StompSubscription[] = []
      const subscribe = (destination: string, handler: (message: IMessage) => void) => {
        subscriptions.push(client.subscribe(destination, handler))
      }

      subscribe(`/sub/rooms/${roomId}`, (message) => {
        handlers.onChatMessage?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/deletions`, (message) => {
        handlers.onChatDeleted?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/transcripts`, (message) => {
        handlers.onTranscript?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/ai/insights`, (message) => {
        handlers.onInsight?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/ai/recommendations`, (message) => {
        handlers.onRecommendations?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/ai/voice`, (message) => {
        handlers.onVoiceAnswer?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/polls`, (message) => {
        handlers.onPoll?.(JSON.parse(message.body))
      })
      subscribe(`/sub/rooms/${roomId}/braille`, (message) => {
        handlers.onBrailleProblem?.(JSON.parse(message.body))
      })
      // 학습자별 채점 결과는 강사에게만 (user destination)
      subscribe(`/user/sub/rooms/${roomId}/braille/board`, (message) => {
        handlers.onBrailleBoard?.(JSON.parse(message.body))
      })

      handlers.onConnected?.()
    },
    onDisconnect: () => handlers.onDisconnected?.(),
    onWebSocketClose: () => handlers.onDisconnected?.(),
  })

  client.activate()

  return {
    sendChat(message: string) {
      client.publish({
        destination: `/pub/rooms/${roomId}/messages`,
        body: JSON.stringify({ message }),
      })
    },
    deactivate() {
      void client.deactivate()
    },
  }
}
