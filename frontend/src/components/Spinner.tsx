export function Spinner({ size = 20 }: { size?: number }) {
  return (
    <div className="inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
    </div>
  )
}
