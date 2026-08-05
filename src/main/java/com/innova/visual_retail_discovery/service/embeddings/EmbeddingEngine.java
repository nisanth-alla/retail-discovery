package com.innova.visual_retail_discovery.service.embeddings;

import ai.djl.modality.Classifications;

import java.util.List;

public interface EmbeddingEngine {
    /**
     * Generate a float vector for the given image + detected labels.
     * Vector is used for similarity comparison.
     *
     * FUTURE SWAP: Replace with CLIP/FashionCLIP to get multi-modal
     * embeddings that capture both visual style and semantic meaning.
     */
    float[] embed(String imagePath, List<String> detectedLabels) throws Exception;
    float[] embed(Classifications.Classification obj, List<String> detectedLabels) throws Exception;

    /** Dimensionality of vectors this engine produces */
    int dimensions();

}
