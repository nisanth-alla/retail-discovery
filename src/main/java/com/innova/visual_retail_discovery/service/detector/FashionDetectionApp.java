package com.innova.visual_retail_discovery.service.detector;


import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import com.innova.visual_retail_discovery.utils.Utils;
import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;
import com.innova.visual_retail_discovery.service.embeddings.impl.EmbeddingEngineImpl;
import com.innova.visual_retail_discovery.service.engine.FashionSearchEngine;
import com.innova.visual_retail_discovery.service.vector.FashionIndexer;
import com.innova.visual_retail_discovery.service.vector.JsonVectorStore;
import com.innova.visual_retail_discovery.service.vector.VectorStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────────────────────
// ENTRY POINT
// ─────────────────────────────────────────────────────────────
public class FashionDetectionApp {

    private static final Logger log = LoggerFactory.getLogger(FashionDetectionApp.class);


    public static List<SearchResult> searchImage(File jpeg) throws Exception {
        String databasePath = Path.of("src", "main", "resources", "static").toString();


        ObjectDetector detector = new EnhancedYoloObjectDetector();
        String storePath = "embeddings";     // folder where vector store is saved
        VectorStore vectorStore = new JsonVectorStore(storePath);

        if (vectorStore.isEmpty()) {
            TextEmbeddingService textEmbeddingService = new TextEmbeddingService();
            textEmbeddingService.init();
            EmbeddingEngine embedder = new EmbeddingEngineImpl();
            FashionIndexer indexer = new FashionIndexer(vectorStore, textEmbeddingService, embedder);
            log.info("=== STEP 1-4: Building vector index from filedatabase/ ===");
            indexer.buildIndex(databasePath);
            log.info("Index built and saved.");
        } else {
            log.info("=== Existing index found. Loading... ===");
            vectorStore.load();
        }

        // ── QUERY: put your query image path here ───────────────────────────
        String queryImagePath = jpeg.getPath();

        log.info("[Step 1] Calling FashionSearchEngine for {} ===", queryImagePath);
        FashionSearchEngine searchEngine = new FashionSearchEngine(detector, vectorStore);
        List<SearchResult> results = searchEngine.search(queryImagePath, 5);

        log.info("--- Top Matches ---");

        List<SearchResult> uniqueList = new ArrayList<>(
                results.stream()
                        .collect(Collectors.toMap(
                                sr -> sr.imagePath,          // key: imagePath
                                sr -> sr,                   // value: object
                                (existing, replacement) -> existing // handle duplicates (keep first)
                        ))
                        .values()
        );

        for (SearchResult result : uniqueList) {

            log.info("Score: {}", result.score);
            result.imagePath = normaliseImagePath(result.imagePath);
            log.info("Image path: {}", result.imagePath);
            log.info("Detected labels: {}", result.detectedLabels);

        }
        uniqueList.sort(Comparator.comparing(unique->unique.detectedLabels.get(0),Comparator.nullsLast(Comparator.naturalOrder())));
        return uniqueList;
    }

    public static List<SearchResult> searchImageByFileLabelAndPrice(File jpeg, List<String> labels, Double price) throws Exception {
        String databasePath = Path.of("src", "main", "resources", "static").toString();
        String storePath = "embeddings";     // folder where vector store is saved

        ObjectDetector detector = new EnhancedYoloObjectDetector();

        VectorStore vectorStore = new JsonVectorStore(storePath);

        if (vectorStore.isEmpty()) {
            TextEmbeddingService textEmbeddingService = new TextEmbeddingService();
            textEmbeddingService.init();
            EmbeddingEngine embedder = new EmbeddingEngineImpl();
            FashionIndexer indexer = new FashionIndexer(vectorStore, textEmbeddingService, embedder);
            log.info("=== STEP 1-4: Building vector index from filedatabase/ ===");
            indexer.buildIndex(databasePath);
            log.info("Index built and saved.");
        } else {
            log.info("=== Existing index found. Loading... ===");
            vectorStore.load();
        }

        // ── QUERY: put your query image path here ───────────────────────────

        String queryImagePath = jpeg.getPath();

        log.info("[Step 1] Calling FashionSearchEngine for {} ===", queryImagePath);
        FashionSearchEngine searchEngine = new FashionSearchEngine(detector, vectorStore);
        List<SearchResult> results = searchEngine.search(queryImagePath, 5);

        if(price>0){
            results = results.stream().filter(searchResult -> searchResult.price<price).collect(Collectors.toList());
        }
        log.info("--- Top Matches ---");

        List<SearchResult> uniqueList = new ArrayList<>(
                results.stream()
                        .collect(Collectors.toMap(
                                sr -> sr.imagePath,          // key: imagePath
                                sr -> sr,                   // value: object
                                (existing, replacement) -> existing // handle duplicates (keep first)
                        ))
                        .values()
        );

        for (SearchResult result : uniqueList) {

            log.info("Score: {}", result.score);
            result.imagePath = normaliseImagePath(result.imagePath);
            log.info("Image path: {}", result.imagePath);
            log.info("Detected labels: {}", result.detectedLabels);

        }
        uniqueList.sort(Comparator.comparing(
                unique -> (unique.detectedLabels != null && !unique.detectedLabels.isEmpty())
                        ? unique.detectedLabels.get(0)
                        : null,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return uniqueList;
    }

    public List<SearchResult> queryImagesByLabel(VectorStore vectorStore , float[] embedding) throws Exception{

        ObjectDetector detector = new EnhancedYoloObjectDetector();

        List<SearchResult> searchResultList = detector.queryImagesByLabel(vectorStore,embedding,2);
        return searchResultList;
    }

    private static String normaliseImagePath(String path) {
        if (path == null) return "";
        String normalizedPath = path.replace('\\', '/');
        int staticIndex = normalizedPath.indexOf("/static/");
        if (staticIndex >= 0) {
            return normalizedPath.substring(staticIndex + "/static".length());
        }
        return path;
    }
}
