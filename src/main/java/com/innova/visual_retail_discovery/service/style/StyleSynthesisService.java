package com.innova.visual_retail_discovery.service.style;

import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.model.SmartSwapResult;
import com.innova.visual_retail_discovery.service.vector.CosineSimilarity;
import com.innova.visual_retail_discovery.service.vector.ImageVector;
import com.innova.visual_retail_discovery.service.vector.SimilarityMetric;
import com.innova.visual_retail_discovery.service.vector.VectorStore;

import com.innova.visual_retail_discovery.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pillar 2 – Style Synthesis & Smart Swaps.
 *
 * Two capabilities:
 *  1. getComplementaryProducts  – "Complete the Look" suggestions
 *  2. getSmartSwaps             – "Similarity-with-Benefits" alternatives
 */
public class StyleSynthesisService {

    // ── Fashion affinity map ──────────────────────────────────────────────────
    // Key   = detected category of an item the user already has
    // Value = categories that complement it
    private static final Map<String, List<String>> AFFINITY_MAP = new LinkedHashMap<>();

    static {
        AFFINITY_MAP.put("shirt",    List.of("pants", "jacket", "belt", "watch", "scarf"));
        AFFINITY_MAP.put("top",      List.of("pants", "skirt", "jacket", "scarf"));
        AFFINITY_MAP.put("sweater",  List.of("pants", "scarf", "hat", "coat"));
        AFFINITY_MAP.put("cardigan", List.of("shirt", "pants", "scarf"));
        AFFINITY_MAP.put("jacket",   List.of("shirt", "pants", "scarf", "hat", "watch"));
        AFFINITY_MAP.put("vest",     List.of("shirt", "pants", "watch"));
        AFFINITY_MAP.put("pants",    List.of("shirt", "jacket", "belt", "watch"));
        AFFINITY_MAP.put("shorts",   List.of("top", "hat", "glasses"));
        AFFINITY_MAP.put("skirt",    List.of("top", "scarf", "hat"));
        AFFINITY_MAP.put("coat",     List.of("sweater", "scarf", "hat", "watch"));
        AFFINITY_MAP.put("cape",     List.of("sweater", "pants", "hat"));
        AFFINITY_MAP.put("hat",      List.of("coat", "scarf", "jacket"));
        AFFINITY_MAP.put("scarf",    List.of("coat", "jacket", "sweater", "hat"));
        AFFINITY_MAP.put("watch",    List.of("shirt", "jacket", "vest"));
        AFFINITY_MAP.put("glasses",  List.of("hat", "jacket", "shirt"));
        AFFINITY_MAP.put("bead",     List.of("top", "shirt", "jacket"));
    }

    // Minimum visual similarity to qualify as a smart swap candidate
    private static final float SMART_SWAP_SIMILARITY_THRESHOLD = 0.30f;

    // Maximum smart-swap alternatives returned per matched item
    private static final int MAX_SWAPS_PER_ITEM = 3;

    // Maximum complementary products returned in total
    private static final int MAX_COMPLEMENTARY = 6;

