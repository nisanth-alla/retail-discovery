package com.innova.visual_retail_discovery.service.engine;

import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.service.detector.ObjectDetector;
import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;
import com.innova.visual_retail_discovery.service.vector.CosineSimilarity;
import com.innova.visual_retail_discovery.service.vector.SimilarityMetric;
import com.innova.visual_retail_discovery.service.vector.VectorStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FashionSearchEngine {

    private static final Logger log = LoggerFactory.getLogger(FashionSearchEngine.class);

    private final ObjectDetector detector;
    private final VectorStore vectorStore;

    public FashionSearchEngine(ObjectDetector detector, VectorStore vectorStore) {
        this.detector    = detector;
        this.vectorStore = vectorStore;
    }

    public List<SearchResult> search(String queryImagePath, int topK) throws Exception {

        // ── STEP 5a: Detect objects in query image ─────────────────────────
        log.info("[Step 2] Detecting objects in query image...");
        List<SearchResult> searchResultList = detector.detectAndReturnSearchResults(queryImagePath,vectorStore, topK);

        return searchResultList;
    }

    public List<SearchResult> searchByImageLabel(String queryImagePath, List<String> label, int topK) throws Exception {

        // ── STEP 5a: Detect objects in query image ─────────────────────────
        log.info("[Step 2] Detecting objects in query image...");
        List<SearchResult> searchResultList = detector.detectAndReturnSearchResults(queryImagePath,vectorStore, topK);

        return searchResultList;
    }
}
