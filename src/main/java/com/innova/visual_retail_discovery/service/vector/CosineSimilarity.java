package com.innova.visual_retail_discovery.service.vector;

public class CosineSimilarity implements SimilarityMetric {

    @Override
    public float similarity(float[] a, float[] b) {
        if (a.length != b.length) return 0f;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0f : (float)(dot / denom);
    }
}