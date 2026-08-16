import { Link } from 'react-router-dom'

export function RegisterPage() {
  return (
    <div className="flex flex-1 items-center justify-center bg-[#F6FAFE] px-4 py-12">
      <div className="w-full max-w-md rounded-3xl border border-[#0070CD]/10 bg-white p-8 text-center shadow-xl">
        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-[#0070CD] text-lg font-bold text-white">VR</div>
        <h1 className="text-2xl font-bold text-[#0070CD]">Create an account</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Accounts are created through Google sign-in. There is no local password database in this demo.
        </p>
        <Link
          to="/login"
          className="mt-7 block w-full rounded-xl bg-[#0070CD] px-4 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-[#005fa3]"
        >
          Continue to sign in
        </Link>
        <Link className="mt-5 block text-sm text-[#0070CD] underline" to="/">Return to catalogue</Link>
      </div>
    </div>
  )
}
