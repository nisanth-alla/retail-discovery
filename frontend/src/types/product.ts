export interface Product {
  imagePath: string
  detectedLabels: string[]
  score: number
  productId: string | null
  name: string | null
  brand: string | null
  price: number | null
}
