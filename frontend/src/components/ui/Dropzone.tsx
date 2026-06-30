import { Upload } from 'lucide-react'
import { useCallback, useState } from 'react'
import { cn } from '../../lib/utils'

interface DropzoneProps {
  label: string
  accept?: string
  file: File | null
  onFileChange: (file: File | null) => void
  hint?: string
}

export function Dropzone({ label, accept, file, onFileChange, hint }: DropzoneProps) {
  const [dragOver, setDragOver] = useState(false)

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      const dropped = e.dataTransfer.files[0]
      if (dropped) onFileChange(dropped)
    },
    [onFileChange],
  )

  return (
    <div
      onDragOver={(e) => {
        e.preventDefault()
        setDragOver(true)
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      className={cn(
        'relative flex min-h-[120px] cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-4 py-6 text-center transition-colors',
        dragOver ? 'border-brand-500 bg-brand-50' : 'border-border bg-slate-50 hover:border-brand-300 hover:bg-brand-50/50',
        file && 'border-emerald-400 bg-emerald-50',
      )}
    >
      <input
        type="file"
        accept={accept}
        className="absolute inset-0 cursor-pointer opacity-0"
        onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
      />
      <Upload className="mb-2 h-6 w-6 text-muted-foreground" />
      <p className="text-sm font-medium text-foreground">{label}</p>
      {file ? (
        <p className="mt-1 text-xs text-emerald-700">{file.name}</p>
      ) : (
        <p className="mt-1 text-xs text-muted-foreground">
          {hint ?? 'Drag and drop or click to browse'}
        </p>
      )}
    </div>
  )
}
