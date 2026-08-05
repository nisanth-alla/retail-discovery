package com.innova.visual_retail_discovery.service.embeddings.impl;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Translator;

import java.net.URI;
import java.util.List;

public class TextEmbeddingService {

    // ---------------------------------------------------------------
    // Cache model + predictor — load ONCE, reuse for every call
    // ---------------------------------------------------------------
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    public void init() throws Exception {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2") // Use the corrected URL
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();

        model     = criteria.loadModel();
        predictor = model.newPredictor();
    }

    public void close() {
        if (predictor != null) predictor.close();
        if (model     != null) model.close();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------
    public float[] embed(List<String> labels) throws Exception {
        String text = String.join(", ", labels);
        return embedText(text);
    }

    public float[] embedText(String text) throws Exception {
        return predictor.predict(text);
    }

    // ---------------------------------------------------------------
    // Translator
    // ---------------------------------------------------------------
    public static class TextEmbeddingTranslator implements Translator<String, float[]> {

        private HuggingFaceTokenizer tokenizer;

        @Override
        public void prepare(TranslatorContext ctx) throws Exception {
            // Pulls tokenizer config from HuggingFace Hub
            tokenizer = HuggingFaceTokenizer.newInstance(
                    "sentence-transformers/all-MiniLM-L6-v2",
                    java.util.Map.of("maxLength", "128", "padding", "true", "truncation", "true")
            );
        }

        @Override
        public NDList processInput(TranslatorContext ctx, String input) {
            Encoding enc = tokenizer.encode(input);
            NDManager mgr = ctx.getNDManager();

            NDArray inputIds      = mgr.create(enc.getIds());
            NDArray attentionMask = mgr.create(enc.getAttentionMask());
            NDArray tokenTypeIds  = mgr.create(enc.getTypeIds());

            // Name them so the model knows which tensor is which
            inputIds.setName("input_ids");
            attentionMask.setName("attention_mask");
            tokenTypeIds.setName("token_type_ids");

            return new NDList(inputIds, attentionMask, tokenTypeIds);
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDArray tokenEmbeddings = list.get(0);          // [seq_len, hidden_size]
            NDArray meanPooled      = tokenEmbeddings.mean(new int[]{0}); // [hidden_size]

            // L2-normalise so cosine similarity == dot product
            NDArray norm       = meanPooled.norm();
            NDArray normalized = meanPooled.div(norm);

            return normalized.toFloatArray();
        }
    }
}
