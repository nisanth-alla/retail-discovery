package com.innova.visual_retail_discovery.service.vector;

public interface SimilarityMetric {
    /**
     * Return similarity score between two vectors (higher = more similar).
     * FUTURE SWAP: Replace with Dot Product for CLIP embeddings (unit vectors).
     */
    float similarity(float[] a, float[] b);


}
