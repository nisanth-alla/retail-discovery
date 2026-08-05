import type { Product } from "../types/product";

export function generateStylistResponse(products: Product[], userInput?: string) {
  if (!products.length) {
    return "I couldn't find products, but here's a styling idea: keep it minimal and balanced.";
  }

  const name = products[0]?.name?.toLowerCase() || "";

  let type = "outfit";
  if (name.includes("shirt")) type = "shirt";
  else if (name.includes("dress")) type = "dress";
  else if (name.includes("jeans")) type = "jeans";
  else if (name.includes("jacket")) type = "jacket";
  else if (name.includes("kurta")) type = "ethnic";

  let style = "casual";
  if (name.includes("formal")) style = "formal";
  if (name.includes("party")) style = "party";
  if (name.includes("sport")) style = "sporty";

  const suggestions: Record<string, string[]> = {
    shirt: ["Pair with black or blue jeans", "Add white sneakers", "Layer with a denim jacket"],
    dress: ["Pair with heels or flats", "Add a sling bag", "Keep accessories minimal"],
    jeans: ["Match with a plain t-shirt or shirt", "Add sneakers or boots"],
    jacket: ["Layer over a t-shirt", "Pair with slim-fit jeans"],
    ethnic: ["Pair with churidar or jeans", "Add traditional footwear"],
    outfit: ["Keep colors balanced", "Add simple accessories"],
  };

  let occasionTip = "Great for everyday wear.";
  const lower = userInput?.toLowerCase() ?? "";
  if (lower.includes("party")) occasionTip = "Perfect for parties—add bold accessories!";
  else if (lower.includes("office")) occasionTip = "Works well for office—keep it clean and minimal.";
  else if (lower.includes("date")) occasionTip = "Nice choice for a date—keep it stylish but simple.";

  return `This looks like a ${style} ${type}.\nStyle it with:\n${suggestions[type].map(s => `• ${s}`).join("\n")}\n${occasionTip}\n\nI found similar products below.`;
}
