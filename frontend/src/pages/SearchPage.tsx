import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ImagePreview } from '../components/ImagePreview'
import { ImageUploader } from '../components/ImageUploader'
import { ResultsGrid } from '../components/ResultsGrid'
import { Spinner } from '../components/Spinner'
import { useSearchImage } from '../context/SearchImageContext'
import { API_BASE, searchCatalogByText } from '../services/api'
import type { Product } from '../types/product'

type SearchMode = 'image' | 'text'

const isCropped = (p: Product) => p.imagePath.toLowerCase().includes('/cropped')

export function SearchPage() {
  const {
    imageFile,
    previewUrl,
    setImage,
    removeImage,
    results: imageResults,
    status: imageStatus,
    error: imageError,
    search,
    hasImage,
  } = useSearchImage()

  const [textSearch, setTextSearch] = useState('')
  const [textResults, setTextResults] = useState<Product[]>([])
  const [textLoading, setTextLoading] = useState(false)
  const [textError, setTextError] = useState<string | null>(null)
  const [lastTextQuery, setLastTextQuery] = useState('')
  const [activeMode, setActiveMode] = useState<SearchMode>('image')

  const handleTextSearch = async () => {
    if (!textSearch.trim()) return
    setTextLoading(true)
    setTextError(null)
    try {
      const res = await searchCatalogByText(textSearch.trim())
      const mapped: Product[] = res.images.map((img) => ({
        imagePath: `${API_BASE}${img.url}`,
        detectedLabels: [],
        score: img.score != null ? parseFloat(String(img.score)) / 100 : 0,
        productId: null,
        name: img.filename,
        brand: img.brand ?? null,
        price: null,
      }))
      setTextResults(mapped)
      setLastTextQuery(textSearch.trim())
      setActiveMode('text')
    } catch (err: unknown) {
      setTextError((err instanceof Error ? err.message : null) || 'Text search failed')
      setTextResults([])
    } finally {
      setTextLoading(false)
    }
  }

  const handleImageSearch = () => {
    setActiveMode('image')
    search()
  }

  useEffect(() => {
    if (!textSearch.trim()) {
      setTextResults([])
      setTextError(null)
    }
  }, [textSearch])

  useEffect(() => {
    if (imageStatus === 'loading' || imageStatus === 'success') setActiveMode('image')
  }, [imageStatus])

  const isLoading = activeMode === 'text' ? textLoading : imageStatus === 'loading'
  const displayError = activeMode === 'text' ? textError : imageStatus === 'error' ? imageError : null
  const allResults = activeMode === 'text' ? textResults : imageResults
  const detectedItems = activeMode === 'image' ? allResults.filter(isCropped) : []
  const displayResults = allResults.filter((p) => !isCropped(p))
  const resultCount = displayResults.length

  return (
    <main className="mx-auto flex min-h-full max-w-7xl flex-col gap-8 px-2 py-10">
      <header className="space-y-3 text-center animate-fade-in">
        <h1 className="text-3xl font-bold tracking-tight text-[#0070CD]">Visual Search</h1>
        <p className="max-w-xl mx-auto text-sm leading-relaxed text-[#0070CD]/80">
          Upload a product photo and our AI finds visually similar items from the catalog.{' '}
          <Link to="/" className="text-[#0070CD] font-medium underline hover:text-[#005fa3]">
            Browse catalog →
          </Link>
        </p>
      </header>

      <div className="grid gap-8 lg:grid-cols-[450px_1fr]">
        <section className="space-y-5 animate-slide-up">
          <div className="rounded-2xl border border-[#0070CD]/20 bg-white p-6 shadow-xl dark:bg-slate-900">
            <h2 className="text-base font-semibold text-[#0070CD] mb-4">Search Products</h2>
            <div className="flex flex-col gap-4">
              <div className="flex items-center rounded-xl border border-[#0070CD]/30 bg-white shadow-sm focus-within:border-[#0070CD] focus-within:ring-2 focus-within:ring-[#0070CD]/20 transition-all dark:bg-slate-900">
                <input
                  type="text"
                  placeholder="Search by name, brand, category..."
                  value={textSearch}
                  onChange={(e) => setTextSearch(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleTextSearch()}
                  className="flex-1 bg-transparent px-4 py-2.5 text-sm outline-none text-[#0070CD] placeholder:text-[#0070CD]/50"
                  disabled={textLoading}
                />
                <button
                  type="button"
                  onClick={handleTextSearch}
                  disabled={textLoading}
                  aria-label="Text search"
                  className="mr-1.5 flex items-center justify-center rounded-lg bg-[#0070CD] px-3 py-1.5 text-white hover:bg-[#005fa3] disabled:opacity-50 transition-colors"
                >
                  {textLoading
                    ? <Spinner size={16} />
                    : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                  }
                </button>
              </div>

              <div className="flex items-center gap-3 text-xs text-slate-400">
                <div className="flex-1 border-t border-slate-200 dark:border-slate-700" />
                <span>or search by image</span>
                <div className="flex-1 border-t border-slate-200 dark:border-slate-700" />
              </div>

              <ImageUploader imageFile={imageFile} onSelect={setImage} onRemove={removeImage} />
              {previewUrl && <ImagePreview src={previewUrl} alt={imageFile?.name} />}
              <button
                type="button"
                disabled={!hasImage || imageStatus === 'loading'}
                onClick={handleImageSearch}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#0070CD] px-4 py-3 text-sm font-semibold text-white shadow-lg transition-all duration-200 hover:shadow-xl hover:scale-[1.02] disabled:cursor-not-allowed disabled:opacity-50 disabled:scale-100"
              >
                {imageStatus === 'loading'
                  ? <Spinner size={18} />
                  : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                }
                {imageStatus === 'loading' ? 'Searching…' : 'Find Similar Products'}
              </button>
            </div>
          </div>

          <div className="rounded-2xl border border-[#0070CD]/20 bg-white p-5 shadow-sm dark:bg-slate-900">
            <h2 className="text-sm font-semibold text-[#0070CD]">Tips</h2>
            <ul className="mt-2 space-y-1.5 text-xs text-slate-600 dark:text-slate-400">
              <li>• Use a clear, centered photo of one product.</li>
              <li>• Avoid busy backgrounds or multiple items.</li>
              <li>• A clean product image boosts matching accuracy.</li>
            </ul>
          </div>
        </section>

        <section>
          

          {displayError && !isLoading && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-3 mb-4 text-sm text-red-700 dark:bg-red-950/30 dark:border-red-800 dark:text-red-400">
              <strong className="font-semibold">Search failed.</strong> {displayError}
            </div>
          )}

          {isLoading && (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="aspect-[4/3] rounded-2xl animate-pulse bg-slate-100 dark:bg-slate-800" />
              ))}
            </div>
          )}

          {!isLoading && detectedItems.length > 0 && (
            <div className="mb-6 rounded-2xl border border-[#0070CD]/20 bg-[#f0f7ff] dark:bg-[#0070CD]/10 p-4">
              <div className="flex items-center gap-2 mb-3">
                <svg className="w-4 h-4 text-[#0070CD] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <p className="text-xs font-semibold text-[#0070CD]">
                  {detectedItems.length} item{detectedItems.length > 1 ? 's' : ''} detected.
                </p>
              </div>
              <div className="flex gap-3 overflow-x-auto pb-1">
                {detectedItems.map((item, i) => (
                  <div key={`${item.imagePath}-${i}`} className="shrink-0 flex flex-col items-center gap-1.5 w-24">
                    <div className="w-24 h-24 rounded-xl overflow-hidden border border-[#0070CD]/20 bg-white shadow-sm">
                      <img
                        src={item.imagePath}
                        alt={item.name || 'Detected item'}
                        className="w-full h-full object-contain"
                        onError={(e) => { (e.currentTarget as HTMLImageElement).src = 'https://via.placeholder.com/96?text=?' }}
                      />
                    </div>
                    </div>
                ))}
              </div>
            </div>
          )}
          <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
            <div className="flex items-center gap-2">
              <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Results</h2>
              {(displayResults.length > 0 || isLoading) && (
                <span className="inline-flex items-center gap-1 rounded-full bg-[#0070CD]/10 px-2.5 py-0.5 text-xs font-medium text-[#0070CD]">
                  {activeMode === 'text' ? (
                    <>
                      <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                      Text: &ldquo;{lastTextQuery}&rdquo;
                    </>
                  ) : (
                    <>
                      <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                      Image similarity
                    </>
                  )}
                </span>
              )}
            </div>
            <span className="text-sm text-slate-500">
              {isLoading ? 'Searching…' : `${resultCount} product${resultCount === 1 ? '' : 's'}`}
            </span>
          </div>

          {!isLoading && displayResults.length > 0 && (
            <ResultsGrid products={displayResults} />
          )}

          {!isLoading && !displayError && displayResults.length === 0 && (imageStatus === 'success' || (activeMode === 'text' && lastTextQuery)) && (
            <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500 text-center shadow-sm dark:border-slate-800 dark:bg-slate-900">
              {activeMode === 'text'
                ? `No products found for "${lastTextQuery}".`
                : 'No similar products found. Try a clearer, single-item photo.'}
            </div>
          )}

          {!isLoading && !displayError && displayResults.length === 0 && imageStatus === 'idle' && activeMode === 'image' && (
            <div className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 py-20 min-w-[320px] md:min-w-[420px] text-center text-slate-400 text-base gap-3 transition-all duration-500">
              <svg className="w-10 h-10 text-slate-300 dark:text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              Search by text or upload an image to see results here.
            </div>
          )}
        </section>
      </div>
    </main>
  )
}
