/**
 * Generates all demo try-on results in parallel using Replicate IDM-VTON.
 * Run once before the demo:  node scripts/generateDemoResults.mjs
 *
 * Output: updates src/data/demoTryOn.ts with real result URLs.
 */

import fs from "fs";
import path from "path";
import https from "https";

const REPLICATE_TOKEN = process.env.REPLICATE_TOKEN;

// ── Same data as demoTryOn.ts ─────────────────────────────────────────────────
// Local file paths (relative to frontend/public/)
const PERSONS = [
  { id: "p1", photo: path.resolve("public/demo/persons/person1.jpg") },
  { id: "p2", photo: path.resolve("public/demo/persons/person2.jpg") },
  { id: "p3", photo: path.resolve("public/demo/persons/person3.jpg") },
];

const OUTFITS = [
  { id: "o1", photo: path.resolve("public/demo/outfits/outfit1.png"), category: "upper_body", desc: "outfit 1" },
  { id: "o2", photo: path.resolve("public/demo/outfits/outfit2.png"), category: "upper_body", desc: "outfit 2" },
  { id: "o3", photo: path.resolve("public/demo/outfits/outfit3.png"), category: "dresses",    desc: "outfit 3" },
  { id: "o4", photo: path.resolve("public/demo/outfits/outfit4.png"), category: "upper_body", desc: "outfit 4" },
];

// ─────────────────────────────────────────────────────────────────────────────

async function fetchJson(url, options = {}) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, {
      ...options,
      headers: {
        Authorization: `Bearer ${REPLICATE_TOKEN}`,
        "Content-Type": "application/json",
        ...(options.headers || {}),
      },
    }, (res) => {
      let data = "";
      res.on("data", chunk => data += chunk);
      res.on("end", () => {
        try { resolve(JSON.parse(data)); }
        catch { reject(new Error("Invalid JSON: " + data)); }
      });
    });
    req.on("error", reject);
    if (options.body) req.write(options.body);
    req.end();
  });
}

function fileToDataUri(filePath) {
  const buf  = fs.readFileSync(filePath);
  const ext  = path.extname(filePath).toLowerCase();
  const mime = ext === ".png" ? "image/png" : ext === ".webp" ? "image/webp" : "image/jpeg";
  return `data:${mime};base64,${buf.toString("base64")}`;
}

async function startPrediction(personUrl, garmentUrl, category, desc) {
  const [humanDataUri, garmDataUri] = [
    fileToDataUri(personUrl),
    fileToDataUri(garmentUrl),
  ];

  const body = JSON.stringify({
    input: {
      human_img:       humanDataUri,
      garm_img:        garmDataUri,
      garment_des:     desc,
      is_checked:      true,
      is_checked_crop: false,
      denoise_steps:   30,
      seed:            42,
      category,
    },
  });

  const data = await fetchJson("api.replicate.com", {
    method: "POST",
    path: "/v1/models/cuuupid/idm-vton/predictions",
    headers: { Prefer: "wait=30" },
    body,
  });

  return data;
}

// Proper https.request wrapper that takes a full URL string
async function replicatePost(urlPath, bodyObj) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(bodyObj);
    const options = {
      hostname: "api.replicate.com",
      path: urlPath,
      method: "POST",
      headers: {
        Authorization: `Bearer ${REPLICATE_TOKEN}`,
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(body),
        Prefer: "wait=30",
      },
    };
    const req = https.request(options, res => {
      let d = "";
      res.on("data", c => d += c);
      res.on("end", () => { try { resolve(JSON.parse(d)); } catch { reject(new Error(d)); } });
    });
    req.on("error", reject);
    req.write(body);
    req.end();
  });
}

async function replicateGet(urlPath) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "api.replicate.com",
      path: urlPath,
      method: "GET",
      headers: { Authorization: `Bearer ${REPLICATE_TOKEN}` },
    };
    const req = https.request(options, res => {
      let d = "";
      res.on("data", c => d += c);
      res.on("end", () => { try { resolve(JSON.parse(d)); } catch { reject(new Error(d)); } });
    });
    req.on("error", reject);
    req.end();
  });
}

