package com.innova.visual_retail_discovery.model;

import java.util.List;

/**
 * A value-added alternative to an exact match:
 * same visual DNA, but better price / rating / promotion.
 */
public class SmartSwapResult {

    public Long    productId;
    public String  name;
    public String  brand;
    public Double  price;
    public String  imagePath;
    public List<String> detectedLabels;

    /** Visual similarity to the original matched product (0‒1). */
    public float   visualSimilarity;

    /** Composite "value" score combining similarity + savings + rating + discount. */
    public float   valueScore;

    /** Human-readable reason for the swap suggestion. */
    public String  swapReason;

    /** How much cheaper than the original match (0 if not cheaper). */
    public Double  savingsAmount;

    /** Rating out of 5.0. */
    public Double  rating;

    /** Discount percentage (0–100). */
    public Double  discountPercent;

    public SmartSwapResult() {}

    public SmartSwapResult(Long productId, String name, String brand, Double price,
                           String imagePath, List<String> detectedLabels,
                           float visualSimilarity, float valueScore,
                           String swapReason, Double savingsAmount,
                           Double rating, Double discountPercent) {
        this.productId       = productId;
        this.name            = name;
        this.brand           = brand;
        this.price           = price;
        this.imagePath       = imagePath;
        this.detectedLabels  = detectedLabels;
        this.visualSimilarity = visualSimilarity;
        this.valueScore      = valueScore;
        this.swapReason      = swapReason;
        this.savingsAmount   = savingsAmount;
        this.rating          = rating;
        this.discountPercent = discountPercent;
    }
}
