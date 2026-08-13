import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { roomsApi } from '../lib/rooms'
import type { ActionItem } from '../lib/rooms'
import { Card } from './ui'

const COLUMNS: { key: ActionItem['status']; label: string; accent: string }[] = [
  { key: 'TODO', label: '할 일', accent: '#94a3b8' },
  { key: 'DOING', label: '진행 중', accent: '#5276df' },
  { key: 'DONE', label: '완료', accent: '#16a34a' },
]

/**
 * 회의에서 추출된 액션아이템 칸반 보드.
 * 카드를 드래그해서 컬럼 사이를 옮기면 상태가 저장된다.
 */
export default function ActionBoard({ roomId }: { roomId: number }) {
  const queryClient = useQueryClient()
  const [dragOver, setDragOver] = useState<ActionItem['status'] | null>(null)

  const { data: items } = useQuery({
    queryKey: ['actionItems', roomId],
    queryFn: () => roomsApi.actionItems(roomId),
    staleTime: 5 * 60 * 1000,
  })

  const moveMutation = useMutation({
    mutationFn: ({ itemId, status }: { itemId: number; status: ActionItem['status'] }) =>
      roomsApi.updateActionItemStatus(itemId, status),
    onMutate: async ({ itemId, status }) => {
      // 낙관적 업데이트 — 드래그 직후 바로 반영
      await queryClient.cancelQueries({ queryKey: ['actionItems', roomId] })
      queryClient.setQueryData<ActionItem[]>(['actionItems', roomId], (prev) =>
        prev?.map((item) => (item.id === itemId ? { ...item, status } : item)),
      )
    },
    onError: () => void queryClient.invalidateQueries({ queryKey: ['actionItems', roomId] }),
  })

  if (!items?.length) {
    return null
  }

  const handleDrop = (status: ActionItem['status'], e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(null)
    const itemId = Number(e.dataTransfer.getData('text/plain'))
    const item = items.find((candidate) => candidate.id === itemId)
    if (item && item.status !== status) {
      moveMutation.mutate({ itemId, status })
    }
  }

  const doneCount = items.filter((item) => item.status === 'DONE').length

  return (
    <Card className="mt-6 p-6 print:hidden">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-[15px] font-extrabold text-ink">📋 액션 보드</h2>
        <span className="font-mono text-[11.5px] text-faint">
          {doneCount}/{items.length} 완료
        </span>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        {COLUMNS.map((column) => {
          const columnItems = items.filter((item) => item.status === column.key)
          return (
            <div
              key={column.key}
              onDragOver={(e) => {
                e.preventDefault()
                setDragOver(column.key)
              }}
              onDragLeave={() => setDragOver(null)}
              onDrop={(e) => handleDrop(column.key, e)}
              className={`rounded-[12px] border-2 border-dashed p-3 transition-colors ${
                dragOver === column.key ? 'border-primary bg-primary-soft/50' : 'border-transparent bg-surface'
              }`}
            >
              <p className="mb-2.5 flex items-center gap-1.5 px-1 text-[12px] font-extrabold text-muted">
                <span className="h-2 w-2 rounded-full" style={{ background: column.accent }} />
                {column.label}
                <span className="ml-auto font-mono text-[11px] text-faint">{columnItems.length}</span>
              </p>
              <div className="space-y-2">
                {columnItems.map((item) => (
                  <div
                    key={item.id}
                    draggable
                    onDragStart={(e) => e.dataTransfer.setData('text/plain', String(item.id))}
                    className={`cursor-grab rounded-[10px] border border-line bg-white p-3 shadow-[0_1px_3px_rgba(23,35,70,0.05)] transition-opacity active:cursor-grabbing ${
                      item.status === 'DONE' ? 'opacity-60' : ''
                    }`}
                  >
                    <p
                      className={`text-[13px] font-semibold leading-relaxed text-ink ${
                        item.status === 'DONE' ? 'line-through' : ''
                      }`}
                    >
                      {item.task}
                    </p>
                    <div className="mt-2 flex items-center justify-between">
                      <span className="rounded-full bg-primary-soft px-2 py-0.5 text-[10.5px] font-bold text-primary-deep">
                        {item.assignee}
                      </span>
                      {item.due && item.due !== '미정' && (
                        <span className="font-mono text-[10.5px] text-faint">~{item.due}</span>
                      )}
                    </div>
                  </div>
                ))}
                {columnItems.length === 0 && (
                  <p className="px-1 py-3 text-center text-[11.5px] text-faint">여기로 드래그</p>
                )}
              </div>
            </div>
          )
        })}
      </div>
      <p className="mt-3 text-right text-[11px] text-faint">회의 발화에서 AI가 추출한 항목입니다 · 카드를 드래그해 상태를 옮기세요</p>
    </Card>
  )
}
