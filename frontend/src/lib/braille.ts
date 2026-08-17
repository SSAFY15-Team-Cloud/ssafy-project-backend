import { api } from './api'

// ===== 타입 =====

export interface BrailleProblem {
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

export interface BrailleCellFeedback {
  index: number
  sourceJamo: string | null
  expected: number
  actual: number
  missingDots: number[]
  extraDots: number[]
  correct: boolean
}

export interface BrailleAttemptResult {
  correct: boolean
  accuracy: number
  expectedUnicode: string
  cells: BrailleCellFeedback[]
}

export interface BrailleBoardEntry {
  userId: number
  name: string
  attempts: number
  solved: boolean
  bestAccuracy: number
  /** 평가 지표는 첫 시도 기준 — 2번째 시도부터는 정답 피드백을 본 뒤다 */
  firstCorrect: boolean
  firstAccuracy: number
}

export interface BrailleProblemBoard {
  problemId: number
  text: string
  entries: BrailleBoardEntry[]
}

export interface BrailleSkill {
  jamo: string
  proficiency: number
  attemptCount: number
  wrongCount: number
  lastWrongTime: string | null
  priority: number
}

export interface BrailleClassContext {
  hostId: number
  host: boolean
}

// ===== API =====

export const brailleApi = {
  context: (roomId: number) => api<BrailleClassContext>(`/rooms/${roomId}/braille/context`),

  createProblem: (roomId: number, text: string) =>
    api<BrailleProblem>(`/rooms/${roomId}/braille/problems`, {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),

  problems: (roomId: number) => api<BrailleProblem[]>(`/rooms/${roomId}/braille/problems`),

  board: (roomId: number) => api<BrailleProblemBoard[]>(`/rooms/${roomId}/braille/board`),

  submitAttempt: (problemId: number, cells: number[]) =>
    api<BrailleAttemptResult>(`/braille/problems/${problemId}/attempts`, {
      method: 'POST',
      body: JSON.stringify({ cells }),
    }),

  mySkills: () => api<BrailleSkill[]>('/braille/skills/me'),

  generateReview: () => api<BrailleProblem[]>('/braille/review/generate', { method: 'POST' }),

  myReview: () => api<BrailleProblem[]>('/braille/review'),
}

// ===== 클라이언트 헬퍼 =====

/** 6bit 점형 값 → 유니코드 점자 문자 (1~6점 비트 배치 동일) */
export function cellToUnicode(value: number) {
  return String.fromCharCode(0x2800 + (value & 0x3f))
}

export function cellsToUnicode(cells: number[]) {
  return cells.map(cellToUnicode).join('')
}

/** 음성 안내 (Web Speech API). 시각장애 학습자 접근성의 기본 채널. */
export function speak(text: string, options?: { interrupt?: boolean; rate?: number }) {
  if (!('speechSynthesis' in window)) return
  if (options?.interrupt !== false) window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'ko-KR'
  utterance.rate = options?.rate ?? 1.1
  window.speechSynthesis.speak(utterance)
}

/** 점 단위 채점 피드백 → 음성 문장 ("2번째 칸, 3점 누락, 5점 잘못 입력") */
export function feedbackToSpeech(result: BrailleAttemptResult) {
  if (result.correct) return '정답입니다! 잘하셨어요.'
  const wrongCells = result.cells.filter((cell) => !cell.correct)
  const parts = wrongCells.slice(0, 3).map((cell) => {
    const segments: string[] = [`${cell.index + 1}번째 칸`]
    if (cell.sourceJamo) segments.push(cell.sourceJamo)
    if (cell.missingDots.length > 0) segments.push(`${cell.missingDots.join(', ')}점 누락`)
    if (cell.extraDots.length > 0) segments.push(`${cell.extraDots.join(', ')}점 잘못 입력`)
    return segments.join(', ')
  })
  const more = wrongCells.length > 3 ? ` 외 ${wrongCells.length - 3}개 칸 오답.` : ''
  return `오답입니다. ${parts.join('. ')}.${more} 다시 시도해 보세요.`
}

// ===== Web Serial (아두이노 6점 촉각 디스플레이) =====

interface SerialPortLike {
  open: (options: { baudRate: number }) => Promise<void>
  close: () => Promise<void>
  writable: WritableStream<Uint8Array> | null
}

let serialPort: SerialPortLike | null = null

export function serialSupported() {
  return 'serial' in navigator
}

export function serialConnected() {
  return serialPort !== null
}

/** 아두이노 연결 (사용자 제스처 필요). 프로토콜: "SET <0-63>\n" / "CLEAR\n" */
export async function connectSerial(): Promise<boolean> {
  if (!serialSupported()) return false
  try {
    const port = (await (
      navigator as unknown as { serial: { requestPort: () => Promise<SerialPortLike> } }
    ).serial.requestPort()) as SerialPortLike
    await port.open({ baudRate: 9600 })
    serialPort = port
    return true
  } catch {
    return false
  }
}

export async function disconnectSerial() {
  if (!serialPort) return
  try {
    await writeChain // 마지막 쓰기와 close의 경합 방지
    await serialPort.close()
  } catch {
    /* 이미 닫힘 */
  }
  serialPort = null
}

// 연속 입력(화음 타이핑)에서 writer lock 충돌이 나지 않도록 쓰기를 직렬화한다
let writeChain: Promise<void> = Promise.resolve()

/** 현재 칸의 점형을 촉각 디스플레이로 출력 */
export function serialSetCell(value: number): Promise<void> {
  writeChain = writeChain
    .then(async () => {
      if (!serialPort?.writable) return
      const writer = serialPort.writable.getWriter()
      try {
        await writer.write(new TextEncoder().encode(`SET ${value & 0x3f}\n`))
      } finally {
        writer.releaseLock()
      }
    })
    .catch(() => {
      /* 포트 분리 등 — 촉각 출력 실패는 무시 */
    })
  return writeChain
}
