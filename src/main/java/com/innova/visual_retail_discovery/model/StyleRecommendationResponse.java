package com.innova.visual_retail_discovery.model;

import java.util.List;

/**
 * Full response for Pillar 2 – Style Synthesis & Smart Swaps.
 *
 * complementaryProducts – items that complete the look (The Look).
 * smartSwaps            – value-added alternatives for matched items (The Swap).
 * originalMatches       – the top search results that triggered the recommendations.
 */
public class StyleRecommendationResponse {

    /** Original visual-search matches from the query image. */
    public List<SearchResult> originalMatches;

    /**
     * "Complete the Look" suggestions – products from complementary style categories
     * that pair well with the detected items (e.g., a belt that goes with boots).
     */
    public List<SearchResult> complementaryProducts;

    /**
     * "Similarity-with-Benefits" alternatives – visually similar items with
     * better price, higher rating, or active promotion.
     */
    public List<SmartSwapResult> smartSwaps;

    public StyleRecommendationResponse() {}

    public StyleRecommendationResponse(List<SearchResult> originalMatches,
                                       List<SearchResult> complementaryProducts,
                                       List<SmartSwapResult> smartSwaps) {
        this.originalMatches       = originalMatches;
        this.complementaryProducts = complementaryProducts;
        this.smartSwaps            = smartSwaps;
    }
}
