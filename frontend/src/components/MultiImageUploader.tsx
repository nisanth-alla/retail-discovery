import { useCallback, useEffect, useRef, useState } from 'react'

const ACCEPTED_MIME_TYPES = ['image/jpeg', 'image/pjpeg', 'image/png', 'image/webp', 'image/gif']

type MultiImageUploaderProps = {
  files: File[]
  onChange: (files: File[]) => void
  maxFiles?: number
}

type Preview = { file: File; url: string }

export function MultiImageUploader({ files, onChange, maxFiles = 10 }: MultiImageUploaderProps) {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)
  const [previews, setPreviews] = useState<Preview[]>([])

  useEffect(() => {
    const items = files.map((file) => ({ file, url: URL.createObjectURL(file) }))
    setPreviews(items)
    return () => items.forEach(({ url }) => URL.revokeObjectURL(url))
  }, [files])

  const handleFiles = useCallback(
    (fileList: FileList | null) => {
      if (!fileList?.length) return
      const valid = Array.from(fileList).filter((f) => ACCEPTED_MIME_TYPES.includes(f.type))
      if (!valid.length) return
      onChange([...files, ...valid].slice(0, maxFiles))
    },
    [files, maxFiles, onChange],
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

  const removeFile = useCallback(
    (index: number) => onChange(files.filter((_, i) => i !== index)),
    [files, onChange],
  )

  return (
    <div className="space-y-4">
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
            ? 'border-indigo-400 bg-indigo-50 dark:bg-indigo-900/40'
            : 'border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900'
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          accept="image/*"
          multiple
          onChange={(e) => handleFiles(e.target.files)}
        />
        <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
          Drag &amp; drop product images here, or click to browse
        </p>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          PNG, JPG, GIF, WEBP — up to {maxFiles} files
        </p>
      </div>

      {previews.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {previews.map((preview, index) => (
            <div key={preview.url} className="relative rounded-xl border border-slate-200 bg-white p-2 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <img src={preview.url} alt={preview.file.name} className="h-28 w-full rounded-xl object-cover" />
              <button
                type="button"
                onClick={() => removeFile(index)}
                className="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-1 text-xs font-semibold text-white hover:bg-black"
              >
                Remove
              </button>
              <p className="mt-2 truncate text-xs text-slate-500 dark:text-slate-400">{preview.file.name}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