    private final SimilarityMetric metric = new CosineSimilarity();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * "Complete the Look" – given what was detected in the query image,
     * pull complementary products from the vector store.
     *
     * @param matchedItems top results from the visual search
     * @param vectorStore  full product index
     * @return complementary product suggestions (de-duped, excluding already-matched)
     */
    public List<SearchResult> getComplementaryProducts(List<SearchResult> matchedItems,
                                                        VectorStore vectorStore) {
        // Collect detected categories from the user's matched items
        Set<String> detectedCategories = matchedItems.stream()
                .flatMap(r -> r.detectedLabels.stream())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Build the target complementary categories
        Set<String> targetCategories = detectedCategories.stream()
                .flatMap(cat -> AFFINITY_MAP.getOrDefault(cat, Collections.emptyList()).stream())
                .filter(c -> !detectedCategories.contains(c))  // don't suggest what they already have
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (targetCategories.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect product IDs already shown to the user
        Set<Long> shownProductIds = matchedItems.stream()
                .map(r -> r.productId)
                .collect(Collectors.toSet());

        // Find products in the vector store whose detectedLabels overlap target categories
        List<SearchResult> complementary = vectorStore.getAll().stream()
                .filter(iv -> !shownProductIds.contains(iv.productId))
                .filter(iv -> iv.detectedLabels != null && targetCategories != null &&
                              iv.detectedLabels.stream()
                                  .filter(Objects::nonNull)
                                  .map(String::toLowerCase)
                                  .anyMatch(targetCategories::contains))
                .map(iv -> toSearchResult(iv, computeAffinityScore(iv, targetCategories)))
                .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                .limit(MAX_COMPLEMENTARY)
                .collect(Collectors.toList());

        normaliseImagePaths(complementary);
        return complementary;
    }

    /**
     * "Similarity-with-Benefits" – for each matched item find alternatives that
     * share its visual DNA but offer better price, rating, or a promotion.
     *
     * @param matchedItems   top results from the visual search
     * @param vectorStore    full product index
     * @return smart-swap alternatives sorted by valueScore
     */
    public List<SmartSwapResult> getSmartSwaps(List<SearchResult> matchedItems,
                                                VectorStore vectorStore) {
        // Build a quick lookup: productId → vector
        Map<Long, float[]> vectorByProductId = vectorStore.getAll().stream()
                .collect(Collectors.toMap(iv -> iv.productId, iv -> iv.vector, (a, b) -> a));

        Set<Long> shownProductIds = matchedItems.stream()
                .map(r -> r.productId)
                .collect(Collectors.toSet());

        List<SmartSwapResult> swaps = new ArrayList<>();

        for (SearchResult matched : matchedItems) {
            float[] queryVector = vectorByProductId.get(matched.productId);
            if (queryVector == null) continue;

            double matchedPrice = matched.price != null ? matched.price : 0.0;

            List<SmartSwapResult> alternatives = vectorStore.getAll().stream()
                    .filter(iv -> iv.vector != null && iv.productId != null)          // guard nulls from JSON
                    .filter(iv -> !shownProductIds.contains(iv.productId))
                    .filter(iv -> iv.detectedLabels != null && matched.detectedLabels != null &&
                                  !Collections.disjoint(
                                      normaliseLabels(iv.detectedLabels),
                                      normaliseLabels(matched.detectedLabels)))
                    .filter(iv -> iv.vector.length == queryVector.length)             // vector dimension guard
                    .map(iv -> toSmartSwap(iv, queryVector, matchedPrice))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble((SmartSwapResult s) -> s.valueScore).reversed())
                    .limit(MAX_SWAPS_PER_ITEM)
                    .collect(Collectors.toList());

            swaps.addAll(alternatives);
        }

        // De-duplicate by productId keeping highest valueScore
        return swaps.stream()
                .collect(Collectors.toMap(s -> s.productId, s -> s,
                        (a, b) -> a.valueScore >= b.valueScore ? a : b))
                .values().stream()
                .sorted(Comparator.comparingDouble((SmartSwapResult s) -> s.valueScore).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Extracted from the stream lambda so exceptions are traceable by method name. */
    private SmartSwapResult toSmartSwap(ImageVector iv, float[] queryVector, double matchedPrice) {
        float visualSim = metric.similarity(queryVector, iv.vector);
        if (visualSim < SMART_SWAP_SIMILARITY_THRESHOLD) return null;

        double rating         = generateRating(iv.productId);
        double discountPct    = generateDiscount(iv.productId);
        double effectivePrice = iv.price * (1 - discountPct / 100.0);
        double savings        = Math.max(0, matchedPrice - effectivePrice);

        float valueScore = computeValueScore(visualSim, savings, matchedPrice, rating, discountPct);

        String reason = buildSwapReason(savings, matchedPrice, rating, discountPct);
        if (reason.isEmpty()) return null;   // no real benefit – skip

        String path = normaliseImagePath(iv.imagePath);
        return new SmartSwapResult(
                iv.productId, iv.name, iv.brand, effectivePrice,
                path, iv.detectedLabels,
                visualSim, valueScore,
                reason, savings, rating, discountPct);
    }

    private float computeAffinityScore(ImageVector iv, Set<String> targetCategories) {
        if (iv.detectedLabels == null) return 0f;
        long matches = iv.detectedLabels.stream()
                .map(String::toLowerCase)
                .filter(targetCategories::contains)
                .count();
        return (float) matches / targetCategories.size();
    }

    /**
     * Composite value score:
     *   50% visual similarity  +  30% price saving ratio  +  10% rating bonus  +  10% discount bonus
     */
    private float computeValueScore(float visualSim, double savings,
                                    double originalPrice, double rating, double discountPct) {
        float savingsRatio   = originalPrice > 0 ? (float)(savings / originalPrice) : 0f;
        float ratingBonus    = (float)((rating - 3.0) / 2.0);  // normalise 3‒5 → 0‒1
        float discountBonus  = (float)(discountPct / 100.0);

        return Math.min(1f,
               visualSim    * 0.50f
             + savingsRatio  * 0.30f
             + ratingBonus   * 0.10f
             + discountBonus * 0.10f);
    }

    private String buildSwapReason(double savings, double originalPrice,
                                   double rating, double discountPct) {
        List<String> reasons = new ArrayList<>();
        if (savings > 0)        reasons.add(String.format("%.0f%% cheaper", (savings / originalPrice) * 100));
        if (rating >= 4.5)      reasons.add("top-rated (" + String.format("%.1f", rating) + "★)");
        if (discountPct >= 10)  reasons.add(String.format("%.0f%% off promo", discountPct));
        return String.join(" · ", reasons);
    }

    /**
     * Deterministic synthetic rating seeded by productId (3.0–5.0).
     * In production, replace with a real ratings field from the DB.
     */
    private double generateRating(Long productId) {
        Random rng = new Random(productId * 31L + 7L);
        return 3.0 + rng.nextDouble() * 2.0;  // 3.0–5.0
    }

    /**
     * Deterministic synthetic discount percentage seeded by productId (0–30%).
     * ~40% of products have a discount; the rest have 0%.
     */
    private double generateDiscount(Long productId) {
        Random rng = new Random(productId * 17L + 13L);
        return rng.nextDouble() < 0.40 ? rng.nextInt(30) + 5 : 0.0;
    }

    private SearchResult toSearchResult(ImageVector iv, float score) {
        return new SearchResult(iv.imagePath, iv.detectedLabels, score,
                                iv.productId, iv.name, iv.brand, iv.price);
    }

    private Set<String> normaliseLabels(List<String> labels) {
        return labels.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private String normaliseImagePath(String path) {
        if (path == null) return "";
        if (path.contains("Mahesh")) {
            return path.replace("C:\\Users\\Mahesh.Bantaram\\Desktop\\InternalApps\\Training\\binary_brains_cs04_2026_aih\\target\\classes\\static\\",
                                Utils.API_BASE_URL + "/");
        }
        return path.replace("src\\main\\resources\\static\\", Utils.API_BASE_URL + "/");
    }

    private void normaliseImagePaths(List<SearchResult> results) {
        results.forEach(r -> r.imagePath = normaliseImagePath(r.imagePath));
    }
}
