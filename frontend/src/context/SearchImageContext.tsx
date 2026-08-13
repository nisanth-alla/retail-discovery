import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import type { Product } from '../types/product'
import { getMockResponse, searchVisualProducts } from '../services/api'

export type SearchStatus = 'idle' | 'loading' | 'success' | 'error'

interface SearchImageContextValue {
  startSearchFromUrl: (url: string) => void
  imageFile: File | null
  previewUrl: string | null
  results: Product[]
  status: SearchStatus
  error: string | null
  hasImage: boolean
  setImage: (file: File) => void
  removeImage: () => void
  search: () => Promise<Product[]>
}

const SearchImageContext = createContext<SearchImageContextValue | null>(null)

export function SearchImageProvider({ children }: { children: ReactNode }) {
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [results, setResults] = useState<Product[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!imageFile) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(imageFile)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [imageFile])

  const setImage = useCallback((file: File) => {
    setImageFile(file)
    setResults([])
    setStatus('idle')
    setError(null)
  }, [])

  const removeImage = useCallback(() => {
    setImageFile(null)
    setResults([])
    setStatus('idle')
    setError(null)
  }, [])

  const runSearch = useCallback(async (file: File): Promise<Product[]> => {
    abortControllerRef.current?.abort()
    const controller = new AbortController()
    abortControllerRef.current = controller

    setStatus('loading')
    setError(null)

    try {
      const response = await searchVisualProducts(file, controller.signal)
      setResults(response.results)
      setStatus('success')
      return response.results
    } catch (err) {
      if ((err as any)?.name === 'AbortError') return []
      setError((err as Error).message ?? 'Something went wrong')
      setStatus('error')
      const fallback = getMockResponse().results
      setResults(fallback)
      return fallback
    }
  }, [])

  const search = useCallback(async (): Promise<Product[]> => {
    if (!imageFile) return []
    return runSearch(imageFile)
  }, [imageFile, runSearch])

  const startSearchFromUrl = useCallback((url: string) => {
    abortControllerRef.current?.abort()
    setStatus('loading')
    setError(null)
    setResults([])

    fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error(`Image request failed: ${res.status}`)
        return res.blob()
      })
      .then((blob) => {
        if (!blob.type.startsWith('image/')) throw new Error('The catalogue response was not an image')
        const filename = decodeURIComponent(url.split('/').pop() || 'image.jpg')
        const file = new File([blob], filename, { type: blob.type || 'image/jpeg' })
        setImageFile(file)
        runSearch(file)
      })
      .catch(() => {
        setStatus('error')
        setError('Failed to load image. Try uploading manually.')
      })
  }, [runSearch])

  const hasImage = useMemo(() => Boolean(imageFile), [imageFile])

  return (
    <SearchImageContext.Provider
      value={{
        startSearchFromUrl,
        imageFile,
        previewUrl,
        results,
        status,
        error,
        hasImage,
        setImage,
        removeImage,
        search,
      }}
    >
      {children}
    </SearchImageContext.Provider>
  )
}

export function useSearchImage() {
  const ctx = useContext(SearchImageContext)
  if (!ctx) throw new Error('useSearchImage must be used within SearchImageProvider')
  return ctx
}
