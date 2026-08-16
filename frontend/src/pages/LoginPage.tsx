import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function LoginPage() {
  const { isAuthenticated, isInitialized, loginWithGoogle, userRole } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname

  useEffect(() => {
    if (!isInitialized || !isAuthenticated) return
    navigate(from ?? (userRole === 'superuser' ? '/vendor' : '/'), { replace: true })
  }, [from, isAuthenticated, isInitialized, navigate, userRole])

  return (
    <div className="flex flex-1 items-center justify-center bg-white px-4 py-12 transition-all duration-500">
      <div className="w-full max-w-md rounded-3xl border border-[#0070CD]/30 bg-white p-8 text-center shadow-2xl animate-fade-in">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#0070CD] text-2xl font-bold text-white">VR</div>
        <h1 className="text-2xl font-bold text-[#0070CD]">Sign in</h1>
        <p className="mt-2 text-sm text-slate-600">Use your Google account to access your saved retail discovery session.</p>

        <button
          type="button"
          onClick={loginWithGoogle}
          disabled={!isInitialized}
          className="mt-8 flex w-full items-center justify-center gap-3 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60"
        >
          <span className="text-base font-bold">G</span>
          Continue with Google
        </button>

        <p className="mt-6 text-center text-xs leading-5 text-slate-500">
          Google sign-in is handled securely by the backend. Your password is never sent to this application.
        </p>
        <p className="mt-5 text-sm text-slate-600">
          Need help? <Link className="font-medium text-[#0070CD] underline" to="/">Return to catalogue</Link>
        </p>
      </div>
    </div>
  )
}
