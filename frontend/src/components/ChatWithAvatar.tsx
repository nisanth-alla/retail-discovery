import { useEffect, useState, useRef, useCallback } from "react";
import { Avatar2D } from "./Avatar3D";
import { useSearchImage } from "../context/SearchImageContext";
import { useChat } from "../context/ChatContext";
import type { Product } from "../types/product";
import { generateStylistResponse } from "../utils/aiStylist";
import {
  API_BASE,
  searchCatalogByText,
  sendFashionChat,
  styleItProducts,
  searchByLabel,
  type ChatHistoryMessage,
  type StyleItItem,
} from "../services/api";
import VolumeUpIcon from "@mui/icons-material/VolumeUp";
import VolumeOffIcon from "@mui/icons-material/VolumeOff";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import MicIcon from "@mui/icons-material/Mic";
import SendIcon from "@mui/icons-material/Send";
import CloseIcon from "@mui/icons-material/Close";

declare global {
  interface Window {
    webkitSpeechRecognition?: any;
    SpeechRecognition?: any;
  }
}

export type ChatItem =
  | { role: "user"; text?: string; image?: string; time: string }
  | { role: "avatar"; text: string; products?: Product[]; styledItems?: Record<string, StyleItItem[]>; time: string };

type UserProfile = { occasion: string; age: string; gender: string };

const ONBOARDING_QUESTIONS = [
  "Hi! I'm your AI Stylist. Before we dive in, a couple of quick questions to personalise your experience. What occasion are you shopping for? (e.g. casual, wedding, office, date night, party)",
  "What's your age group? (e.g. teens, 20s, 30s, 40s+)",
  "And your gender? (Male / Female / Other / Prefer not to say)",
];

const CLOTHING_CATEGORIES = [
  "shirt", "pants", "jeans", "jacket", "dress", "kurta", "glasses", "shoes",
  "blazer", "top", "skirt", "kurti", "shorts", "suit", "coat", "wallet", "bag", "watch",
];

const CLOTHING_SYNONYMS: Record<string, string> = {
  trousers: "pants", tee: "shirt", tshirt: "shirt",
  sneakers: "shoes", sandals: "shoes", hoodie: "jacket", saree: "dress",
};

const STYLE_IT_INTENT = /style\s*(me|this|it|up)|what\s+goes\s+with|pair\s+with|goes?\s+well\s+with|outfit\s*(ideas?|for|suggest\w*)|complete\s+the\s+look|what\s+(should\s+i\s+wear|to\s+wear)/i;
const PRICE_INTENT = /(?:under|below|less\s+than|budget|within|max|upto|up\s+to)\s*[₹rs.]?\s*(\d+)/i;

