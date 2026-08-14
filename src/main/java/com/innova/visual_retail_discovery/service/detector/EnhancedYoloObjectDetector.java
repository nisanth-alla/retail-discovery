
package com.innova.visual_retail_discovery.service.detector;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.translate.TranslateException;
import ai.onnxruntime.OrtException;
import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.service.translator.EnhancedYoloV11Translator;
import com.innova.visual_retail_discovery.service.vector.CosineSimilarity;
import com.innova.visual_retail_discovery.service.vector.ImageVector;
import com.innova.visual_retail_discovery.service.vector.SimilarityMetric;
import com.innova.visual_retail_discovery.service.vector.VectorStore;
import com.innova.visual_retail_discovery.service.embeddings.impl.CroppedImageEmbedder;
import com.innova.visual_retail_discovery.service.embeddings.impl.StandAloneYoloObjectClassification;
import com.innova.visual_retail_discovery.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.innova.visual_retail_discovery.utils.Utils.TRAINED_MODEL_PATH;

public class EnhancedYoloObjectDetector implements ObjectDetector {

    private static final Logger log = LoggerFactory.getLogger(EnhancedYoloObjectDetector.class);

    private static final List<String> CLASSES = List.of(
            Utils.SUPPORTED_CLASSES
    );

    private static final Object MODEL_LOCK = new Object();
    private static volatile Model sharedModel;
    private static volatile Predictor<Image, DetectedObjects> sharedPredictor;
    private final Predictor<Image, DetectedObjects> predictor;

    public EnhancedYoloObjectDetector() throws IOException, MalformedModelException {
        predictor = getSharedPredictor();

        //System.out.println("YOLO ONNX model loaded successfully.");
    }

    private static Predictor<Image, DetectedObjects> getSharedPredictor() throws IOException, MalformedModelException {
        Predictor<Image, DetectedObjects> predictor = sharedPredictor;
        if (predictor == null) {
            synchronized (MODEL_LOCK) {
                predictor = sharedPredictor;
                if (predictor == null) {
                    Model model = Model.newInstance("yolo11n", "OnnxRuntime");
                    model.load(Paths.get(TRAINED_MODEL_PATH));
                    sharedModel = model;
                    predictor = model.newPredictor(new EnhancedYoloV11Translator(CLASSES));
                    sharedPredictor = predictor;
                }
            }
        }
        return predictor;
    }

    private DetectedObjects predict(Image image) throws TranslateException {
        synchronized (MODEL_LOCK) {
            return predictor.predict(image);
        }
    }

    @Override
    public List<SearchResult> detectAndReturnSearchResults(String imagePath, VectorStore vectorStore, int topK) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(3);
        //Call Stand-alone code to get the cropped files data.
        Future<List<File>> croppedFilesFuture = executor.submit(() -> createCroppedImageAndReturnFiles(imagePath));

        List<File> files = croppedFilesFuture.get();
        List<SearchResult> searchResultList = new ArrayList<>();
        SimilarityMetric metric = new CosineSimilarity();
        for (File croppedImageFile : files) {
            if(!croppedImageFile.getName().toLowerCase().contains("hat")) {
                  List<SearchResult> fileSearchResult = getSearchResult(croppedImageFile, vectorStore, metric, topK);
                  searchResultList.addAll(fileSearchResult);
                  //String imagePath, List<String> detectedLabels, float score, Long productId,String name,String brand,double price
                  searchResultList.add(new SearchResult(croppedImageFile.getPath().replace("target\\classes\\static\\", Utils.API_BASE_URL + "/"), List.of(croppedImageFile.getName()), 0, 1L, croppedImageFile.getName().toUpperCase(), "Brand", 100d));
             }
        }
        if(files.size()==0){
            searchResultList=getSearchResult(new File(imagePath),vectorStore,metric,topK);
        }

