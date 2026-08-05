package com.innova.visual_retail_discovery.service.detector;

import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.service.vector.VectorStore;

import java.util.List;

public interface ObjectDetector extends AutoCloseable {
    /**
     * Detect fashion-relevant objects in an image file.
     * Returns a list of label strings, e.g. ["shirt", "watch", "glasses"]
     *
     * FUTURE SWAP: Implement this with FashionCLIP or a fine-tuned
     * DeepFashion2 YOLO model for much better fashion category accuracy.
     */
    List<String> detect(String imagePath) throws Exception;

    public List<SearchResult> detectAndReturnSearchResults(String imagePath, VectorStore vectorStore, int topK) throws Exception;

    public List<SearchResult> queryImagesByLabel(VectorStore vectorStore, float[] queryVector, int topK) throws Exception;
}

