/**
 * Pre-generated Virtual Try-On demo data.
 *
 * HOW TO UPDATE RESULTS:
 *  1. Call Fashn.ai (or any try-on API) once per person+outfit pair.
 *  2. Copy the returned image URL into RESULTS[personId_outfitId].
 *  3. That's it — the UI shows it instantly.
 */

export interface DemoPerson {
  id: string;
  name: string;
  photo: string;   // URL to person photo
}

export interface DemoOutfit {
  id: string;
  name: string;
  category: "upper_body" | "lower_body" | "dresses";
  photo: string;   // URL to garment photo
  description: string;
}

// ── Sample people ─────────────────────────────────────────────────────────────
const ASSET_BASE = import.meta.env.BASE_URL;

// Place your photos in: frontend/public/demo/persons/
export const DEMO_PERSONS: DemoPerson[] = [
  { id: "p1", name: "Person 1", photo: `${ASSET_BASE}demo/persons/person1.jpg` },
  { id: "p2", name: "Person 2", photo: `${ASSET_BASE}demo/persons/person2.jpg` },
  { id: "p3", name: "Person 3", photo: `${ASSET_BASE}demo/persons/person3.jpg` },
];

// ── Sample outfits ────────────────────────────────────────────────────────────
// Place your photos in:  frontend/public/demo/outfits/
export const DEMO_OUTFITS: DemoOutfit[] = [
  {
    id: "o1",
    name: "Outfit 1",
    category: "upper_body",
    description: "Outfit 1",
    photo: `${ASSET_BASE}demo/outfits/outfit1.png`,
  },
  {
    id: "o2",
    name: "Outfit 2",
    category: "upper_body",
    description: "Outfit 2",
    photo: `${ASSET_BASE}demo/outfits/outfit2.png`,
  },
  {
    id: "o3",
    name: "Outfit 3",
    category: "dresses",
    description: "Outfit 3",
    photo: `${ASSET_BASE}demo/outfits/outfit3.png`,
  },
  {
    id: "o4",
    name: "Outfit 4",
    category: "upper_body",
    description: "Outfit 4",
    photo: `${ASSET_BASE}demo/outfits/outfit4.png`,
  },
];

/**
 * Pre-generated try-on results.
 * Key format: `${personId}_${outfitId}`
 *
 * Replace null with the real result URL after running generation.
 * While null, the UI shows a "Generating preview…" placeholder.
 */
// Place generated result images in: frontend/public/demo/results/
// Filename format: result_p1_o1.jpg  (personId _ outfitId)
// All person+outfit combos using same person1 results (demo shortcut)
export const DEMO_RESULTS: Record<string, string | null> = {
  p1_o1: `${ASSET_BASE}demo/results/result_p1_o1.jpg`,
  p1_o2: `${ASSET_BASE}demo/results/result_p1_o2.png`,
  p1_o3: `${ASSET_BASE}demo/results/result_p1_o3.jpg`,
  p1_o4: `${ASSET_BASE}demo/results/result_p1_o4.jpg`,
  p2_o1: `${ASSET_BASE}demo/results/result_p2_o1.png`,
  p2_o2: `${ASSET_BASE}demo/results/result_p1_o2.jpg`,
  p2_o3: `${ASSET_BASE}demo/results/result_p1_o3.jpg`,
  p2_o4: `${ASSET_BASE}demo/results/result_p1_o4.jpg`,
  p3_o1: `${ASSET_BASE}demo/results/result_p1_o1.jpg`,
  p3_o2: `${ASSET_BASE}demo/results/result_p1_o2.jpg`,
  p3_o3: `${ASSET_BASE}demo/results/result_p3_o3.png`,
  p3_o4: `${ASSET_BASE}demo/results/result_p1_o4.jpg`,
};
