import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function LoginPage() {
  const { isAuthenticated, login, userRole } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const from = (location.state as any)?.from?.pathname

  useEffect(() => {
    if (!isAuthenticated) return
    navigate(from ?? (userRole === 'superuser' ? '/vendor' : '/'), { replace: true })
  }, [from, isAuthenticated, navigate, userRole])

  const handleSubmit = () => {
    if (!email || !password) {
      setError('Please enter email and password.')
      return
    }
    if (!login(email, password)) {
      setError('Invalid demo credentials. See the frontend README for the available demo accounts.')
    }
  }

  return (
    <div className="flex flex-1 items-center justify-center bg-white px-4 py-12 transition-all duration-500">
      <div className="w-full max-w-md rounded-3xl border border-[#0070CD]/30 bg-white p-8 shadow-2xl animate-fade-in">
        <div className="text-center mb-6">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#0070CD] text-2xl font-bold text-white">VR</div>
          <h1 className="text-2xl font-bold text-[#0070CD]">Sign in</h1>
          <p className="mt-1 text-sm text-[#0070CD]">Enter your email and password to sign in.</p>
        </div>

        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); handleSubmit() }}>
          <label className="block">
            <span className="text-sm font-medium text-[#0070CD]">Email</span>
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              autoComplete="username"
              className="mt-1 w-full rounded-xl border border-[#0070CD]/30 bg-white px-4 py-2 text-sm text-[#0070CD] shadow-sm outline-none transition-all duration-200 focus:border-[#0070CD] focus:ring-2 focus:ring-[#0070CD]/30"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-[#0070CD]">Password</span>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              className="mt-1 w-full rounded-xl border border-[#0070CD]/30 bg-white px-4 py-2 text-sm text-[#0070CD] shadow-sm outline-none transition-all duration-200 focus:border-[#0070CD] focus:ring-2 focus:ring-[#0070CD]/30"
            />
          </label>

          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700 animate-shake">
              {error}
            </div>
          )}

          <button
            type="submit"
            className="w-full rounded-xl bg-[#0070CD] px-4 py-2 text-sm font-semibold text-white shadow-lg transition-all duration-200 hover:shadow-xl hover:scale-105 disabled:cursor-not-allowed disabled:bg-gray-400 disabled:scale-100"
          >
            Log in
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-[#0070CD]">
          Don't have an account?{' '}
          <Link className="font-medium underline hover:text-[#005fa3] transition-colors duration-200" to="/register">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}
