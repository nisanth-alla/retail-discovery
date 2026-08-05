import { useState } from 'react'
import { API_BASE, type CatalogImage } from '../services/api'

interface ParsedProduct {
  name: string
  brand: string
  category: string
  price: number | null
}

function parseFilename(filename: string): ParsedProduct {
  const base = filename.replace(/\.[^.]+$/, '')
  const parts = base.split('_')

  if (parts.length < 4) return { name: base, brand: '', category: '', price: null }

  const colorAndName = parts[0]
  const dashIdx = colorAndName.indexOf('-')
  const color = dashIdx >= 0 ? colorAndName.slice(0, dashIdx) : ''
  const shortName = dashIdx >= 0 ? colorAndName.slice(dashIdx + 1) : colorAndName

  const priceRaw = parseInt(parts[3])

  return {
    name: color ? `${color} ${shortName}` : shortName,
    brand: parts[1],
    category: parts[2],
    price: isNaN(priceRaw) ? null : priceRaw,
  }
}

const FALLBACK = 'https://via.placeholder.com/400x500?text=No+Image'

interface CatalogCardProps {
  image: CatalogImage
  onFindSimilar: (image: CatalogImage) => void
}

export function CatalogCard({ image, onFindSimilar }: CatalogCardProps) {
  const info = parseFilename(image.filename)
  const [imgLoaded, setImgLoaded] = useState(false)
  const [src, setSrc] = useState(`${API_BASE}${image.url}`)

  return (
    <article className="group relative overflow-hidden rounded-xl bg-white shadow-md hover:shadow-xl transition-all duration-300 hover:-translate-y-1 cursor-pointer border border-[#0070CD]/10">
      <div className="aspect-[3/4] overflow-hidden bg-gradient-to-br from-[#e6f0fa] to-[#f5faff] relative">
        {!imgLoaded && (
          <div className="absolute inset-0 animate-pulse bg-gradient-to-br from-[#b3d6f6]/60 to-[#e6f0fa]/60" />
        )}
        <img
          src={src}
          alt={info.name}
          loading="lazy"
          onLoad={() => setImgLoaded(true)}
          onError={() => { setSrc(FALLBACK); setImgLoaded(true) }}
          className={`h-full w-full object-cover transition-transform duration-500 group-hover:scale-105 ${imgLoaded ? 'opacity-100' : 'opacity-0'}`}
        />

        <div className="absolute inset-0 bg-gradient-to-t from-[#0070CD]/90 via-[#0070CD]/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-3">
          <div className="translate-y-2 group-hover:translate-y-0 transition-transform duration-300 space-y-1.5">
            <span className="inline-block rounded-full bg-[#0070CD] px-2 py-0.5 text-[10px] font-semibold text-white uppercase tracking-wide">
              {info.category}
            </span>
            <h3 className="text-sm font-bold text-white capitalize leading-tight line-clamp-1">{info.name}</h3>
            <p className="text-xs text-blue-100">{info.brand}</p>
            <div className="flex items-center justify-between pt-1">
              {info.price !== null && (
                <span className="text-sm font-bold text-white">₹{info.price.toLocaleString('en-IN')}</span>
              )}
              <button
                type="button"
                onClick={() => onFindSimilar(image)}
                className="ml-auto flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-[#0070CD] shadow-lg hover:bg-[#0070CD]/90 hover:text-white transition-all hover:scale-105 active:scale-95 border border-[#0070CD]/30"
              >
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                Find Similar
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="px-3 py-2.5">
        <p className="text-xs font-semibold text-[#0070CD] capitalize truncate">{info.name}</p>
        <div className="flex items-center justify-between mt-0.5">
          <p className="text-xs text-[#0070CD]/70 truncate">{info.brand}</p>
          {info.price !== null && (
            <span className="text-xs font-bold text-[#0070CD] shrink-0 ml-1">₹{info.price.toLocaleString('en-IN')}</span>
          )}
        </div>
      </div>
    </article>
  )
}
