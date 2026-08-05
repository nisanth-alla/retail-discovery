package com.innova.visual_retail_discovery.service.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StyleRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(StyleRuleEngine.class);

    // ─── Catalogue ────────────────────────────────────────────────────────────
    static final Set<String> CATALOGUE = new LinkedHashSet<>(Arrays.asList(
            "shirt", "top", "sweater", "cardigan", "jacket", "vest",
            "pants", "shorts", "skirt", "coat", "bead", "watch",
            "cape", "glasses", "hat", "scarf"
    ));

    // ─── Rule DSL ─────────────────────────────────────────────────────────────
    // Each Rule: gender + baseItem + color  →  map of category → list of catalogue items
    static final List<Rule> RULES = new ArrayList<>();

    static {
        // ── MEN / SHIRT ───────────────────────────────────────────────────────
        addRule("men", "shirt", "blue",
                cat("tops",        "vest"),
                cat("bottoms",     "pants", "shorts"),
                cat("outerwear",   "jacket", "coat", "cardigan"),
                cat("accessories", "watch", "bead", "glasses", "hat", "scarf")
        );
        addRule("men", "shirt", "white",
                cat("tops",        "vest"),
                cat("bottoms",     "pants", "shorts"),
                cat("outerwear",   "jacket", "coat", "sweater"),
                cat("accessories", "watch", "scarf", "hat", "glasses")
        );
        addRule("men", "shirt", "black",
                cat("tops",        "vest"),
                cat("bottoms",     "pants", "shorts"),
                cat("outerwear",   "jacket", "coat", "cardigan"),
                cat("accessories", "watch", "bead", "scarf", "glasses")
        );
        addRule("men", "shirt", "grey",
                cat("tops",        "vest"),
                cat("bottoms",     "pants"),
                cat("outerwear",   "jacket", "coat", "sweater"),
                cat("accessories", "watch", "scarf", "hat")
        );

        // ── MEN / SWEATER ─────────────────────────────────────────────────────
        addRule("men", "sweater", "blue",
                cat("bottoms",     "pants", "shorts"),
                cat("outerwear",   "jacket", "coat"),
                cat("accessories", "watch", "scarf", "hat", "glasses")
        );
        addRule("men", "sweater", "grey",
                cat("bottoms",     "pants"),
                cat("outerwear",   "jacket", "coat"),
                cat("accessories", "watch", "scarf", "hat")
        );

        // ── MEN / JACKET ──────────────────────────────────────────────────────
        addRule("men", "jacket", "black",
                cat("tops",        "shirt", "sweater", "top"),
                cat("bottoms",     "pants", "shorts"),
                cat("accessories", "watch", "scarf", "glasses")
        );
        addRule("men", "jacket", "brown",
                cat("tops",        "shirt", "sweater"),
                cat("bottoms",     "pants"),
                cat("accessories", "watch", "scarf", "hat")
        );

        // ── MEN / PANTS ───────────────────────────────────────────────────────
        addRule("men", "pants", "navy",
                cat("tops",        "shirt", "sweater", "top"),
                cat("outerwear",   "jacket", "coat", "cardigan"),
                cat("accessories", "watch", "bead", "glasses")
        );
        addRule("men", "pants", "grey",
                cat("tops",        "shirt", "sweater", "vest"),
                cat("outerwear",   "jacket", "coat"),
                cat("accessories", "watch", "scarf")
        );
        addRule("men", "pants", "black",
                cat("tops",        "shirt", "top", "sweater"),
                cat("outerwear",   "jacket", "coat", "cardigan"),
                cat("accessories", "watch", "bead", "glasses", "scarf")
        );

        // ── WOMEN / TOP ───────────────────────────────────────────────────────
        addRule("women", "top", "white",
                cat("bottoms",     "pants", "skirt", "shorts"),
                cat("outerwear",   "jacket", "cardigan", "coat"),
                cat("accessories", "bead", "watch", "scarf", "hat", "glasses")
        );
        addRule("women", "top", "black",
                cat("bottoms",     "pants", "skirt", "shorts"),
                cat("outerwear",   "jacket", "cardigan", "coat"),
                cat("accessories", "bead", "watch", "scarf", "glasses")
        );
        addRule("women", "top", "red",
                cat("bottoms",     "pants", "skirt"),
                cat("outerwear",   "jacket", "coat"),
                cat("accessories", "bead", "watch", "glasses")
        );
        addRule("women", "top", "pink",
                cat("bottoms",     "skirt", "pants"),
                cat("outerwear",   "cardigan", "jacket"),
                cat("accessories", "bead", "watch", "scarf", "hat")
        );

        // ── WOMEN / SHIRT ─────────────────────────────────────────────────────
        addRule("women", "shirt", "blue",
                cat("bottoms",     "skirt", "pants", "shorts"),
                cat("outerwear",   "jacket", "cardigan"),
                cat("accessories", "bead", "watch", "scarf", "glasses")
        );
        addRule("women", "shirt", "white",
                cat("bottoms",     "skirt", "pants"),
                cat("outerwear",   "jacket", "cardigan", "coat"),
                cat("accessories", "bead", "watch", "hat", "glasses")
        );

        // ── WOMEN / SKIRT ─────────────────────────────────────────────────────
        addRule("women", "skirt", "black",
                cat("tops",        "top", "shirt", "sweater"),
                cat("outerwear",   "jacket", "cardigan", "coat"),
                cat("accessories", "bead", "watch", "scarf", "glasses")
        );
        addRule("women", "skirt", "floral",
                cat("tops",        "top", "shirt"),
                cat("outerwear",   "cardigan", "jacket"),
                cat("accessories", "bead", "hat", "glasses")
        );

        // ── WOMEN / CARDIGAN ──────────────────────────────────────────────────
        addRule("women", "cardigan", "beige",
                cat("tops",        "top", "shirt"),
                cat("bottoms",     "pants", "skirt"),
                cat("accessories", "bead", "scarf", "watch", "glasses")
        );

        // ── WOMEN / COAT ──────────────────────────────────────────────────────
        addRule("women", "coat", "camel",
                cat("tops",        "sweater", "shirt", "top"),
                cat("bottoms",     "pants", "skirt"),
                cat("accessories", "bead", "scarf", "hat", "watch", "glasses")
        );

        // ── UNISEX / SCARF ────────────────────────────────────────────────────
        addRule("unisex", "scarf", "grey",
                cat("tops",        "sweater", "shirt"),
                cat("outerwear",   "coat", "jacket"),
                cat("accessories", "hat", "watch", "glasses")
        );

        // ── UNISEX / HAT ──────────────────────────────────────────────────────
        addRule("unisex", "hat", "black",
                cat("tops",        "shirt", "top", "sweater"),
                cat("outerwear",   "jacket", "coat"),
                cat("accessories", "scarf", "glasses", "watch")
        );

        // ── UNISEX / VEST ─────────────────────────────────────────────────────
        addRule("unisex", "vest", "navy",
                cat("tops",        "shirt"),
                cat("bottoms",     "pants"),
                cat("accessories", "watch", "scarf", "glasses")
        );
    }

    // ─── Rule Engine Logic ────────────────────────────────────────────────────

    public static Map<String, List<String>> query(String input) {
        String[] tokens = input.toLowerCase().trim().split("\\s+");

        String gender   = extractGender(tokens);
        String color    = extractColor(tokens);
        String baseItem = extractItem(tokens);

        if (baseItem == null) {
            return Collections.singletonMap("error",
                    Collections.singletonList("No recognised catalogue item found in: \"" + input + "\""));
        }

        // 1. Try exact match (gender + item + color)
        for (Rule r : RULES) {
            if (r.matches(gender, baseItem, color)) {
                return annotateResult(r.complements, gender, color, baseItem, "exact");
            }
        }

        // 2. Fallback: match gender + item only (ignore color)
        for (Rule r : RULES) {
            if (r.matchesNoColor(gender, baseItem)) {
                return annotateResult(r.complements, gender, color, baseItem, "item-only");
            }
        }

        // 3. Fallback: item only (any gender)
        for (Rule r : RULES) {
            if (r.baseItem.equals(baseItem)) {
                return annotateResult(r.complements, gender, color, baseItem, "generic");
            }
        }

        return Collections.singletonMap("error",
                Collections.singletonList("No rule found for: \"" + input + "\""));
    }

    // ─── Token Extractors ─────────────────────────────────────────────────────

    static String extractGender(String[] tokens) {
        for (String t : tokens) {
            if (t.equals("men") || t.equals("man") || t.equals("male")) return "men";
            if (t.equals("women") || t.equals("woman") || t.equals("female")) return "women";
        }
        return "unisex";
    }

    static final Set<String> COLORS = new HashSet<>(Arrays.asList(
            "blue","red","black","white","grey","gray","green","yellow","pink",
            "purple","orange","brown","beige","navy","camel","floral","olive",
            "burgundy","cream","maroon","coral","teal","khaki","tan"
    ));

    static String extractColor(String[] tokens) {
        for (String t : tokens) {
            String norm = t.equals("gray") ? "grey" : t;
            if (COLORS.contains(norm)) return norm;
        }
        return "any";
    }

    static String extractItem(String[] tokens) {
        for (String t : tokens) {
            if (CATALOGUE.contains(t)) return t;
        }
        return null;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    static Map<String, List<String>> annotateResult(
            Map<String, List<String>> base, String gender, String color, String item, String matchType) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("_input_summary", Arrays.asList(
                "gender=" + gender, "item=" + item, "color=" + color, "match=" + matchType
        ));
        result.putAll(base);
        return result;
    }

    // Builder helpers
    static void addRule(String gender, String item, String color, Map<String, List<String>>... cats) {
        Map<String, List<String>> complements = new LinkedHashMap<>();
        for (Map<String, List<String>> c : cats) complements.putAll(c);
        RULES.add(new Rule(gender, item, color, complements));
    }

    @SafeVarargs
    static Map<String, List<String>> cat(String category, String... items) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(category, Arrays.asList(items));
        return m;
    }

    // ─── Rule Model ───────────────────────────────────────────────────────────

    static class Rule {
        String gender, baseItem, color;
        Map<String, List<String>> complements;

        Rule(String gender, String baseItem, String color, Map<String, List<String>> complements) {
            this.gender = gender;
            this.baseItem = baseItem;
            this.color = color;
            this.complements = complements;
        }

        boolean matches(String g, String item, String c) {
            boolean genderOk = gender.equals("unisex") || gender.equals(g) || g.equals("unisex");
            return genderOk && baseItem.equals(item) && (color.equals(c) || c.equals("any"));
        }

        boolean matchesNoColor(String g, String item) {
            boolean genderOk = gender.equals("unisex") || gender.equals(g) || g.equals("unisex");
            return genderOk && baseItem.equals(item);
        }
    }

    // ─── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        log.info("Style Rule Engine (Java) started. Catalogue: {}", CATALOGUE);

        while (true) {
            System.out.print("Enter style query: ");
            String input = sc.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) break;
            if (input.isEmpty()) continue;

            Map<String, List<String>> result = query(input);
            printResult(input, result);
        }
        sc.close();
    }

    static void printResult(String input, Map<String, List<String>> result) {
        log.info("Query: {}", input);
        for (Map.Entry<String, List<String>> e : result.entrySet()) {
            String cat = e.getKey();
            if (cat.equals("_input_summary")) {
                log.info("[Parsed ] {}", e.getValue());
            } else if (cat.equals("error")) {
                log.error("[Error  ] {}", e.getValue().get(0));
            } else {
                log.info("{} -> {}", capitalize(cat), e.getValue());
            }
        }
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
