package com.innova.visual_retail_discovery.service.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(JsonVectorStore.class);

    private final String           storePath;
    private final String           storeFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ImageVector> store  = new ArrayList<>();

    public JsonVectorStore(String storePath) {
        this.storePath = storePath;
        this.storeFile = storePath + "/vector_store.json";
    }

    @Override
    public void save(ImageVector iv) throws IOException {
        store.add(iv);
        persist();   // write-through; batch for performance if needed
    }

    @Override
    public void load() throws IOException {
        File f = new File(storeFile);
        if (f.exists()) {
            ImageVector[] loaded = mapper.readValue(f, ImageVector[].class);
            store.clear();
            Collections.addAll(store, loaded);
            log.info("  Loaded {} vectors from store.", store.size());
        }
    }

    @Override
    public List<ImageVector> getAll() { return Collections.unmodifiableList(store); }

    @Override
    public boolean isEmpty() {
        File f = new File(storeFile);
        return !f.exists() || f.length() == 0;
    }

    private void persist() throws IOException {
        Files.createDirectories(Paths.get(storePath));
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(storeFile), store);
    }
}