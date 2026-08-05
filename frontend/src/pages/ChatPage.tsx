import { ChatWithAvatar } from '../components/ChatWithAvatar'

export function ChatPage() {
  return (
    <div className="flex flex-col bg-white" style={{ minHeight: 'calc(100vh - 64px)' }}>
      <div className="bg-[#0070CD] py-6 px-4">
        <div className="mx-auto max-w-[1400px]">
          <h1 className="text-3xl font-bold text-white">AI Stylist</h1>
          <p className="mt-1 text-sm text-white/90">
            Chat with Innovator, your personal stylist — get outfit ideas, styling advice, and more a photo, or search by style.
          </p>
        </div>
      </div>
      <div className="flex flex-1 mx-auto w-full max-w-[1400px] px-4 py-4">
        <div className="flex-1 rounded-2xl border border-[#0070CD]/20 bg-white shadow-sm overflow-hidden">
          <ChatWithAvatar />
        </div>
      </div>
    </div>
  )
}
