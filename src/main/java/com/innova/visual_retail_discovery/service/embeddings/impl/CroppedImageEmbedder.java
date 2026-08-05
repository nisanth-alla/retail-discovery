package com.innova.visual_retail_discovery.service.embeddings.impl;

import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;
import com.innova.visual_retail_discovery.service.embeddings.impl.EmbeddingEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class CroppedImageEmbedder {

    private static final Logger log = LoggerFactory.getLogger(CroppedImageEmbedder.class);

    public static float[] searchEmbeddingsOnCroppedImages(File croppedImageFile,String queryLabels) throws Exception {
        log.info("[Step 5] Searching Embedding for the detected object............{}", queryLabels);

        EmbeddingEngine embedder = new EmbeddingEngineImpl();
        //String queryLabels = obj.getClassName();
        float[] queryVector = embedder.embed(croppedImageFile.getPath(), List.of(queryLabels));
        return queryVector;
    }
}
