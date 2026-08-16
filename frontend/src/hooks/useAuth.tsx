import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { getCurrentUser, logoutCurrentUser, API_BASE, type AuthUser } from '../services/api'

export type AuthState = {
  isAuthenticated: boolean
  userEmail: string | null
  userRole: 'user' | 'superuser' | null
  isInitialized: boolean
}

type AuthContextType = AuthState & {
  loginWithGoogle: () => void
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

function toAuthState(user: AuthUser | null): AuthState {
  return {
    isAuthenticated: Boolean(user),
    userEmail: user?.email ?? null,
    userRole: user?.role === 'vendor' ? 'superuser' : user ? 'user' : null,
    isInitialized: true,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    userEmail: null,
    userRole: null,
    isInitialized: false,
  })

  const refresh = useCallback(async () => {
    try {
      setAuthState(toAuthState(await getCurrentUser()))
    } catch {
      setAuthState(toAuthState(null))
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const loginWithGoogle = useCallback(() => {
    window.location.assign(`${API_BASE}/oauth2/authorization/google`)
  }, [])

  const logout = useCallback(async () => {
    await logoutCurrentUser()
    setAuthState(toAuthState(null))
  }, [])

  return (
    <AuthContext.Provider value={{ ...authState, loginWithGoogle, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
