import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export function RegisterPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = () => {
    if (!email || !password || !confirmPassword) {
      setError('Please fill in all fields.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    setError(null)
    setSuccess(true)
    setTimeout(() => navigate('/login'), 1200)
  }

  const inputClass = 'mt-1 w-full rounded-xl border border-[#0070CD]/20 bg-white px-4 py-2 text-sm text-[#0070CD] shadow-sm outline-none transition focus:border-[#0070CD] focus:ring-2 focus:ring-[#0070CD]/20'

  return (
    <div className="flex flex-1 items-center justify-center bg-[#F6FAFE] px-4 py-12">
      <div className="w-full max-w-md rounded-3xl border border-[#0070CD]/10 bg-white p-8 shadow-xl">
        <div className="flex flex-col items-center mb-6">
          <div className="mb-2 flex h-12 w-12 items-center justify-center rounded-xl bg-[#0070CD] text-lg font-bold text-white">VR</div>
          <h1 className="text-2xl font-bold text-[#0070CD]">Create an account</h1>
        </div>
        <p className="mb-4 text-sm text-[#0070CD]/80 text-center">
          This is a demo registration flow. You will be redirected back to the login page.
        </p>

        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); handleSubmit() }}>
          <label className="block">
            <span className="text-sm font-medium text-[#0070CD]">Email</span>
            <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" autoComplete="email" className={inputClass} />
          </label>
          <label className="block">
            <span className="text-sm font-medium text-[#0070CD]">Password</span>
            <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="••••••••" autoComplete="new-password" className={inputClass} />
          </label>
          <label className="block">
            <span className="text-sm font-medium text-[#0070CD]">Confirm Password</span>
            <input value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} type="password" placeholder="••••••••" autoComplete="new-password" className={inputClass} />
          </label>

          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 animate-shake">{error}</div>
          )}
          {success && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              Registration successful! Redirecting to login…
            </div>
          )}

          <button
            type="submit"
            className="w-full rounded-xl bg-[#0070CD] px-4 py-2 text-sm font-semibold text-white shadow-lg transition hover:bg-[#005fa3] disabled:cursor-not-allowed disabled:bg-[#7bb8e6]"
          >
            Register
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-[#0070CD]/80">
          Already have an account?{' '}
          <Link className="font-medium text-[#0070CD] hover:text-[#005fa3]" to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
