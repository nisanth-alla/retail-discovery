import { createContext, useContext, useState, type ReactNode } from "react";
import type { ChatItem } from "../components/ChatWithAvatar";

const WELCOME: ChatItem = {
  role: "avatar",
  text: "Hey there! 👋 I'm your personal AI stylist. Ask me about outfit ideas, styling tips, or upload a photo to find similar looks!",
  time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
};

interface ChatContextValue {
  conversation: ChatItem[];
  setConversation: React.Dispatch<React.SetStateAction<ChatItem[]>>;
}

const ChatContext = createContext<ChatContextValue | null>(null);

export function ChatProvider({ children }: { children: ReactNode }) {
  const [conversation, setConversation] = useState<ChatItem[]>([WELCOME]);
  return (
    <ChatContext.Provider value={{ conversation, setConversation }}>
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error("useChat must be used within ChatProvider");
  return ctx;
}