async function poll(id, label) {
  const deadline = Date.now() + 5 * 60 * 1000;
  while (Date.now() < deadline) {
    await new Promise(r => setTimeout(r, 6000));
    const data = await replicateGet(`/v1/predictions/${id}`);
    if (data.status === "succeeded") {
      const out = data.output;
      const url = Array.isArray(out) ? out[0] : out;
      console.log(`  ✓ ${label}: ${url}`);
      return url;
    }
    if (data.status === "failed" || data.status === "canceled") {
      throw new Error(`${label} ${data.status}: ${data.error}`);
    }
    process.stdout.write(`  ⏳ ${label} (${data.status})…\r`);
  }
  throw new Error(`${label} timed out`);
}

async function generateOne(personId, personUrl, outfitId, garmentUrl, category, desc) {
  const label = `${personId}_${outfitId}`;
  console.log(`  → Starting ${label}`);
  const [humanDataUri, garmDataUri] = [
    fileToDataUri(personUrl),
    fileToDataUri(garmentUrl),
  ];

  const prediction = await replicatePost("/v1/models/cuuupid/idm-vton/predictions", {
    input: {
      human_img:       humanDataUri,
      garm_img:        garmDataUri,
      garment_des:     desc,
      is_checked:      true,
      is_checked_crop: false,
      denoise_steps:   30,
      seed:            42,
      category,
    },
  });

  if (prediction.error) throw new Error(`${label}: ${prediction.error}`);
  if (prediction.status === "succeeded") {
    const out = prediction.output;
    const url = Array.isArray(out) ? out[0] : out;
    console.log(`  ✓ ${label} (instant): ${url}`);
    return { key: label, url };
  }

  const url = await poll(prediction.id, label);
  return { key: label, url };
}

// ── Main ──────────────────────────────────────────────────────────────────────
async function main() {
  // Only generate for person1 × all 4 outfits = 4 jobs (~3-4 min total)
  // person2 and person3 will reuse person1's results in the demo
  const person = PERSONS[0];
  console.log(`🚀 Generating 4 try-on results for ${person.id} sequentially…\n`);

  const resultMap = {};
  for (const outfit of OUTFITS) {
    try {
      const result = await generateOne(
        person.id, person.photo,
        outfit.id, outfit.photo,
        outfit.category, outfit.desc,
      );
      resultMap[result.key] = result.url;
      // Reuse same result for person2 and person3 (demo shortcut)
      resultMap[`p2_${outfit.id}`] = result.url;
      resultMap[`p3_${outfit.id}`] = result.url;
    } catch (err) {
      console.error(`  ✗ Failed p1_${outfit.id}:`, err.message);
    }
  }

  console.log("\n✅ Done! Updating demoTryOn.ts…\n");

  // Build the DEMO_RESULTS block
  const lines = Object.keys(resultMap).length > 0
    ? Object.entries(resultMap)
        .map(([k, v]) => `  ${k}: "${v}",`)
        .join("\n")
    : "  // No results generated";

  // Read and patch the file
  const filePath = path.resolve("src/data/demoTryOn.ts");
  let src = fs.readFileSync(filePath, "utf8");
  src = src.replace(
    /export const DEMO_RESULTS[\s\S]*?^};/m,
    `export const DEMO_RESULTS: Record<string, string | null> = {\n${lines}\n};`
  );
  fs.writeFileSync(filePath, src, "utf8");

  console.log("📝 demoTryOn.ts updated with result URLs:");
  Object.entries(resultMap).forEach(([k, v]) => console.log(`  ${k}: ${v}`));
  console.log("\nRestart npm run dev and the demo will show results instantly!");
}

main().catch(err => { console.error("Fatal:", err); process.exit(1); });
