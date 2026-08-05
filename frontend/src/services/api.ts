import type { Product } from '../types/product'

export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface CatalogImage {
  filename: string
  url: string
  score?: string | number
  brand?: string
}

export type CatalogResponse = {
  total: number
  images: CatalogImage[]
}

export type TextSearchResponse = CatalogResponse

export interface VisualSearchResponse {
  results: Product[]
}

export interface StyleItItem {
  filename: string
  style: string
  url: string
}

export interface StyleItResponse {
  total: number
  images: StyleItItem[]
}

export interface ChatHistoryMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface FashionChatResponse {
  reply: string
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function multipart(...files: Array<[string, File]>): FormData {
  const fd = new FormData()
  for (const [key, file] of files) fd.append(key, file)
  return fd
}

async function post<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { method: 'POST', ...init })
  if (!response.ok) throw new Error(`${url} → ${response.status}`)
  return response.json()
}

// Virtual Try-On API — calls Replicate IDM-VTON directly from the browser

export interface TryOnResponse {
  result_url: string
}

const REPLICATE_TOKEN = import.meta.env.VITE_REPLICATE_TOKEN

/** Resize + compress an image file to max 768px on the longest side, JPEG quality 0.88 */
function compressImage(file: File, maxPx = 768): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = reject
    reader.onload = () => {
      const img = new Image()
      img.onerror = reject
      img.onload = () => {
        const scale = Math.min(1, maxPx / Math.max(img.width, img.height))
        const w = Math.round(img.width  * scale)
        const h = Math.round(img.height * scale)
        const canvas = document.createElement('canvas')
        canvas.width = w; canvas.height = h
        canvas.getContext('2d')!.drawImage(img, 0, 0, w, h)
        resolve(canvas.toDataURL('image/jpeg', 0.88))
      }
      img.src = reader.result as string
    }
    reader.readAsDataURL(file)
  })
}

async function pollReplicate(predictionId: string): Promise<string> {
  const url = `/replicate/v1/predictions/${predictionId}`
  const deadline = Date.now() + 300_000   // 5-minute total polling budget
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 5000))
    const res  = await fetch(url, { headers: { Authorization: `Bearer ${REPLICATE_TOKEN}` } })
    const data = await res.json()
    if (data.status === 'succeeded') {
      const out = data.output
      return Array.isArray(out) ? out[0] : out
    }
    if (data.status === 'failed' || data.status === 'canceled') {
      throw new Error(`Prediction ${data.status}: ${data.error ?? 'unknown error'}`)
    }
  }
  throw new Error('Prediction timed out after 5 minutes')
}

/**
 * AI virtual try-on — calls Replicate IDM-VTON directly from the browser.
 * No backend required.
 */
export async function virtualTryOn(
  personImage: File,
  garmentImage: File,
  category: string,
  garmentDesc: string,
): Promise<TryOnResponse> {
  const [humanDataUri, garmDataUri] = await Promise.all([
    compressImage(personImage),
    compressImage(garmentImage),
  ])

  const response = await fetch(
    '/replicate/v1/models/cuuupid/idm-vton/predictions',
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${REPLICATE_TOKEN}`,
        'Content-Type': 'application/json',
        Prefer: 'wait=60',
      },
      body: JSON.stringify({
        input: {
          human_img:       humanDataUri,
          garm_img:        garmDataUri,
          garment_des:     garmentDesc || 'a garment',
          is_checked:      true,
          is_checked_crop: false,
          denoise_steps:   30,
          seed:            42,
          category,
        },
      }),
    },
  )

  const data = await response.json()
  if (!response.ok) {
    throw new Error(data?.detail ?? data?.error ?? `Replicate error ${response.status}`)
  }

  if (data.status === 'succeeded') {
    const out = data.output
    return { result_url: Array.isArray(out) ? out[0] : out }
  }

  // Still processing — poll until done
  const resultUrl = await pollReplicate(data.id)
  return { result_url: resultUrl }
}

// Mock Data (for local/demo use only)
// ─── Catalog ──────────────────────────────────────────────────────────────────

export async function fetchCatalogImages(): Promise<CatalogResponse> {
  const response = await fetch(`${API_BASE}/api/image/fetch`)
  if (!response.ok) throw new Error(`Catalog fetch failed: ${response.status}`)
  return response.json()
}

export async function searchCatalogByText(text: string): Promise<TextSearchResponse> {
  return post(`${API_BASE}/api/image/searchtext?text=${encodeURIComponent(text)}`)
}

// ─── Visual Search ────────────────────────────────────────────────────────────

export async function searchVisualProducts(
  image: File,
  signal?: AbortSignal,
): Promise<VisualSearchResponse> {
  const response = await fetch(`${API_BASE}/api/image/search`, {
    method: 'POST',
    body: multipart(['file', image]),
    signal,
  })

  if (!response.ok) {
    if (response.status === 404 || response.status === 500) return MOCK_VISUAL_RESPONSE
    throw new Error(`Visual search failed: ${response.status}`)
  }

  const json = await response.json()
  return Array.isArray(json) ? { results: json as Product[] } : (json as VisualSearchResponse)
}

export async function styleItProducts(text?: string, image?: File): Promise<StyleItResponse> {
  const fd = new FormData()
  if (image) fd.append('file', image)
  const qs = text ? `?text=${encodeURIComponent(text)}` : ''
  return post(`${API_BASE}/api/image/styleIt${qs}`, { body: fd })
}

export async function searchByLabel(labels: string[], price: number, image: File): Promise<Product[]> {
  const params = new URLSearchParams({ price: String(price) })
  labels.forEach(l => params.append('label', l))
  return post(`${API_BASE}/api/image/searchByLabel?${params}`, { body: multipart(['file', image]) })
}

// ─── Vendor ───────────────────────────────────────────────────────────────────

export async function uploadVendorProducts(images: File[], signal?: AbortSignal): Promise<void> {
  const fd = new FormData()
  images.forEach(f => fd.append('images', f))
  await fetch(`${API_BASE}/api/image/register`, { method: 'POST', body: fd, signal })
}

// ─── Fashion Chat ─────────────────────────────────────────────────────────────

export async function sendFashionChat(
  message: string,
  conversationHistory?: ChatHistoryMessage[],
  userContext?: string,
): Promise<FashionChatResponse> {
  return post(`${API_BASE}/api/fashion/chat`, {
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, conversationHistory, userContext }),
  })
}

// ─── Mock Data ────────────────────────────────────────────────────────────────

const MOCK_VISUAL_RESPONSE: VisualSearchResponse = {
  results: [
    { imagePath: 'https://picsum.photos/seed/sneaker1/600/400', detectedLabels: ['sneaker', 'shoe'],    score: 0.95, productId: 'p123', name: 'Air Runner',   brand: 'BrandCo',     price: 129.99 },
    { imagePath: 'https://picsum.photos/seed/sneaker2/600/400', detectedLabels: ['sneaker'],            score: 0.89, productId: 'p456', name: 'Street Lite',  brand: 'UrbanKicks',  price: 99.99  },
    { imagePath: 'https://picsum.photos/seed/sneaker3/600/400', detectedLabels: ['sneaker', 'running'], score: 0.83, productId: 'p789', name: 'Trail Runner', brand: 'OutdoorEdge', price: 149.99 },
  ],
}

export function getMockResponse(): VisualSearchResponse {
  return MOCK_VISUAL_RESPONSE
}
