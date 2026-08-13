import { createContext, useContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'


const AUTH_KEY = 'vrd_isLoggedIn'
const AUTH_USER = 'vrd_userEmail'
const AUTH_ROLE = 'vrd_userRole'

const HARDCODED_USERS = [
  {
    email: 'demo@retail-discovery.local',
    password: 'password123',
    role: 'user',
  },
  {
    email: 'vendor@retail-discovery.local',
    password: 'vendorpassword',
    role: 'superuser',
  },
]


export type AuthState = {
  isAuthenticated: boolean
  userEmail: string | null
  userRole: 'user' | 'superuser' | null
  isInitialized: boolean
}


type AuthContextType = AuthState & {
  login: (email: string, password: string) => boolean
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)


const getAuthStateFromStorage = (): AuthState => {
  if (typeof window === 'undefined' || !window.localStorage) {
    return {
      isAuthenticated: false,
      userEmail: null,
      userRole: null,
      isInitialized: true,
    }
  }

  const stored = localStorage.getItem(AUTH_KEY)
  const email = localStorage.getItem(AUTH_USER)
  const roleRaw = localStorage.getItem(AUTH_ROLE)
  const role: 'user' | 'superuser' | null =
    roleRaw === 'user' || roleRaw === 'superuser' ? roleRaw : null
  return {
    isAuthenticated: stored === 'true',
    userEmail: email,
    userRole: role,
    isInitialized: true,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {

  const [authState, setAuthState] = useState<AuthState>(getAuthStateFromStorage)

  useEffect(() => {
    const handleStorage = () => {
      setAuthState(getAuthStateFromStorage())
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])


  const login = useCallback((email: string, password: string) => {
    const found = HARDCODED_USERS.find(
      (u) => u.email === email.toLowerCase() && u.password === password
    )
    if (!found) return false

    localStorage.setItem(AUTH_KEY, 'true')
    localStorage.setItem(AUTH_USER, found.email)
    localStorage.setItem(AUTH_ROLE, found.role)
    setAuthState({
      isAuthenticated: true,
      userEmail: found.email,
      userRole: found.role as 'user' | 'superuser',
      isInitialized: true,
    })
    return true
  }, [])


  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_KEY)
    localStorage.removeItem(AUTH_USER)
    localStorage.removeItem(AUTH_ROLE)
    setAuthState({ isAuthenticated: false, userEmail: null, userRole: null, isInitialized: true })
  }, [])


  const value: AuthContextType = {
    ...authState,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
