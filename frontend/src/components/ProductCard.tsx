import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Product } from '../types/product'
import { useSearchImage } from '../context/SearchImageContext'

const FALLBACK_IMAGE = 'https://via.placeholder.com/600x400?text=No+Image'

function ImageLightbox({ src, alt, onClose }: { src: string; alt: string; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fade-in"
      onClick={onClose}
    >
      <div
        className="relative max-w-2xl w-full rounded-2xl overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <img src={src} alt={alt} className="w-full object-contain bg-slate-950 max-h-[85vh]" />
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="absolute top-3 right-3 rounded-full bg-black/50 p-1.5 text-white hover:bg-black/80 transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
          </svg>
        </button>
        <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/70 to-transparent px-4 py-3">
          <p className="text-white text-sm font-semibold truncate">{alt}</p>
        </div>
      </div>
    </div>
  )
}

const scoreColor = (pct: number) =>
  pct >= 80 ? 'text-emerald-400' : pct >= 60 ? 'text-amber-400' : 'text-red-400'

export function ProductCard({ product }: { product: Product }) {
  const [src, setSrc] = useState(product.imagePath || FALLBACK_IMAGE)
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const { startSearchFromUrl } = useSearchImage()
  const navigate = useNavigate()

  const matchPct = Math.round(product.score * 100)

  const nameNorm = (product.name ?? '').toLowerCase().trim()
  const brandNorm = (product.brand ?? '').toLowerCase().trim()
  const categoryTags = product.detectedLabels.filter(
    (l) => l.toLowerCase().trim() !== nameNorm && l.toLowerCase().trim() !== brandNorm,
  )

  const handleFindSimilar = (e: React.MouseEvent) => {
    e.stopPropagation()
    startSearchFromUrl(product.imagePath)
    navigate('/search')
  }

  return (
    <>
      {lightboxOpen && (
        <ImageLightbox
          src={src}
          alt={product.name || 'Product'}
          onClose={() => setLightboxOpen(false)}
        />
      )}

      <article
        className="group relative flex flex-col overflow-hidden rounded-2xl border border-[#0070CD]/15 bg-white shadow-md transition-all duration-300 hover:-translate-y-1 hover:shadow-xl dark:border-[#0070CD]/20 dark:bg-slate-900 animate-fade-in cursor-zoom-in"
        onClick={() => setLightboxOpen(true)}
      >
        <div
          className="relative w-full shrink-0 overflow-hidden bg-gradient-to-br from-[#e6f0fa] to-[#f5faff] dark:from-slate-800 dark:to-slate-700"
          style={{ paddingBottom: '75%' }}
        >
          <img
            className="absolute inset-0 h-full w-full object-contain transition-transform duration-300 group-hover:scale-105"
            src={src}
            alt={product.name || 'Product'}
            loading="lazy"
            onError={() => { if (src !== FALLBACK_IMAGE) setSrc(FALLBACK_IMAGE) }}
          />
          <div className="absolute top-2 right-2 rounded-full bg-black/30 border border-white/20 px-2.5 py-1 flex flex-col items-center leading-none gap-0.5">
            <span className={`text-[12px] font-bold ${scoreColor(matchPct)}`}>{matchPct}%</span>
            <span className="text-[10px] text-white/70">match</span>
          </div>
        </div>

        <div className="flex flex-col gap-1.5 p-3 flex-1 min-w-0">
          {product.brand && (
            <p className="truncate text-[11px] font-semibold uppercase tracking-widest text-[#0070CD] dark:text-[#4da6e8]">
              {product.brand}
            </p>
          )}

          <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 leading-snug line-clamp-2">
            {product.name || 'Unknown Product'}
          </h3>

          {categoryTags.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-0.5">
              {categoryTags.map((label) => (
                <span
                  key={label}
                  className="inline-block max-w-full truncate rounded-full bg-[#0070CD]/10 px-2 py-0.5 text-[11px] font-medium text-[#0070CD] dark:bg-[#0070CD]/20 dark:text-[#4da6e8]"
                >
                  {label}
                </span>
              ))}
            </div>
          )}

          {product.price != null && (
            <div className="mt-auto flex items-center justify-between gap-2 pt-2 border-t border-[#0070CD]/10 dark:border-[#0070CD]/20">
              <span className="text-base font-extrabold text-[#0070CD] dark:text-[#4da6e8] leading-none">
                ₹{product.price.toLocaleString('en-IN')}
              </span>
              <button
                type="button"
                onClick={handleFindSimilar}
                className="flex items-center gap-1 rounded-full bg-[#0070CD] px-3 py-1 text-[11px] font-semibold text-white shadow-sm transition-all duration-200 hover:bg-[#005fa3] hover:shadow-md hover:scale-105 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#0070CD] whitespace-nowrap cursor-pointer"
              >
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                Find Similar
              </button>
            </div>
          )}
        </div>
      </article>
    </>
  )
}
