import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'


type RequireAuthProps = {
  children: ReactNode
  requiredRole?: 'user' | 'superuser'
}


export function RequireAuth({ children, requiredRole }: RequireAuthProps) {
  const { isAuthenticated, userRole, isInitialized } = useAuth()
  const location = useLocation()

  if (!isInitialized) {
    return <div className="flex flex-1 items-center justify-center p-8 text-sm text-slate-500">Checking your session…</div>
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (requiredRole && userRole !== requiredRole) {
    // Optionally, redirect to home or show unauthorized message
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
