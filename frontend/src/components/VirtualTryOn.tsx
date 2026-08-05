import React, { useState } from "react";
import CheckroomIcon from "@mui/icons-material/Checkroom";
import AutoFixHighIcon from "@mui/icons-material/AutoFixHigh";
import DownloadIcon from "@mui/icons-material/Download";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import { DEMO_PERSONS, DEMO_OUTFITS, DEMO_RESULTS } from "../data/demoTryOn";
import type { DemoPerson, DemoOutfit } from "../data/demoTryOn";

export function VirtualTryOn() {
  const [selectedPerson, setSelectedPerson] = useState<DemoPerson | null>(null);
  const [selectedOutfit, setSelectedOutfit] = useState<DemoOutfit | null>(null);

  const resultKey    = selectedPerson && selectedOutfit
    ? `${selectedPerson.id}_${selectedOutfit.id}` : null;
  const resultUrl    = resultKey ? DEMO_RESULTS[resultKey] ?? null : null;
  const hasBothSelected = Boolean(selectedPerson && selectedOutfit);

  const reset = () => { setSelectedPerson(null); setSelectedOutfit(null); };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 to-blue-50 py-8 px-4">

      {/* ── Header ── */}
      <div className="mx-auto max-w-6xl mb-8">
        <h1 className="text-3xl font-bold text-slate-800">
          AI Virtual <span className="text-[#0070CD]">Try-On</span>
        </h1>
        <p className="text-slate-500 text-sm mt-1">
          Select a person and an outfit — see the AI-generated result instantly
        </p>
      </div>

      <div className="mx-auto max-w-6xl grid grid-cols-1 lg:grid-cols-[1fr_1fr_1fr] gap-6">

        {/* ── Step 1: Choose Person ── */}
        <StepCard step={1} title="Choose a Person" subtitle="Select the model to try the outfit on">
          <div className="grid grid-cols-3 gap-3">
            {DEMO_PERSONS.map(person => (
              <button
                key={person.id}
                onClick={() => setSelectedPerson(person)}
                className={`relative rounded-xl overflow-hidden aspect-[3/4] transition-all duration-200
                  ${selectedPerson?.id === person.id
                    ? "ring-4 ring-[#0070CD] scale-[1.03] shadow-xl"
                    : "ring-1 ring-slate-200 hover:scale-[1.02] hover:shadow-md opacity-80 hover:opacity-100"
                  }`}
              >
                <img
                  src={person.photo}
                  alt={person.name}
                  className="w-full h-full object-cover"
                  onError={e => {
                    const t = e.target as HTMLImageElement;
                    t.onerror = null;
                    t.src = `https://placehold.co/200x280/e2e8f0/475569?text=${encodeURIComponent(person.name)}`;
                  }}
                />
                {selectedPerson?.id === person.id && (
                  <div className="absolute top-2 right-2 bg-[#0070CD] text-white rounded-full w-6 h-6 flex items-center justify-center text-xs font-bold shadow">
                    ✓
                  </div>
                )}
                <div className="absolute bottom-0 left-0 right-0 bg-black/50 px-2 py-1 text-white text-xs font-medium text-center">
                  {person.name}
                </div>
              </button>
            ))}
          </div>
          {selectedPerson && (
            <p className="mt-3 text-xs text-emerald-600 font-medium text-center">
              ✓ {selectedPerson.name} selected
            </p>
          )}
        </StepCard>

        {/* ── Step 2: Choose Outfit ── */}
        <StepCard step={2} title="Choose an Outfit" subtitle="Pick a garment to virtually try on">
          <div className="grid grid-cols-2 gap-3">
            {DEMO_OUTFITS.map(outfit => (
              <button
                key={outfit.id}
                onClick={() => setSelectedOutfit(outfit)}
                className={`relative rounded-xl overflow-hidden aspect-[3/4] flex flex-col transition-all duration-200 bg-white border
                  ${selectedOutfit?.id === outfit.id
                    ? "ring-4 ring-[#0070CD] scale-[1.03] shadow-xl border-[#0070CD]"
                    : "ring-1 ring-slate-200 hover:scale-[1.02] hover:shadow-md border-slate-200 opacity-80 hover:opacity-100"
                  }`}
              >
                <div className="flex-1 overflow-hidden">
                  <img
                    src={outfit.photo}
                    alt={outfit.name}
                    className="w-full h-full object-cover"
                    onError={e => {
                      const t = e.target as HTMLImageElement;
                      t.onerror = null;
                      t.src = `https://placehold.co/200x240/e2e8f0/475569?text=${encodeURIComponent(outfit.name)}`;
                    }}
                  />
                </div>
                {selectedOutfit?.id === outfit.id && (
                  <div className="absolute top-2 right-2 bg-[#0070CD] text-white rounded-full w-6 h-6 flex items-center justify-center text-xs font-bold shadow">
                    ✓
                  </div>
                )}
                <div className="bg-white px-2 py-1.5 text-left">
                  <p className="text-xs font-semibold text-slate-700 leading-tight">{outfit.name}</p>
                  <p className="text-xs text-slate-400">{outfit.category.replace("_", " ")}</p>
                </div>
              </button>
            ))}
          </div>
          {selectedOutfit && (
            <p className="mt-3 text-xs text-emerald-600 font-medium text-center">
              ✓ {selectedOutfit.name} selected
            </p>
          )}
        </StepCard>

        {/* ── Step 3: Result ── */}
        <StepCard step={3} title="Your Look" subtitle="AI-generated virtual try-on result">

          {hasBothSelected && resultUrl ? (
            /* ── Real result ── */
            <div className="flex flex-col gap-3">
              <div className="rounded-xl overflow-hidden aspect-[3/4] bg-slate-100 shadow-md">
                <img
                  src={resultUrl}
                  alt="Try-on result"
                  className="w-full h-full object-cover"
                  onError={e => {
                    const t = e.target as HTMLImageElement;
                    t.onerror = null;
                    // Result image not placed yet — show instructions
                    t.style.display = "none";
                    t.parentElement!.innerHTML = `
                      <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;gap:12px;padding:16px;text-align:center;color:#64748b;">
                        <div style="font-size:40px">📁</div>
                        <p style="font-size:13px;font-weight:600;">Place result image here:</p>
                        <code style="font-size:11px;background:#f1f5f9;padding:6px 10px;border-radius:6px;word-break:break-all;">
                          public/demo/results/${resultUrl.split("/").pop()}
                        </code>
                        <p style="font-size:11px;">Generate free at<br/><strong>huggingface.co/spaces/yisol/IDM-VTON</strong></p>
                      </div>`;
                  }}
                />
              </div>
              <p className="text-xs text-emerald-600 font-semibold text-center">
                ✓ {selectedPerson!.name} wearing {selectedOutfit!.name}
              </p>
              <div className="flex gap-2">
                <a
                  href={resultUrl}
                  download="tryon-result.jpg"
                  target="_blank"
                  rel="noreferrer"
                  className="flex-1 flex items-center justify-center gap-1.5 bg-[#0070CD] hover:bg-[#005fa8] text-white text-sm font-semibold rounded-xl py-2.5 transition"
                >
                  <DownloadIcon fontSize="small" /> Download
                </a>
                <button
                  onClick={reset}
                  className="flex-1 flex items-center justify-center gap-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-semibold rounded-xl py-2.5 transition"
                >
                  <RestartAltIcon fontSize="small" /> Try Another
                </button>
              </div>
            </div>

          ) : hasBothSelected && resultUrl === null ? (
            /* ── Combination selected but no result generated yet ── */
            <div className="rounded-xl aspect-[3/4] bg-amber-50 border-2 border-dashed border-amber-300 flex flex-col items-center justify-center gap-3 px-4 text-center">
              <AutoFixHighIcon style={{ fontSize: 52, color: "#f59e0b" }} />
              <p className="text-sm font-semibold text-amber-700">
                Result not generated yet
              </p>
              <p className="text-xs text-amber-600">
                Run the generation script and add the result URL to <code className="bg-amber-100 px-1 rounded">demoTryOn.ts</code>
              </p>
              <div className="mt-2 text-xs text-slate-500 bg-white rounded-lg px-3 py-2 border border-slate-200 text-left w-full">
                <p className="font-semibold mb-1">Key to fill in:</p>
                <code className="text-[#0070CD]">{`${selectedPerson!.id}_${selectedOutfit!.id}`}</code>
              </div>
            </div>

          ) : (
            /* ── Nothing selected yet ── */
            <div className="rounded-xl aspect-[3/4] bg-slate-100 border-2 border-dashed border-slate-300 flex flex-col items-center justify-center gap-3 px-4 text-center text-slate-400">
              <CheckroomIcon style={{ fontSize: 52 }} />
              <p className="text-sm">
                Select a <strong>person</strong> and an <strong>outfit</strong><br />
                to see the result instantly
              </p>
              {selectedPerson && !selectedOutfit && (
                <p className="text-xs text-[#0070CD] font-medium animate-pulse">
                  Now pick an outfit →
                </p>
              )}
              {!selectedPerson && (
                <p className="text-xs text-[#0070CD] font-medium animate-pulse">
                  ← Start by selecting a person
                </p>
              )}
            </div>
          )}

        </StepCard>
      </div>

      {/* ── Before / After comparison (shown when result is ready) ── */}
      {hasBothSelected && resultUrl && (
        <div className="mx-auto max-w-6xl mt-8">
          <h2 className="text-lg font-bold text-slate-700 mb-4 text-center">Before vs After</h2>
          <div className="grid grid-cols-3 gap-4">
            <CompareCard label="Person" src={selectedPerson!.photo} />
            <CompareCard label="Outfit" src={selectedOutfit!.photo} objectFit="contain" />
            <CompareCard label="Result" src={resultUrl} highlight />
          </div>
        </div>
      )}

    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function StepCard({ step, title, subtitle, children }: {
  step: number;
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-white rounded-2xl shadow-md border border-slate-200 p-5">
      <div className="flex items-center gap-3 mb-4">
        <span className="flex-shrink-0 w-8 h-8 rounded-full bg-[#0070CD] text-white font-bold text-sm flex items-center justify-center shadow">
          {step}
        </span>
        <div>
          <h2 className="font-bold text-slate-800 leading-tight">{title}</h2>
          <p className="text-xs text-slate-400">{subtitle}</p>
        </div>
      </div>
      {children}
    </div>
  );
}

function CompareCard({ label, src, objectFit = "cover", highlight = false }: {
  label: string;
  src: string;
  objectFit?: "cover" | "contain";
  highlight?: boolean;
}) {
  return (
    <div className={`rounded-2xl overflow-hidden shadow-md border-2 ${highlight ? "border-[#0070CD] shadow-[#0070CD]/20 shadow-lg" : "border-slate-200"}`}>
      <div className="bg-slate-50 px-3 py-2 border-b border-slate-200 flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-600">{label}</span>
        {highlight && <span className="text-xs bg-[#0070CD] text-white rounded-full px-2 py-0.5 font-semibold">AI Result</span>}
      </div>
      <div className="aspect-[3/4] bg-slate-100">
        <img src={src} alt={label} className={`w-full h-full object-${objectFit}`} />
      </div>
    </div>
  );
}
