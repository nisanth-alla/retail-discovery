import type { Product } from '../types/product'
import { ProductCard } from './ProductCard'

type ResultsGridProps = {
  products: Product[]
}

export function ResultsGrid({ products }: ResultsGridProps) {
  if (products.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-200 bg-white p-8 text-center text-sm text-slate-500 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-400">
        No items to show yet.
      </div>
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {products.map((product, index) => (
        <ProductCard key={product.productId ?? `${product.imagePath ?? ''}-${index}`} product={product} />
      ))}
    </div>
  )
}
