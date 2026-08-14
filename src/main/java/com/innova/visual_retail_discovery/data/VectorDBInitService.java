package com.innova.visual_retail_discovery.data;

import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;
import com.innova.visual_retail_discovery.service.embeddings.impl.EmbeddingEngineImpl;
import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import com.innova.visual_retail_discovery.service.vector.FashionIndexer;
import com.innova.visual_retail_discovery.service.vector.JsonVectorStore;
import com.innova.visual_retail_discovery.service.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class VectorDBInitService {

    private static final Logger log = LoggerFactory.getLogger(VectorDBInitService.class);

    public static void createDatabase() throws Exception {

        String databasePath = Path.of("src", "main", "resources", "static").toString();

        String storePath = "embeddings";     // folder where vector store is saved
        VectorStore vectorStore = new JsonVectorStore(storePath);

        if (vectorStore.isEmpty()) {
            TextEmbeddingService textEmbeddingService = new TextEmbeddingService();
            textEmbeddingService.init();
            EmbeddingEngine embedder = new EmbeddingEngineImpl();
            FashionIndexer indexer = new FashionIndexer(vectorStore, textEmbeddingService, embedder);
            log.info("=== STEP 1 Building vector index from filedatabase/ ===");
            indexer.buildIndex(databasePath);
            log.info("Index built and saved.");
        } else {
            log.info("Existing vector index found; skipping startup indexing.");
        }
        log.info("DB Initialized");
    }
}
