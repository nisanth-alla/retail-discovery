package com.innova.visual_retail_discovery.service.embeddings.impl;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.innova.visual_retail_discovery.service.embeddings.EmbeddingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

public class EmbeddingEngineImpl implements EmbeddingEngine {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEngineImpl.class);


    @Override
    public float[] embed(String imagePath, List<String> detectedLabels) throws Exception {
        Criteria<Image, float[]> criteria = Criteria.builder()
                // specify input/output types
                .setTypes(Image.class, float[].class)
                // indicate image classification model
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                // search filters
                .optFilter("layers", "18")          // ResNet-18
                .optFilter("dataset", "imagenet")    // trained on ImageNet
                .optEngine("PyTorch")
                .optTranslator(new EmbeddingTranslator()) // your custom translator
                .build();

        try (ZooModel<Image, float[]> model = criteria.loadModel();
             Predictor<Image, float[]> predictor = model.newPredictor()) {

            Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));
            float[] embedding = predictor.predict(img);



            log.debug("Embedding: {}", embedding);

            return embedding;
        }

    }

    @Override
    public float[] embed(Classifications.Classification obj, List<String> detectedLabels) throws Exception {
        return new float[0];
    }

    @Override
    public int dimensions() {
        return 0;
    }

    static class EmbeddingTranslator implements Translator<Image, float[]> {

        private Resize resize;
        private ToTensor toTensor;
        private Normalize normalize;

        public EmbeddingTranslator() {
            resize = new Resize(224, 224);
            toTensor = new ToTensor();
            normalize = new Normalize(
                    new float[]{0.485f, 0.456f, 0.406f},
                    new float[]{0.229f, 0.224f, 0.225f});
        }

        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDManager manager = ctx.getNDManager();

            // Convert Image -> NDArray with model's NDManager
            NDArray array = input.toNDArray(manager);

            // Apply transforms manually
            array = resize.transform(array);
            array = toTensor.transform(array);
            array = normalize.transform(array);

            // Add batch dimension
            //array = array.expandDims(0);

            // Wrap in NDList
            return new NDList(array);
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDArray features = list.singletonOrThrow().flatten(); // full vector
            return features.toFloatArray();
        }
    }



    // Custom translator for text input


}