        return searchResultList;
    }

    public List<SearchResult> detectAndReturnSearchResultsByImageLabel(String imagePath, List<String> labels, VectorStore vectorStore, int topK) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(3);
        //Call Stand-alone code to get the cropped files data.
        Future<List<File>> croppedFilesFuture = executor.submit(() -> createCroppedImageAndReturnFiles(imagePath));

        List<File> files = croppedFilesFuture.get();
        List<SearchResult> searchResultList = new ArrayList<>();
        SimilarityMetric metric = new CosineSimilarity();
        for (File croppedImageFile : files) {
            List<SearchResult> fileSearchResult=getSearchResultByLabelandImages(croppedImageFile,labels,vectorStore,metric,topK);
            searchResultList.addAll(fileSearchResult);
            //String imagePath, List<String> detectedLabels, float score, Long productId,String name,String brand,double price
            searchResultList.add(new SearchResult(croppedImageFile.getPath().replace("target\\classes\\static\\",Utils.API_BASE_URL + "/"),List.of(croppedImageFile.getName()),0,1L,croppedImageFile.getName().toUpperCase(),"Brand",100d));
        }
        if(files.size()==0){
            searchResultList=getSearchResultByLabelandImages(new File(imagePath), labels,vectorStore,metric,topK);
        }

        return searchResultList;
    }

    private List<SearchResult> getSearchResult(File croppedImageFile, VectorStore vectorStore,SimilarityMetric metric,int topK) throws Exception{
        List<SearchResult> searchResultList = new ArrayList<>();
        Image croppedImage = ImageFactory.getInstance()
                .fromFile(Paths.get(croppedImageFile.getPath()));

        DetectedObjects detections = predict(croppedImage);

        List<String> result = new ArrayList<>();

        for (Classifications.Classification obj : detections.items()) {
            log.info("[Step 5] Calling Embedding On Cropped Object.... fileName={}", croppedImageFile.getName());
            String label;
            if(croppedImageFile.getName().contains("_")){
                label = croppedImageFile.getName().split("_")[1];
            }
            else{
                label="Shirt";
            }
            double prob = obj.getProbability();

            log.info("    [Step 5.1] Label={}, PROB={}", label, prob);

            // Optional filtering
            if (prob > 50d) {
                result.add(label);
                float[] queryVector = CroppedImageEmbedder.searchEmbeddingsOnCroppedImages(croppedImageFile, label);

                // ── STEP 6: Score all vectors in the store ─────────────────────────
                log.info("[Step 6] Searching vector store ({} entries)...", vectorStore.getAll().size());
                List<SearchResult> croppedSearchResults = vectorStore.getAll().stream()
                        .map(iv -> new SearchResult(
                                iv.imagePath,
                                iv.detectedLabels,
                                metric.similarity(queryVector, iv.vector), iv.productId, iv.name, iv.brand, iv.price
                        ))
                        .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                        .limit(topK)
                        .collect(Collectors.toList());

                searchResultList.addAll(croppedSearchResults);

            } else {
                log.warn("    [Step 5.1] Prob error <30%: {}", prob);
            }

        }
        return searchResultList;
    }

    private List<SearchResult> getSearchResultByLabelandImages(File croppedImageFile, List<String> labels, VectorStore vectorStore,SimilarityMetric metric,int topK) throws Exception{
        List<SearchResult> searchResultList = new ArrayList<>();
        Image croppedImage = ImageFactory.getInstance()
                .fromFile(Paths.get(croppedImageFile.getPath()));

        DetectedObjects detections = predict(croppedImage);

        List<String> result = new ArrayList<>();

        for (Classifications.Classification obj : detections.items()) {
            log.info("[Step 5] Calling Embedding On Cropped Object.... fileName={}", croppedImageFile.getName());
            String label;
            if(croppedImageFile.getName().contains("_")){
                label = croppedImageFile.getName().split("_")[1];
            }
            else{
                label="Shirt";
            }
            double prob = obj.getProbability();

            log.info("    [Step 5.1] Label={}, PROB={}", label, prob);

            // Optional filtering
            if (prob > 50d) {
                result.add(label);
                if(labels.size()>0){
                    result.addAll(labels);

                    result = result.stream().distinct().collect(Collectors.toList());
                }
                float[] queryVector = CroppedImageEmbedder.searchEmbeddingsOnCroppedImages(croppedImageFile, label);

                // ── STEP 6: Score all vectors in the store ─────────────────────────
                log.info("[Step 6] Searching vector store ({} entries)...", vectorStore.getAll().size());
                List<SearchResult> croppedSearchResults = vectorStore.getAll().stream()
                        .map(iv -> new SearchResult(
                                iv.imagePath,
                                iv.detectedLabels,
                                metric.similarity(queryVector, iv.vector), iv.productId, iv.name, iv.brand, iv.price
                        ))
                        .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                        .limit(topK)
                        .collect(Collectors.toList());

//                for (SearchResult searchResult : croppedSearchResults) {
//                    if (searchResult.detectedLabels == null) {
//                        searchResult.detectedLabels = new ArrayList<>();
//                    }
//                    searchResult.brand = obj.getClassName().toUpperCase();
//                    searchResult.score = (float) obj.getProbability();
//                    //System.out.println(searchResult.imagePath);
//                }
                searchResultList.addAll(croppedSearchResults);

            } else {
                log.warn("    [Step 5.1] Prob error <30%: {}", prob);
            }

        }
        return searchResultList;
    }

    @Override
    public List<String> detect(String imagePath) throws Exception {
        return null;
    }

    @Override
    public void close() {
        // Shared model resources live for the process lifetime and are not closed per request.
    }

    public static List<File> createCroppedImageAndReturnFiles(String imagePath) throws OrtException {
        log.info("[Step 3] Calling StandAlone Cropped Image version......");
        return StandAloneYoloObjectClassification.createCroppedImageAndReturnFiles(imagePath);
    }

    public List<SearchResult> queryImagesByLabel(VectorStore vectorStore, float[] queryVector, int topK) throws Exception {
//        SimilarityMetric metric = new CosineSimilarity();
//        List<SearchResult> croppedSearchResults = vectorStore.getAll().stream()
//                .map(iv -> new SearchResult(
//                        iv.imagePath,
//                        iv.detectedLabels,
//                        metric.similarity(queryVector, iv.vector), iv.productId, iv.name, iv.brand, iv.price
//                ))
//                .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
//                .limit(topK)
//                .collect(Collectors.toList());
//
//
        SearchResult searchResult = new SearchResult();
        searchResult.imagePath = findMostSimilar(vectorStore,queryVector);
        searchResult.name = "Test";
        return List.of(searchResult);
    }

    public String findMostSimilar(VectorStore vectorStore, float[] queryEmbedding) {
        SimilarityMetric metric = new CosineSimilarity();
        String bestName = null;
        double bestScore = -1;

        for (ImageVector imageVector : vectorStore.getAll()) {
            double score = metric.similarity(queryEmbedding, imageVector.vector);
            if (score > bestScore) {
                bestScore = score;
                bestName = imageVector.imagePath;
            }
        }
        return bestName; // name of the most similar image
    }
}
