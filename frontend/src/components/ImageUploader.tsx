import { useCallback, useRef, useState } from 'react'

const ACCEPTED_MIME_TYPES = ['image/jpeg', 'image/pjpeg', 'image/png', 'image/webp', 'image/gif']

type ImageUploaderProps = {
  imageFile: File | null
  onSelect: (file: File) => void
  onRemove: () => void
}

export function ImageUploader({ imageFile, onSelect, onRemove }: ImageUploaderProps) {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)

  const handleFiles = useCallback(
    (files: FileList | null) => {
      if (!files?.length) return
      const file = files[0]
      if (!ACCEPTED_MIME_TYPES.includes(file.type)) return
      onSelect(file)
    },
    [onSelect],
  )

  const onDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault()
      setIsDragOver(false)
      handleFiles(e.dataTransfer.files)
    },
    [handleFiles],
  )

  const onDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setIsDragOver(true)
  }, [])

  return (
    <div className="space-y-3">
      <div
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click() }}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onDragLeave={() => setIsDragOver(false)}
        className={`relative rounded-xl border border-dashed p-5 text-center transition focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 ${
          isDragOver
            ? 'border-indigo-400 bg-indigo-50 dark:bg-indigo-950/40'
            : 'border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900'
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          accept="image/*"
          onChange={(e) => handleFiles(e.target.files)}
        />
        <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
          {imageFile ? 'Drop another image or click to replace' : 'Drag & drop an image here, or click to choose a file'}
        </p>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">PNG, JPG (max 10MB)</p>
      </div>

      {imageFile && (
        <button
          type="button"
          onClick={onRemove}
          className="inline-flex items-center justify-center rounded-full bg-red-50 px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100 dark:bg-red-950/40 dark:text-red-300 dark:hover:bg-red-900"
        >
          Remove image
        </button>
      )}
    </div>
  )
}
