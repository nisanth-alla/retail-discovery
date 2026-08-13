import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CatalogCard } from '../components/CatalogCard'
import { useSearchImage } from '../context/SearchImageContext'
import { API_BASE, fetchCatalogImages, type CatalogImage, searchCatalogByText } from '../services/api'

const PAGE_SIZE = 24

export function Home() {
  const navigate = useNavigate()
  const { startSearchFromUrl } = useSearchImage()

  const [catalogImages, setCatalogImages] = useState<CatalogImage[]>([])
  const [catalogTotal, setCatalogTotal] = useState(0)
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [catalogError, setCatalogError] = useState<string | null>(null)
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const [activeCategory, setActiveCategory] = useState('All')
  const [searchTerm, setSearchTerm] = useState('')
  const [searchResults, setSearchResults] = useState<CatalogImage[] | null>(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)

  useEffect(() => {
    fetchCatalogImages()
      .then((data) => {
        setCatalogImages(data.images)
        setCatalogTotal(data.total)
      })
      .catch((err: Error) => setCatalogError(err.message))
      .finally(() => setCatalogLoading(false))
  }, [])

  const categories = useMemo(() => {
    const seen = new Map<string, string>()
    catalogImages.forEach((img) => {
      const raw = (img.filename.replace(/\.[^.]+$/, '').split('_')[2] ?? 'Other').trim()
      const lower = raw.toLowerCase()
      if (!seen.has(lower)) seen.set(lower, raw)
    })
    return ['All', ...Array.from(seen.values()).sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()))]
  }, [catalogImages])

  const trending = useMemo(() => catalogImages.slice(0, 5), [catalogImages])

  const filteredCatalog = useMemo(() => {
    if (activeCategory === 'All') return catalogImages
    const normalizedActive = activeCategory.toLowerCase()
    return catalogImages.filter((img) => {
      const raw = (img.filename.replace(/\.[^.]+$/, '').split('_')[2] ?? 'Other').trim()
      return raw.toLowerCase() === normalizedActive
    })
  }, [catalogImages, activeCategory])

  const visibleCatalog = filteredCatalog.slice(0, visibleCount)
  const visibleSearchResults = searchResults?.slice(0, visibleCount) ?? null

  const handleCatalogFindSimilar = useCallback(
    (image: CatalogImage) => {
      startSearchFromUrl(`${API_BASE}${image.url}`)
      navigate('/search')
    },
    [startSearchFromUrl, navigate],
  )

  const handleCategoryChange = (cat: string) => {
    setActiveCategory(cat)
    setVisibleCount(PAGE_SIZE)
  }

  const handleTextSearch = async () => {
    if (!searchTerm.trim()) return
    setSearchLoading(true)
    setSearchError(null)
    try {
      const res = await searchCatalogByText(searchTerm.trim())
      setSearchResults(res.images)
      setVisibleCount(PAGE_SIZE)
    } catch (err: any) {
      setSearchError(err.message || 'Search failed')
      setSearchResults([])
    } finally {
      setSearchLoading(false)
    }
  }

  const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleTextSearch()
  }

  useEffect(() => {
    if (!searchTerm.trim()) {
      setSearchResults(null)
      setSearchError(null)
    }
  }, [searchTerm])

  const displayCount = activeCategory === 'All' ? catalogTotal : filteredCatalog.length

  return (
    <div className="min-h-screen bg-white">
      <div className="bg-[#0070CD] py-8 px-4">
        <div className="mx-auto max-w-7xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-white">Home</h1>
            <p className="text-white/90 text-sm mt-1">Search a demo fashion catalogue by image, text, or style.</p>
          </div>
          <div className="flex w-full sm:w-auto items-center gap-2 bg-white/90 rounded-xl shadow-md px-2 py-1 mt-4 sm:mt-0">
            <button
              type="button"
              className="p-2 text-[#0070CD] hover:bg-[#0070CD]/10 rounded-lg"
              onClick={handleTextSearch}
              aria-label="Search"
              disabled={searchLoading}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>
            <input
              type="text"
              placeholder="Search for products, brands, categories..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              onKeyDown={handleInputKeyDown}
              className="flex-1 bg-transparent outline-none px-2 py-2 text-sm text-[#0070CD] placeholder:text-[#0070CD]/60"
              disabled={searchLoading}
            />
            <button
              type="button"
              className="inline-flex items-center gap-2 rounded-lg bg-[#0070CD] px-4 py-2 text-sm font-semibold text-white shadow hover:bg-[#005fa3] transition-all"
              onClick={() => navigate('/search')}
            >
              Visual Search
            </button>
          </div>
        </div>
      </div>

      {!catalogLoading && trending.length > 0 && searchResults === null && (
        <section className="mx-auto max-w-7xl px-4 py-6">
          <h2 className="text-xl font-bold text-[#0070CD] mb-4">Trending Now</h2>
          <div className="flex gap-4 overflow-x-auto pb-2">
            {trending.map((img) => (
              <div key={img.filename} className="min-w-[180px] max-w-[200px]">
                <CatalogCard image={img} onFindSimilar={handleCatalogFindSimilar} />
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="py-8 px-4">
        <div className="mx-auto max-w-7xl">
          {searchResults !== null ? (
            <>
              {searchLoading && <div className="text-center py-8 text-[#0070CD]">Searching…</div>}
              {searchError && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-sm text-red-700">
                  {searchError}
                </div>
              )}
              {!searchLoading && !searchError && visibleSearchResults && (
                <>
                  {visibleSearchResults.length === 0 ? (
                    <div className="py-16 text-center text-slate-500 text-sm">
                      No results found for "{searchTerm}".
                    </div>
                  ) : (
                    <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                      {visibleSearchResults.map((img) => (
                        <CatalogCard key={img.filename} image={img} onFindSimilar={handleCatalogFindSimilar} />
                      ))}
                    </div>
                  )}
                  {searchResults && visibleCount < searchResults.length && (
                    <div className="mt-10 flex justify-center">
                      <button
                        type="button"
                        onClick={() => setVisibleCount((n) => n + PAGE_SIZE)}
                        className="inline-flex items-center gap-2 rounded-full border border-[#0070CD]/30 bg-white px-6 py-2.5 text-sm font-semibold text-[#0070CD] shadow-sm hover:bg-[#0070CD]/10 hover:shadow-md transition-all duration-200"
                      >
                        Load more
                        <span className="text-[#0070CD]/60 font-normal">({searchResults.length - visibleCount} remaining)</span>
                      </button>
                    </div>
                  )}
                </>
              )}
            </>
          ) : (
            <>
              {!catalogLoading && categories.length > 1 && (
                <div className="flex flex-wrap items-center gap-2 mb-6">
                  {categories.map((cat) => (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => handleCategoryChange(cat)}
                      className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all duration-200 border-2 ${
                        activeCategory === cat
                          ? 'bg-[#0070CD] text-white border-[#0070CD] shadow-md'
                          : 'bg-white text-[#0070CD] border-[#0070CD]/30 hover:bg-[#0070CD]/10'
                      }`}
                    >
                      {cat}
                    </button>
                  ))}
                  {activeCategory !== 'All' && (
                    <span className="text-xs text-[#0070CD]/60 ml-1">
                      {displayCount} item{displayCount === 1 ? '' : 's'}
                    </span>
                  )}
                </div>
              )}

              {catalogError && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-sm text-red-700">
                  Failed to load catalog: {catalogError}
                </div>
              )}

              {catalogLoading && (
                <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                  {Array.from({ length: 20 }).map((_, i) => (
                    <div key={i} className="rounded-xl overflow-hidden">
                      <div className="aspect-[3/4] animate-pulse bg-slate-200" />
                      <div className="p-3 space-y-1.5">
                        <div className="h-3 w-3/4 rounded animate-pulse bg-slate-200" />
                        <div className="h-3 w-1/2 rounded animate-pulse bg-slate-100" />
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {!catalogLoading && !catalogError && (
                <>
                  {visibleCatalog.length === 0 ? (
                    <div className="py-16 text-center text-slate-500 text-sm">No items in this category.</div>
                  ) : (
                    <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                      {visibleCatalog.map((img) => (
                        <CatalogCard key={img.filename} image={img} onFindSimilar={handleCatalogFindSimilar} />
                      ))}
                    </div>
                  )}

                  {visibleCount < filteredCatalog.length && (
                    <div className="mt-10 flex justify-center">
                      <button
                        type="button"
                        onClick={() => setVisibleCount((n) => n + PAGE_SIZE)}
                        className="inline-flex items-center gap-2 rounded-full border border-[#0070CD]/30 bg-white px-6 py-2.5 text-sm font-semibold text-[#0070CD] shadow-sm hover:bg-[#0070CD]/10 hover:shadow-md transition-all duration-200"
                      >
                        Load more
                        <span className="text-[#0070CD]/60 font-normal">({filteredCatalog.length - visibleCount} remaining)</span>
                      </button>
                    </div>
                  )}
                </>
              )}
            </>
          )}
        </div>
      </section>
    </div>
  )
}
