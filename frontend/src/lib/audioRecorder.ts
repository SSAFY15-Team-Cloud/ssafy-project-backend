import { roomsApi } from './rooms'

const CHUNK_DURATION_MS = 15_000
const MIN_BLOB_SIZE = 4_000

function pickMimeType(): { mimeType: string; extension: string } {
  if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
    return { mimeType: 'audio/webm;codecs=opus', extension: 'webm' }
  }
  if (MediaRecorder.isTypeSupported('audio/ogg;codecs=opus')) {
    return { mimeType: 'audio/ogg;codecs=opus', extension: 'ogg' }
  }
  return { mimeType: 'audio/webm', extension: 'webm' }
}

// Jackson LocalDateTime은 타임존 접미사를 못 읽으므로 'Z'를 뗀 로컬 시각 문자열을 만든다
function toLocalDateTimeString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/**
 * 마이크 트랙을 15초 단위 청크로 녹음해 S3 presigned URL로 업로드하고
 * 메타데이터를 등록해 STT 파이프라인(자막/AI 인사이트/문서 추천)을 구동한다.
 *
 * 청크마다 MediaRecorder를 새로 시작해 각 청크가 독립 재생 가능한 파일이 되게 한다.
 */
export function startChunkedAudioUpload(roomId: number, track: MediaStreamTrack) {
  const { mimeType, extension } = pickMimeType()
  let stopped = false
  let currentRecorder: MediaRecorder | null = null

  const recordOneChunk = () => {
    if (stopped || track.readyState !== 'live') return

    const stream = new MediaStream([track])
    const recorder = new MediaRecorder(stream, { mimeType })
    currentRecorder = recorder
    const startedAt = new Date()
    const parts: Blob[] = []

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) parts.push(event.data)
    }

    recorder.onstop = () => {
      const endedAt = new Date()
      const blob = new Blob(parts, { type: mimeType })

      if (blob.size >= MIN_BLOB_SIZE) {
        void uploadChunk(roomId, blob, mimeType, extension, startedAt, endedAt)
      }
      if (!stopped) {
        recordOneChunk()
      }
    }

    recorder.start()
    setTimeout(() => {
      if (recorder.state !== 'inactive') recorder.stop()
    }, CHUNK_DURATION_MS)
  }

  recordOneChunk()

  return () => {
    stopped = true
    if (currentRecorder && currentRecorder.state !== 'inactive') {
      currentRecorder.stop()
    }
  }
}

async function uploadChunk(
  roomId: number,
  blob: Blob,
  mimeType: string,
  extension: string,
  startedAt: Date,
  endedAt: Date,
) {
  try {
    const { uploadUrl } = await roomsApi.audioUploadUrl(roomId, extension)
    const putUrl = new URL(uploadUrl)

    const putRes = await fetch(uploadUrl, {
      method: 'PUT',
      body: blob,
      headers: { 'Content-Type': mimeType },
    })
    if (!putRes.ok) {
      console.warn('audio chunk upload failed', putRes.status)
      return
    }

    await roomsApi.registerAudio(roomId, {
      path: `${putUrl.origin}${putUrl.pathname}`,
      mimeType,
      duration: Math.round((endedAt.getTime() - startedAt.getTime()) / 1000),
      fileSize: blob.size,
      startTime: toLocalDateTimeString(startedAt),
      endTime: toLocalDateTimeString(endedAt),
    })
  } catch (e) {
    console.warn('audio chunk pipeline error', e)
  }
}
