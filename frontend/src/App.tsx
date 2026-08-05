import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { VendorPage } from './pages/VendorPage'
import { Home } from './pages/Home'
import { ChatPage } from './pages/ChatPage'
import { SearchPage } from './pages/SearchPage'
import { TryOnPage } from './pages/TryOnPage'
import { Navbar } from './components/Navbar'
import { RequireAuth } from './components/RequireAuth'
import { AuthProvider } from './hooks/useAuth'
import { SearchImageProvider } from './context/SearchImageContext'
import { ChatProvider } from './context/ChatContext'

export default function App() {
  return (
    <AuthProvider>
      <SearchImageProvider>
        <ChatProvider>
        <BrowserRouter>
          <div className="flex min-h-screen flex-col bg-gradient-to-br from-purple-50 via-blue-50 to-indigo-100 text-slate-900 dark:from-slate-950 dark:via-purple-950 dark:to-slate-900 dark:text-slate-100 transition-all duration-500">
            <Navbar />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/chat" element={<ChatPage />} />
              <Route path="/tryon" element={<TryOnPage />} />
              <Route
                path="/vendor"
                element={
                  <RequireAuth requiredRole="superuser">
                    <VendorPage />
                  </RequireAuth>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </BrowserRouter>
        </ChatProvider>
      </SearchImageProvider>
    </AuthProvider>
  )
}
