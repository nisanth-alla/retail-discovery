type ImagePreviewProps = {
  src: string
  alt?: string
}

export function ImagePreview({ src, alt }: ImagePreviewProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="relative overflow-hidden rounded-xl bg-slate-50 dark:bg-slate-950">
        <img
          src={src}
          alt={alt ?? 'Uploaded preview'}
          className="m-auto h-64 w-full max-w-full object-contain"
          loading="lazy"
        />
      </div>
    </div>
  )
}
