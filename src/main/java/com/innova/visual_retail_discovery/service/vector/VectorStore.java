package com.innova.visual_retail_discovery.service.vector;

import java.io.IOException;
import java.util.List;

// ── Interface 3: Vector Storage ───────────────────────────────
public interface VectorStore {
    void save(ImageVector iv) throws IOException;
    void load() throws IOException;
    List<ImageVector> getAll();
    boolean isEmpty();
}

