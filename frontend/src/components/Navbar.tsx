import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function Navbar() {
  const { isAuthenticated, logout, userEmail, userRole } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement | null>(null)

  const activePage =
    location.pathname === '/vendor' ? 'vendor' :
    location.pathname === '/search' ? 'search' :
    location.pathname === '/chat' ? 'chat' :
    location.pathname === '/tryon' ? 'tryon' :
    'catalog'

  useEffect(() => {
    if (!menuOpen) return
    const handleClickOutside = (e: MouseEvent) => {
      if (!menuRef.current?.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  const navLinkClass = (page: string) =>
    `rounded-lg px-4 py-2 text-sm font-semibold transition-all duration-200 ${
      activePage === page
        ? 'bg-[#0070CD] text-white shadow-md'
        : 'bg-transparent text-[#0070CD] hover:bg-[#0070CD]/10'
    }`

  return (
    <header className="sticky top-0 z-50 border-b border-purple-200/50 bg-white/90 backdrop-blur-md dark:border-purple-800/50 dark:bg-slate-950/90 shadow-lg">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
        <Link to="/" className="flex items-center gap-3 text-lg font-bold text-[#0070CD] hover:scale-105 transition-transform duration-200">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#0070CD] text-sm font-bold text-white">VR</span>
          <span>Visual Retail Discovery</span>
        </Link>

        <nav className="flex items-center gap-2 rounded-xl border border-[#0070CD]/20 bg-white p-1">
          <Link to="/" className={navLinkClass('catalog')}>Home</Link>
          <Link to="/search" className={navLinkClass('search')}>Search</Link>
          <Link to="/chat" className={navLinkClass('chat')}>AI Stylist</Link>
          <Link to="/tryon" className={navLinkClass('tryon')}>Try-On</Link>
          {userRole === 'superuser' && (
            <Link to="/vendor" className={navLinkClass('vendor')}>Vendor</Link>
          )}
        </nav>

        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen((o) => !o)}
                className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-r from-purple-600 to-blue-600 text-xs font-semibold text-white shadow-lg transition-all duration-200 hover:shadow-xl hover:scale-110"
                aria-label="Open profile menu"
              >
                {userEmail?.charAt(0).toUpperCase() ?? 'U'}
              </button>

              {menuOpen && (
                <div className="absolute right-0 z-10 mt-2 w-48 rounded-xl border border-purple-200/50 bg-white/95 backdrop-blur-md py-2 shadow-xl dark:border-purple-800/50 dark:bg-slate-900/95">
                  <div className="px-4 py-2 text-xs font-semibold text-slate-500 dark:text-slate-400">Signed in as</div>
                  <div className="px-4 pb-1 text-sm font-semibold text-slate-900 dark:text-slate-100 break-all">{userEmail}</div>
                  <div className="px-4 pb-2 text-xs font-medium text-slate-500 dark:text-slate-400">
                    Role: <span className="font-semibold text-purple-700 dark:text-purple-300">{userRole}</span>
                  </div>
                  <div className="border-t border-purple-100 dark:border-purple-800" />
                  <button
                    type="button"
                    onClick={() => { void logout().finally(() => { setMenuOpen(false); navigate('/login') }) }}
                    className="w-full px-4 py-2 text-left text-sm text-slate-700 transition-colors duration-200 hover:bg-purple-50 dark:text-slate-200 dark:hover:bg-purple-900/50"
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            <Link
              to="/login"
              className="rounded-full bg-gradient-to-r from-slate-100 to-slate-200 px-4 py-2 text-sm font-medium text-slate-700 shadow-md transition-all duration-200 hover:shadow-lg hover:scale-105 dark:from-slate-800 dark:to-slate-700 dark:text-slate-200 dark:hover:from-slate-700 dark:hover:to-slate-600"
            >
              Login
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