function getTime() {
  return new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function normalizeImagePath(imagePath: string): string {
  if (!imagePath) return "";
  const withSlashes = imagePath.replace(/\\/g, "/");
  try { return new URL(withSlashes).pathname; } catch { return withSlashes; }
}

function extractClothingKeywords(input: string): string[] {
  const lower = input.toLowerCase();
  const matches: string[] = [];
  CLOTHING_CATEGORIES.forEach(cat => { if (lower.includes(cat)) matches.push(cat); });
  Object.entries(CLOTHING_SYNONYMS).forEach(([word, mapped]) => {
    if (lower.includes(word) && !matches.includes(mapped)) matches.push(mapped);
  });
  return matches;
}

function extractPrice(text: string): number | null {
  const match = text.match(PRICE_INTENT);
  return match ? parseInt(match[1], 10) : null;
}

function formatProduct(p: Product): string {
  const parts = [p.name];
  if (p.brand) parts.push(`by ${p.brand}`);
  if (p.price != null) parts.push(`₹${p.price.toLocaleString("en-IN")}`);
  if (p.score) parts.push(`${Math.round(p.score * 100)}% match`);
  return parts.join(" — ");
}

function buildImageSearchPrompt(products: Product[], userMessage: string): string {
  const list = products.map(p => `• ${formatProduct(p)}`).join("\n");
  return [
    "The user uploaded an image. Our visual search system (not you) scanned it and found these specific matching products:",
    list,
    "",
    `User's message: "${userMessage}"`,
    "",
    "Give a short, friendly response that acknowledges these exact results. Only reference products from the list above — do not suggest or mention any others.",
  ].join("\n");
}

function buildStyleItPrompt(grouped: Record<string, StyleItItem[]>, userMessage: string): string {
  const sections = Object.entries(grouped)
    .map(([style, items]) => `${style}: ${items.map(i => i.filename).join(", ")}`)
    .join("\n");
  return [
    "The user wants styling suggestions. Our style API returned these outfit pairings:",
    sections,
    "",
    `User's message: "${userMessage}"`,
    "",
    "Give a brief, enthusiastic intro to these specific suggestions only. Do not invent or suggest additional items.",
  ].join("\n");
}

function buildPriceSearchPrompt(products: Product[], price: number, userMessage: string): string {
  if (!products.length) {
    return `The user asked for items under ₹${price.toLocaleString("en-IN")} but our search found no matching products. Politely let them know and suggest they try a different budget or item type.`;
  }
  const list = products.map(p => `• ${formatProduct(p)}`).join("\n");
  return [
    `The user wants items under ₹${price.toLocaleString("en-IN")}. Our search returned:`,
    list,
    "",
    `User's message: "${userMessage}"`,
    "",
    "Give a brief, friendly response presenting these specific results only.",
  ].join("\n");
}

function groupByStyle(items: StyleItItem[]): Record<string, StyleItItem[]> {
  return items
    .filter(item => item.style !== "error")
    .reduce<Record<string, StyleItItem[]>>((acc, item) => {
      (acc[item.style] ??= []).push(item);
      return acc;
    }, {});
}

async function fetchProductsByText(query: string): Promise<Product[]> {
  try {
    const res = await searchCatalogByText(query);
    return (res.images ?? []).map(img => ({
      imagePath: `${API_BASE}${img.url}`,
      detectedLabels: [],
      score: img.score != null ? parseFloat(String(img.score)) / 100 : 0,
      productId: null,
      name: img.filename,
      brand: img.brand ?? null,
      price: null,
    }));
  } catch {
    return [];
  }
}

function StyleItTabs({ grouped }: { grouped: Record<string, StyleItItem[]> }) {
  const tabs = Object.keys(grouped);
  const [active, setActive] = useState(tabs[0] ?? "");
  if (!tabs.length) return null;
  return (
    <div className="w-full mt-1">
      <p className="mb-1.5 pl-1 text-[10px] font-semibold uppercase tracking-wider text-slate-400">Style It</p>
      <div className="flex flex-wrap gap-1 mb-2">
        {tabs.map(tab => (
          <button
            key={tab}
            type="button"
            onClick={() => setActive(tab)}
            className={`rounded-full px-2.5 py-1 text-[11px] font-medium capitalize transition-colors ${
              active === tab
                ? "bg-[#0070CD] text-white"
                : "bg-slate-100 text-slate-500 hover:bg-[#0070CD]/10 hover:text-[#0070CD]"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>
      <div className="flex gap-2 overflow-x-auto pb-1" style={{ scrollbarWidth: "none" }}>
        {(grouped[active] ?? []).map((item, i) => (
          <div
            key={i}
            className="flex-shrink-0 w-28 rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm hover:shadow-md transition-shadow"
          >
            <div className="h-28 w-full overflow-hidden bg-gradient-to-br from-[#e6f0fa] to-[#f5faff]">
              <img
                src={`${API_BASE}${item.url}`}
                alt={item.filename}
                className="h-full w-full object-contain"
                onError={e => { (e.currentTarget as HTMLImageElement).src = "https://via.placeholder.com/112?text=No+Image"; }}
              />
            </div>
            <div className="p-1.5">
              <p className="text-[10px] font-semibold text-slate-700 leading-tight truncate">{item.filename}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function useVoiceInput(setInput: React.Dispatch<React.SetStateAction<string>>) {
  const [listening, setListening] = useState(false);
  const [speechError, setSpeechError] = useState<string | null>(null);
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition || null;

  const startVoiceRecognition = () => {
    if (window.speechSynthesis?.speaking) window.speechSynthesis.cancel();
    if (!SpeechRecognition) { setSpeechError("Voice input not supported."); return; }
    setSpeechError(null);
    const recognition = new SpeechRecognition();
    recognition.lang = "en-US";
    recognition.interimResults = false;
    recognition.continuous = false;
    recognition.maxAlternatives = 1;
    recognition.onstart = () => setListening(true);
    recognition.onend = () => setListening(false);
    recognition.onresult = (event: any) => {
      const result = event.results?.[event.resultIndex];
      if (result?.isFinal) {
        const transcript = result[0].transcript.trim();
        if (transcript) setInput(prev => prev ? `${prev} ${transcript}` : transcript);
      }
    };
    recognition.onerror = () => { setSpeechError("Voice input failed."); setListening(false); };
    setTimeout(() => recognition.start(), 50);
  };

  return { listening, speechError, startVoiceRecognition };
}

export function ChatWithAvatar() {
  const [input, setInput] = useState("");
  const { conversation, setConversation } = useChat();
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [muted, setMuted] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [onboardingStep, setOnboardingStep] = useState(0);
  const [onboardingProfile, setOnboardingProfile] = useState<Partial<UserProfile>>({});
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);

  const mutedRef = useRef(false);
  const pendingTextRef = useRef("");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const lastImageFileRef = useRef<File | null>(null);
  const lastLabelsRef = useRef<string[]>([]);

  const { imageFile, previewUrl, setImage, removeImage, search, hasImage } = useSearchImage();
  const { listening, speechError, startVoiceRecognition } = useVoiceInput(setInput);

  useEffect(() => {
    removeImage();
    if (conversation.length === 0) {
      setConversation([{ role: "avatar", text: ONBOARDING_QUESTIONS[0], time: getTime() }]);
      setOnboardingStep(1);
    } else {
      setOnboardingStep(4);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [conversation, isTyping]);

  const speak = useCallback((text: string) => {
    if (!("speechSynthesis" in window) || !text) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-US";
    utterance.rate = 1;
    utterance.pitch = 1.05;
    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);
    window.speechSynthesis.speak(utterance);
  }, []);

  const toggleMute = useCallback(() => {
    const nowMuted = !mutedRef.current;
    mutedRef.current = nowMuted;
    setMuted(nowMuted);
    if (nowMuted) {
      window.speechSynthesis?.cancel();
      setIsSpeaking(false);
    } else if (pendingTextRef.current) {
      speak(pendingTextRef.current);
      pendingTextRef.current = "";
    }
  }, [speak]);

  const pushAvatar = useCallback((
    text: string,
    products?: Product[],
    styledItems?: Record<string, StyleItItem[]>,
  ) => {
    setConversation(prev => [...prev, { role: "avatar", text, products, styledItems, time: getTime() }]);
    if (mutedRef.current) {
      pendingTextRef.current = text;
    } else {
      pendingTextRef.current = "";
      speak(text);
    }
  }, [setConversation, speak]);

  const buildHistory = useCallback((snap: typeof conversation): ChatHistoryMessage[] =>
    snap.flatMap((item): ChatHistoryMessage[] => {
      if (item.role === "user" && item.text) return [{ role: "user", content: item.text }];
      if (item.role === "avatar") return [{ role: "assistant", content: item.text }];
      return [];
    }),
  []);

  const handleSubmit = useCallback(async (e: { preventDefault(): void }) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed && !hasImage) return;

    if (onboardingStep === 1) {
      setConversation(prev => [...prev,
        { role: "user", text: trimmed, time: getTime() },
        { role: "avatar", text: ONBOARDING_QUESTIONS[1], time: getTime() },
      ]);
      setInput(""); setOnboardingProfile({ occasion: trimmed }); setOnboardingStep(2); return;
    }
    if (onboardingStep === 2) {
      setConversation(prev => [...prev,
        { role: "user", text: trimmed, time: getTime() },
        { role: "avatar", text: ONBOARDING_QUESTIONS[2], time: getTime() },
      ]);
      setInput(""); setOnboardingProfile(prev => ({ ...prev, age: trimmed })); setOnboardingStep(3); return;
    }
    if (onboardingStep === 3) {
      const finalProfile: UserProfile = {
        occasion: onboardingProfile.occasion ?? "casual",
        age: onboardingProfile.age ?? "unspecified",
        gender: trimmed,
      };
      setConversation(prev => [...prev,
        { role: "user", text: trimmed, time: getTime() },
        { role: "avatar", text: `All set! I'll keep that in mind — styling for a ${finalProfile.occasion} vibe, tailored just for you. What are you looking for today?`, time: getTime() },
      ]);
      setInput(""); setUserProfile(finalProfile); setOnboardingStep(4); return;
    }

    const snapshotConversation = conversation;
    setConversation(prev => [...prev, { role: "user", text: trimmed, image: previewUrl ?? undefined, time: getTime() }]);
    setInput("");
    setIsTyping(true);

    if (!hasImage && /^\s*(hi|hey|hello)\s*[!.]?\s*$/i.test(trimmed)) {
      await new Promise(r => setTimeout(r, 600));
      setIsTyping(false);
      pushAvatar("Hey! Great to see you. What are we styling today? Ask me anything about fashion, or upload a photo to find similar looks!");
      return;
    }

    const history = buildHistory(snapshotConversation);
    const userContext = userProfile
      ? `Occasion: ${userProfile.occasion}, Age group: ${userProfile.age}, Gender: ${userProfile.gender}`
      : undefined;

    const currentImageFile = imageFile;

    // === PATH 1: Style It ===
    if (STYLE_IT_INTENT.test(trimmed)) {
      const imageToUse = currentImageFile ?? lastImageFileRef.current;
      const hasKeyword = extractClothingKeywords(trimmed).length > 0;

      if (!imageToUse && !hasKeyword) {
        setIsTyping(false);
        pushAvatar("What item would you like to style? Describe it (e.g. 'style a blue blazer') or upload a photo!");
        return;
      }

      if (currentImageFile) {
        lastImageFileRef.current = currentImageFile;
        removeImage();
      }

      const usingPreviousImage = !currentImageFile && !!lastImageFileRef.current;

      const styleResult = await styleItProducts(trimmed || undefined, imageToUse ?? undefined).catch(() => null);

      const grouped = styleResult ? groupByStyle(styleResult.images) : {};
      const chatPrompt = Object.keys(grouped).length
        ? buildStyleItPrompt(grouped, usingPreviousImage ? `${trimmed} (based on previously uploaded image)` : trimmed)
        : trimmed;

      const chatResult = await sendFashionChat(chatPrompt, history, userContext).catch(() => null);

      setIsTyping(false);
      const reply = chatResult?.reply || "Here's how I'd style this look for you!";
      pushAvatar(reply, undefined, Object.keys(grouped).length ? grouped : undefined);
      return;
    }

    // === PATH 2: Search By Label + Price ===
    const priceLimit = extractPrice(trimmed);
    if (priceLimit !== null) {
      const imageToUse = currentImageFile ?? lastImageFileRef.current;

      if (!imageToUse) {
        setIsTyping(false);
        pushAvatar("To filter by price, I need an image to reference. Please upload a photo of the item you have in mind!");
        return;
      }

      if (currentImageFile) {
        lastImageFileRef.current = currentImageFile;
        removeImage();
      }

      const msgKeywords = extractClothingKeywords(trimmed);
      const labels = msgKeywords.length
        ? msgKeywords
        : lastLabelsRef.current.length
          ? lastLabelsRef.current
          : ["clothing"];

      const labelResult = await searchByLabel(labels, priceLimit, imageToUse).catch(() => [] as Product[]);

      const chatPrompt = buildPriceSearchPrompt(labelResult, priceLimit, trimmed);
      const chatResult = await sendFashionChat(chatPrompt, history, userContext).catch(() => null);

      setIsTyping(false);
      const reply = chatResult?.reply || `Here are items under ₹${priceLimit.toLocaleString("en-IN")}!`;
      pushAvatar(reply, labelResult.length ? labelResult : undefined);
      return;
    }

    let products: Product[] | undefined;

    if (hasImage) {
      try {
        const imageResults = await search();
        if (imageResults.length > 0) {
          const nonCropped = imageResults.filter(p => !p.imagePath.toLowerCase().includes("/cropped"));
          products = nonCropped.length ? nonCropped : imageResults;
          const labels = [...new Set(imageResults.flatMap(p => [...(p.detectedLabels ?? []), ...(p.name ? [p.name] : [])]))];
          lastLabelsRef.current = labels;
          if (currentImageFile) lastImageFileRef.current = currentImageFile;
        }
      } catch { /* fall through */ } finally { removeImage(); }
    }

    const chatPrompt = products?.length
      ? buildImageSearchPrompt(products, trimmed)
      : trimmed;

    const [beResult, textProductResult] = await Promise.allSettled([
      sendFashionChat(chatPrompt, history, userContext).then(r => r.reply),
      !hasImage && trimmed ? fetchProductsByText(trimmed) : Promise.resolve([] as Product[]),
    ]);

    setIsTyping(false);

    const textProducts = textProductResult.status === "fulfilled" ? textProductResult.value : [];
    const allProducts = [...(products ?? []), ...textProducts];
    const finalProducts = allProducts.length > 0 ? allProducts : undefined;

    if (beResult.status === "fulfilled" && beResult.value) {
      pushAvatar(beResult.value, finalProducts);
    } else {
      const fallback = finalProducts ?? [{ name: "outfit", imagePath: "", detectedLabels: [], score: 1, productId: null, brand: null, price: null }];
      pushAvatar(generateStylistResponse(fallback, trimmed), finalProducts);
    }
  }, [input, hasImage, imageFile, onboardingStep, onboardingProfile, conversation, previewUrl, userProfile, buildHistory, pushAvatar, search, removeImage, setConversation]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) setImage(file);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <div className="flex h-[calc(100vh-180px)] min-h-[560px] rounded-2xl overflow-hidden">
      <div className="flex w-64 flex-shrink-0 flex-col items-center gap-4 bg-gradient-to-b from-slate-900 to-indigo-950 px-5 py-5">
        <div className="flex w-full items-center justify-between">
          <span className="text-sm font-semibold text-white/90">AI Stylist</span>
          <button
            type="button"
            onClick={toggleMute}
            title={muted ? "Unmute" : "Mute"}
            className={`rounded-lg p-1.5 transition-colors ${
              muted ? "bg-red-500/20 text-red-400 hover:bg-red-500/30" : "bg-white/10 text-white/70 hover:bg-white/20"
            }`}
          >
            {muted ? <VolumeOffIcon fontSize="small" /> : <VolumeUpIcon fontSize="small" />}
          </button>
        </div>

        <div className={`w-full rounded-2xl ${isSpeaking ? "speaking-ring" : ""}`}>
          <Avatar2D isSpeaking={isSpeaking} />
        </div>

        {isSpeaking && (
          <div className="flex items-center justify-center gap-1">
            <span className="h-1.5 w-1.5 rounded-full bg-violet-400 dot-bounce" style={{ animationDelay: "0ms" }} />
            <span className="h-1.5 w-1.5 rounded-full bg-violet-400 dot-bounce" style={{ animationDelay: "150ms" }} />
            <span className="h-1.5 w-1.5 rounded-full bg-violet-400 dot-bounce" style={{ animationDelay: "300ms" }} />
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col bg-slate-50 min-w-0">
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
          {conversation.map((item, idx) => (
            <div key={idx} className="animate-message-in">
              {item.role === "user" && (
                <div className="flex flex-col items-end gap-1 w-full">
                  {item.text && (
                    <div className="max-w-[70%] rounded-2xl rounded-br-sm bg-gradient-to-br from-violet-600 to-indigo-600 px-4 py-2.5 text-sm text-white shadow-md leading-relaxed break-words">
                      {item.text}
                    </div>
                  )}
                  {item.image && (
                    <img
                      src={item.image}
                      alt="Uploaded"
                      className="h-40 w-40 rounded-2xl rounded-br-sm border-2 border-violet-200 object-cover shadow-md"
                    />
                  )}
                  <span className="pr-1 text-[10px] text-slate-400">{item.time}</span>
                </div>
              )}

              {item.role === "avatar" && (
                <div className="flex flex-col items-start gap-1.5 w-full">
                  <div className="max-w-[70%] rounded-2xl rounded-bl-sm border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 shadow-sm break-words">
                    <p className="whitespace-pre-line leading-relaxed">{item.text}</p>
                  </div>

                  {item.products && item.products.length > 0 && (
                    <div className="w-full">
                      <p className="mb-1.5 pl-1 text-[10px] font-semibold uppercase tracking-wider text-slate-400">
                        Similar Picks
                      </p>
                      <div className="flex gap-2.5 overflow-x-auto pb-1" style={{ scrollbarWidth: "none" }}>
                        {item.products.map((p, i) => (
                          <div
                            key={i}
                            className="flex-shrink-0 w-32 rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm hover:shadow-md transition-shadow cursor-pointer"
                          >
                            {p.imagePath ? (
                              <img
                                src={normalizeImagePath(p.imagePath)}
                                alt={p.name || "Product"}
                                className="h-32 w-full object-cover"
                                onError={e => {
                                  (e.currentTarget as HTMLImageElement).src = "https://via.placeholder.com/128x128?text=No+Image";
                                }}
                              />
                            ) : (
                              <div className="h-32 w-full bg-gradient-to-br from-[#e6f0fa] to-[#f5faff] flex items-center justify-center text-3xl">
                                👕
                              </div>
                            )}
                            <div className="p-2">
                              <p className="text-[11px] font-semibold text-slate-800 leading-tight truncate">{p.name}</p>
                              {p.brand && <p className="text-[10px] text-[#0070CD] truncate mt-0.5 font-medium uppercase tracking-wide">{p.brand}</p>}
                              {p.price != null && (
                                <p className="mt-1 text-xs font-bold text-[#0070CD]">
                                  ₹{p.price.toLocaleString("en-IN")}
                                </p>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {item.styledItems && <StyleItTabs grouped={item.styledItems} />}

                  <span className="pl-1 text-[10px] text-slate-400">{item.time}</span>
                </div>
              )}
            </div>
          ))}

          {isTyping && (
            <div className="animate-message-in flex items-end justify-start">
              <div className="rounded-2xl rounded-bl-sm border border-slate-100 bg-white px-4 py-3 shadow-sm">
                <div className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-slate-400 dot-bounce" style={{ animationDelay: "0ms" }} />
                  <span className="h-2 w-2 rounded-full bg-slate-400 dot-bounce" style={{ animationDelay: "150ms" }} />
                  <span className="h-2 w-2 rounded-full bg-slate-400 dot-bounce" style={{ animationDelay: "300ms" }} />
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="border-t border-slate-200 bg-white px-4 py-3">
          {speechError && <p className="mb-2 text-xs text-red-500">{speechError}</p>}
          <form onSubmit={handleSubmit}>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 transition-colors focus-within:border-violet-300 focus-within:bg-white focus-within:shadow-sm">
              {previewUrl && (
                <div className="relative flex-shrink-0">
                  <img src={previewUrl} alt="Preview" className="h-9 w-9 rounded-lg border border-slate-200 object-cover" />
                  <button
                    type="button"
                    onClick={removeImage}
                    className="absolute -right-1.5 -top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-white shadow"
                  >
                    <CloseIcon style={{ fontSize: 10 }} />
                  </button>
                </div>
              )}
              <input
                value={input}
                onChange={e => setInput(e.target.value)}
                placeholder="Ask me about fashion, outfits, trends…"
                className="min-w-0 flex-1 self-center bg-transparent text-sm text-slate-700 outline-none placeholder:text-slate-400"
                disabled={isTyping}
              />
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                id="imageUpload"
                className="hidden"
                onChange={handleFileChange}
              />
              <label
                htmlFor="imageUpload"
                className="flex h-8 w-8 flex-shrink-0 cursor-pointer items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-violet-50 hover:text-violet-600"
                title="Upload image"
              >
                <PhotoCameraIcon fontSize="small" />
              </label>
              <button
                type="button"
                onClick={startVoiceRecognition}
                disabled={isTyping}
                title={listening ? "Listening…" : "Voice input"}
                className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg transition-colors ${
                  listening ? "animate-pulse bg-red-50 text-red-500" : "text-slate-400 hover:bg-violet-50 hover:text-violet-600"
                }`}
              >
                <MicIcon fontSize="small" />
              </button>
              <button
                type="submit"
                disabled={isTyping || (!input.trim() && !hasImage)}
                className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 text-white shadow-sm transition-all hover:shadow-md hover:from-violet-700 hover:to-indigo-700 disabled:opacity-40"
                title="Send"
              >
                <SendIcon fontSize="small" />
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
