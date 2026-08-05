package com.innova.visual_retail_discovery.service.vector;

import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FashionIndexer {

    private static final Logger log = LoggerFactory.getLogger(FashionIndexer.class);

    private final VectorStore     vectorStore;
    private final TextEmbeddingService textEmbeddingService;

    private final EmbeddingEngine embedder;

    public FashionIndexer(VectorStore vectorStore,
                          TextEmbeddingService textEmbeddingService, EmbeddingEngine embedder) {
        this.vectorStore = vectorStore;
        this.textEmbeddingService = textEmbeddingService;
        this.embedder = embedder;
    }

    public void buildIndex(String rootPath) throws Exception {

        // ── STEP 1: Scan all image files under filedatabase/ ──────────────
        log.info("[Step 1] Scanning image files...");
        List<Path> images = scanImages(rootPath);
        log.info("  Found {} images.", images.size());
        long i = 100;
        for (Path imgPath : images) {
            try {
                String pathStr = imgPath.toString();
                log.info("  Processing: {}", pathStr);

                // ── STEP 2: Extract name/brand/category from filename ──────────
                // Filename convention: <name>_<brand>_<category>_..._<price>.jpg
                String[] nameParts = imgPath.getFileName().toString().split("_");
                Random random = new Random();
                String productName = "Product-" + (random.nextInt(1000) * 10);
                String brand       = "Brand-"   + (random.nextInt(100)  * 10);
                String category    = null;
                Double priceValue  = 50.0d;
                if (nameParts.length > 3) {
                    productName = nameParts[0].toUpperCase();
                    brand       = nameParts[1].toUpperCase();
                    category    = nameParts[2];
                    priceValue  = Double.parseDouble(
                        nameParts[nameParts.length - 1].replace(".jpg","").replace(".jpeg",""));
                }
                // Labels are derived from the image filename (name + brand + category)
                List<String> labels = buildLabels(productName, brand, category);
                log.info("  Name-derived labels: {}", labels);

                // ── STEP 3: Generate text embedding from labels only ──────────
                log.info("[Step 3] Generating text embedding from labels...");
                float[] labelVector = textEmbeddingService.embed(labels);

                float[] vector = embedder.embed(pathStr, labels);

                // ── STEP 4: Save to Vector Store ───────────────────────────────
                log.info("[Step 4] Saving to vector store...");
                vectorStore.save(new ImageVector(pathStr, labels, vector, labelVector, i++, productName, brand, priceValue));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        log.info("[Step 4] Vector store persisted. Total entries: {}", vectorStore.getAll().size());
    }

    private List<String> buildLabels(String productName, String brand, String category) {
        return java.util.Arrays.stream(new String[]{productName, brand, category})
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

    // Recursively collect .jpg/.png/.jpeg files
    private List<Path> scanImages(String rootPath) throws IOException {
        return Files.walk(Paths.get(rootPath))
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
                })
                .collect(Collectors.toList());
    }
}