import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { knowledgeApi } from '../lib/rooms'
import { useAuth } from '../lib/auth'
import { Button, Card, PageShell, SectionTitle } from '../components/ui'

export default function KnowledgePage() {
  const queryClient = useQueryClient()
  const user = useAuth((s) => s.user)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)

  const { data: documents, isLoading } = useQuery({
    queryKey: ['knowledge'],
    queryFn: knowledgeApi.list,
    refetchInterval: (query) =>
      query.state.data?.some((d) => d.status === 'PROCESSING') ? 3000 : false,
  })

  const uploadMutation = useMutation({
    mutationFn: (file: File) => knowledgeApi.upload(file),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['knowledge'] }),
    onError: (err) => setUploadError(err instanceof Error ? err.message : '업로드 실패'),
  })

  const deleteMutation = useMutation({
    mutationFn: knowledgeApi.remove,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['knowledge'] }),
  })

  const handleFiles = (files: FileList | null) => {
    setUploadError(null)
    if (!files?.length) return
    const file = files[0]
    if (!/\.(md|txt|markdown|pdf|docx)$/i.test(file.name)) {
      setUploadError('md / txt / pdf / docx 파일만 업로드할 수 있습니다.')
      return
    }
    if (file.size > 20 * 1024 * 1024) {
      setUploadError('파일이 너무 큽니다 (최대 20MB).')
      return
    }
    uploadMutation.mutate(file)
  }

  const download = async (documentId: number) => {
    const { downloadUrl } = await knowledgeApi.downloadUrl(documentId)
    window.open(downloadUrl, '_blank')
  }

  return (
    <PageShell>
      <SectionTitle sub="여기 올린 문서는 임베딩되어, 회의 중 대화 맥락과 관련될 때 AI가 자동으로 추천합니다.">
        지식 위키
      </SectionTitle>

      <Card
        className="mb-6 border-dashed p-10 text-center"
        // 드래그&드롭 업로드
      >
        <div
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault()
            handleFiles(e.dataTransfer.files)
          }}
        >
          <p className="text-[15px] font-bold text-ink">문서를 끌어다 놓거나 파일을 선택하세요</p>
          <p className="mt-1 text-[13px] text-muted">.md / .txt / .pdf / .docx · 최대 20MB</p>
          <input
            ref={fileInputRef}
            type="file"
            accept=".md,.txt,.markdown,.pdf,.docx"
            className="hidden"
            onChange={(e) => handleFiles(e.target.files)}
          />
          <Button
            variant="outline"
            className="mt-4"
            disabled={uploadMutation.isPending}
            onClick={() => fileInputRef.current?.click()}
          >
            {uploadMutation.isPending ? '업로드 · 임베딩 중…' : '파일 선택'}
          </Button>
          {uploadError && <p className="mt-3 text-[13px] font-semibold text-danger">{uploadError}</p>}
        </div>
      </Card>

      {isLoading ? (
        <p className="text-muted">불러오는 중…</p>
      ) : !documents?.length ? (
        <p className="text-center text-[14px] text-muted">아직 문서가 없습니다. 첫 문서를 올려보세요.</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {documents.map((doc) => (
            <Card key={doc.documentId} className="p-5">
              <div className="flex items-start justify-between gap-2">
                <h3 className="min-w-0 truncate text-[15px] font-extrabold text-ink">{doc.title}</h3>
                <StatusBadge status={doc.status} />
              </div>
              <p className="mt-1 truncate font-mono text-[12px] text-faint">{doc.filename}</p>
              <p className="mt-3 text-[12px] text-muted">
                청크 {doc.chunkCount}개 · {(doc.fileSize / 1024).toFixed(1)}KB ·{' '}
                {new Date(doc.createdTime).toLocaleDateString('ko-KR')}
              </p>
              <div className="mt-4 flex gap-2">
                <button
                  onClick={() => void download(doc.documentId)}
                  className="flex-1 rounded-full border border-line-strong py-1.5 text-[12px] font-bold text-muted hover:border-primary hover:text-primary"
                >
                  다운로드
                </button>
                {doc.ownerId === user?.userId && (
                  <button
                    onClick={() => deleteMutation.mutate(doc.documentId)}
                    className="flex-1 rounded-full border border-line-strong py-1.5 text-[12px] font-bold text-muted hover:border-danger hover:text-danger"
                  >
                    삭제
                  </button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </PageShell>
  )
}

function StatusBadge({ status }: { status: 'PROCESSING' | 'READY' | 'FAILED' }) {
  const map = {
    READY: ['임베딩 완료', 'bg-success/10 text-success'],
    PROCESSING: ['처리 중', 'bg-primary-soft text-primary-deep'],
    FAILED: ['실패', 'bg-danger/10 text-danger'],
  } as const
  const [label, cls] = map[status]
  return (
    <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-bold ${cls}`}>{label}</span>
  )
}
