import { useState } from 'react'
import { MultiImageUploader } from '../components/MultiImageUploader'
import { Spinner } from '../components/Spinner'
import { uploadVendorProducts } from '../services/api'

const TIPS = [
  'Upload clear product photos on a neutral background.',
  'You can upload up to 10 images at once.',
  'After upload, images will be available for search matches.',
]

export function VendorPage() {
  const [files, setFiles] = useState<File[]>([])
  const [status, setStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle')
  const [message, setMessage] = useState<string | null>(null)

  const canUpload = files.length > 0 && status !== 'uploading'

  const handleUpload = async () => {
    if (!canUpload) return
    setStatus('uploading')
    setMessage(null)
    try {
      await uploadVendorProducts(files)
      setStatus('success')
      setMessage('Images uploaded successfully!')
      setFiles([])
    } catch {
      setStatus('error')
      setMessage('Upload failed. Please try again.')
    }
  }

  return (
    <div className="min-h-full bg-white py-10">
      <div className="mx-auto max-w-6xl px-4">
        <div className="space-y-4 text-center animate-fade-in">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-[#0070CD]">Vendor Product Registration</h1>
            <p className="mt-1 text-lg text-[#0070CD]/80">Upload product images for the visual search catalog.</p>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Use the top navigation to switch between Search and Vendor views.
          </p>
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-[420px_1fr] items-start">
          <section className="space-y-6 rounded-2xl border border-[#0070CD]/20 bg-white p-6 shadow-xl animate-slide-up">
            <div>
              <h2 className="text-lg font-semibold text-[#0070CD]">Upload products</h2>
              <p className="mt-1 text-sm text-[#0070CD]/80">
                Drag & drop or select multiple product images. You can remove items before uploading.
              </p>
            </div>

            <MultiImageUploader files={files} onChange={setFiles} />

            {message && (
              <div className={`rounded-xl border px-4 py-3 text-sm ${
                status === 'success'
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                  : 'border-red-200 bg-red-50 text-red-700'
              }`}>
                {message}
              </div>
            )}

            <button
              type="button"
              onClick={handleUpload}
              disabled={!canUpload}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#0070CD] px-4 py-3 text-sm font-semibold text-white shadow-lg transition-all duration-200 hover:shadow-xl hover:scale-105 disabled:cursor-not-allowed disabled:bg-gray-400 disabled:scale-100"
            >
              {status === 'uploading' && <Spinner size={18} />}
              Upload {files.length > 0 ? `(${files.length})` : ''}
            </button>
          </section>

          <section className="self-start rounded-2xl border border-[#0070CD]/20 bg-white p-6 shadow-xl animate-slide-up">
            <h2 className="text-lg font-semibold text-[#0070CD]">Tips for better results</h2>
            <ul className="mt-3 space-y-2 text-sm text-[#0070CD]/80">
              {TIPS.map((tip) => (
                <li key={tip} className="flex gap-2">
                  <span className="mt-0.5 h-2 w-2 shrink-0 rounded-full bg-[#0070CD]" />
                  <span>{tip}</span>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </div>
  )
}
