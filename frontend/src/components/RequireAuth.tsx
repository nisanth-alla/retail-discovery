import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'


type RequireAuthProps = {
  children: ReactNode
  requiredRole?: 'user' | 'superuser'
}


export function RequireAuth({ children, requiredRole }: RequireAuthProps) {
  const { isAuthenticated, userRole } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (requiredRole && userRole !== requiredRole) {
    // Optionally, redirect to home or show unauthorized message
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
