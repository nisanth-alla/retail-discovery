package com.innova.visual_retail_discovery.controller;

import com.innova.visual_retail_discovery.service.embeddings.impl.TextEmbeddingService;
import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.service.detector.FashionDetectionApp;
import com.innova.visual_retail_discovery.service.engine.StyleRuleEngine;
import com.innova.visual_retail_discovery.service.vector.*;
import com.innova.visual_retail_discovery.unittests.FashionEmbeddingSemanticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/image")
public class ImageSearchController {

    private static final Logger log = LoggerFactory.getLogger(ImageSearchController.class);
    @Autowired
    private VendorService vendorService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Lazy
    TextEmbeddingService textEmbeddingService;

    private static final List<String> IMAGE_EXTENSIONS =
            List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg");

    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<SearchResult>> search(@RequestParam("file") MultipartFile file, @RequestParam(required = false,name="isFindSimilar") boolean isFindSimilar) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        log.info("Received image: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        File jpeg;
        try {
            jpeg = ImageUtil.convertToJpeg(file, "output.jpg");
        } catch (IOException e) {
            log.warn("Rejected unreadable image upload: {}", file.getOriginalFilename());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        FashionDetectionApp fashionDetectionApp = new FashionDetectionApp();

        List<SearchResult> searchResults = fashionDetectionApp.searchImage(jpeg);

        searchResults = searchResults.stream().filter(searchResult -> !searchResult.imagePath.contains("output.jpg")).collect(Collectors.toList());

        return ResponseEntity.ok(searchResults);
    }


    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(@RequestPart("images") List<MultipartFile> images) throws IOException {
        vendorService.registerProducts(images);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @GetMapping(value = "/fetch")
    public ResponseEntity<Map<String, Object>> fetchimages() throws IOException {
        Resource folderResource = new ClassPathResource("static/datastore");

        if (!folderResource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Datastore folder not found"));
        }

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/static/datastore/*");
        List<Map<String, String>> images = Arrays.stream(resources)
                    .filter(Resource::isReadable)
                    .filter(resource -> resource.getFilename() != null && isImageFile(resource.getFilename()))
                    .sorted(Comparator.comparing(resource -> resource.getFilename()))
                    .map(resource -> {
                        String filename = resource.getFilename();
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("filename", filename);
                        entry.put("url", "/datastore/" + filename);
                        return entry;
                    })
                    .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", images.size());
        response.put("images", images);

        return ResponseEntity.ok(response);
    }

    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @PostMapping(value = "/searchByLabel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<SearchResult>> searchByImageLabelsAndPrice(@RequestParam(name="file") MultipartFile file ,
                                                                          @RequestParam("label") List<String> labels,
                                                                          @RequestParam("price") Double price) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        log.info("Received image: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        File jpeg = null;
        if(file!=null) {
            jpeg = ImageUtil.convertToJpeg(file, "output.jpg");
        }
        FashionDetectionApp fashionDetectionApp = new FashionDetectionApp();

        List<SearchResult> searchResults = fashionDetectionApp.searchImageByFileLabelAndPrice(jpeg, labels, price);

        return ResponseEntity.ok(searchResults);
    }

    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @PostMapping(value = "/searchtext")
    public ResponseEntity<Map<String, Object>> searchtext(@RequestParam String text) throws Exception {

        Resource folderResource = new ClassPathResource("static/datastore");


        if (!folderResource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Datastore folder not found"));
        }
        //If it sentense then go for semantic search.
        if(text.length()>20){
            FashionEmbeddingSemanticService fashionEmbeddingSemanticService = new FashionEmbeddingSemanticService();
            List<SearchResult> uniqueSortedResults = fashionEmbeddingSemanticService.getSemanticResults(text);

            List<Map<String,String>> imagesMap = new ArrayList<>();
            for(SearchResult searchResultItr: uniqueSortedResults) {
                if (searchResultItr.score * 100 > 50) {
                    Map<String, String> img = new HashMap<>();
                    img.put("filename", searchResultItr.name);
                    img.put("score", String.valueOf(searchResultItr.score * 100));
                    img.put("brand", searchResultItr.brand);
                    img.put("url", toPublicImageUrl(searchResultItr.imagePath));
                    imagesMap.add(img);
                } else {
                    log.debug("Score filtered out: {}", searchResultItr.score * 100);
                }
            }


            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", imagesMap.size());
            response.put("images", imagesMap);

            return ResponseEntity.ok(response);

        }

        VectorStore vectorStore = new JsonVectorStore("embeddings");
        vectorStore.load();

        SimilarityMetric metric = new CosineSimilarity();
        List<SearchResult> listOfResults = new ArrayList<>();

        float[] queryVector = textEmbeddingService.embed(List.of(text));

        // Query vector store with cosine similarity, top-5 per outfit component
        List<SearchResult> results = vectorStore.getAll().stream()
                .map(iv -> new SearchResult(iv.imagePath, iv.detectedLabels,
                        metric.similarity(queryVector, iv.lableVector), iv.productId, iv.name, iv.brand, iv.price))
                .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                .limit(5)
                .collect(Collectors.toList());

        listOfResults.addAll(results);

        // Deduplicate by imagePath keeping highest score, sort by score descending, normalize paths
        List<SearchResult> uniqueSortedResults = listOfResults.stream()
                .collect(Collectors.toMap(
                        r -> r.imagePath,
                        r -> r,
                        (a, b) -> a.score >= b.score ? a : b,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                .collect(Collectors.toList());

        List<Map<String,String>> imagesMap = new ArrayList<>();

        for(SearchResult searchResult: uniqueSortedResults){
            if(searchResult.score*100>50){
                Map<String, String> img = new HashMap<>();
                img.put("filename", searchResult.name);
                img.put("score", String.valueOf(searchResult.score*100));
                img.put("brand", searchResult.brand);
                img.put("url", toPublicImageUrl(searchResult.imagePath));
                imagesMap.add(img);
            }else{
                log.debug("Score filtered out: {}", searchResult.score * 100);
            }

        }

//        List<Map<String, String>> images;
//        try (var stream = Files.list(folderPath)) {
//            images = stream
//                    .filter(Files::isRegularFile)
//                    .filter(p -> isImageFile(p.getFileName().toString())&& text!=null && p.getFileName().toString().toLowerCase().contains(text))
//                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
//                    .map(p -> {
//                        String filename = p.getFileName().toString();
//                        Map<String, String> entry = new LinkedHashMap<>();
//                        entry.put("filename", filename);
//                        entry.put("url", "http://localhost:8080/datastore/" + filename);
//                        return entry;
//                    })
//                    .limit(5).collect(Collectors.toList());
//        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", imagesMap.size());
        response.put("images", imagesMap);

        return ResponseEntity.ok(response);
    }

    @CrossOrigin(originPatterns = "${app.cors.origin}", allowCredentials = "true")
    @PostMapping(value = "/styleIt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> styleIt(@RequestParam(required = false) String text,@RequestParam(value = "file",required = false) MultipartFile inputFile) throws Exception {
        VectorStore vectorStore = new JsonVectorStore("embeddings");
        vectorStore.load();

        SimilarityMetric metric = new CosineSimilarity();
        Map<String, List<String>> queryResponse = new HashMap<>();
        String type = inputFile!=null?"image":"text";
        Map<String,List<SearchResult>> listOfResults = new HashMap<>();
        if (inputFile != null) {
            File jpeg = ImageUtil.convertToJpeg(inputFile, "output.jpg");
            FashionDetectionApp fashionDetectionApp = new FashionDetectionApp();

            List<SearchResult> searchResults = fashionDetectionApp.searchImage(jpeg);

            List<String> uniqueLabels =
                    searchResults.stream()
                            .flatMap(sr -> sr.detectedLabels.stream())
                            .distinct()
                            .collect(Collectors.toList());

            for (String label : uniqueLabels) {
                log.info("label = "+label);
                queryResponse = StyleRuleEngine.query(label);

                listOfResults.putAll(getResultsByType(type, queryResponse, textEmbeddingService,vectorStore,metric));
            }
        } else {
            log.info("textLabel = "+text);
            queryResponse = StyleRuleEngine.query(text);
            listOfResults = getResultsByType(type, queryResponse, textEmbeddingService,vectorStore,metric);
        }




        List<Map<String,String>> imagesMap = new ArrayList<>();

        for(Map.Entry<String,List<SearchResult>> searchItr : listOfResults.entrySet()){
            for(SearchResult searchResult: searchItr.getValue()){
                if(!searchItr.getKey().equals("_input_summary")){
                    Map<String, String> img = new HashMap<>();
                    img.put("filename", searchResult.name);
                    img.put("style",searchItr.getKey());
                    img.put("url", toPublicImageUrl(searchResult.imagePath));
                    imagesMap.add(img);
                }
            }
        }


        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", imagesMap.size());
        response.put("images", imagesMap);

        return ResponseEntity.ok(response);
    }

    private Map<String, List<SearchResult>> getResultsByType(String type, Map<String, List<String>> queryResponse, TextEmbeddingService textEmbeddingService, VectorStore vectorStore, SimilarityMetric metric) throws Exception {

        Map<String,List<SearchResult>> listOfResults = new HashMap<>();
        for (Map.Entry<String, List<String>> entrySet : queryResponse.entrySet()) {
            List<String> labels = entrySet.getValue();
            log.info("Querying For Key{} with labels: {}",entrySet.getKey(), labels);

            // Embed all labels for this outfit component as one text query
            float[] queryVector = textEmbeddingService.embed(labels);


            if(type.equalsIgnoreCase("image")){
                log.info("Getting Results from Given Image");
                // Query vector store with cosine similarity, top-5 per outfit component
                List<SearchResult> results = vectorStore.getAll().stream()
                        .map(iv -> new SearchResult(iv.imagePath, iv.detectedLabels,
                                metric.similarity(queryVector, iv.vector), iv.productId, iv.name, iv.brand, iv.price))
                        .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                        .limit(2)
                        .collect(Collectors.toList());

                listOfResults.put(entrySet.getKey(), results);
            }else{
                log.info("Getting Results from Given Text");
                // Query vector store with cosine similarity, top-5 per outfit component
                List<SearchResult> results = vectorStore.getAll().stream()
                        .map(iv -> new SearchResult(iv.imagePath, iv.detectedLabels,
                                metric.similarity(queryVector, iv.lableVector), iv.productId, iv.name, iv.brand, iv.price))
                        .sorted(Comparator.comparingDouble((SearchResult r) -> r.score).reversed())
                        .limit(2)
                        .collect(Collectors.toList());

                listOfResults.put(entrySet.getKey(), results);
            }

        }
        return listOfResults;
    }



    private String toPublicImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        String normalizedPath = imagePath.replace('\\', '/');
        int staticIndex = normalizedPath.indexOf("/static/");
        if (staticIndex >= 0) {
            return normalizedPath.substring(staticIndex + "/static".length());
        }
        if (normalizedPath.startsWith("/datastore/") || normalizedPath.startsWith("/cropped/")) {
            return normalizedPath;
        }
        return "/" + normalizedPath;
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

}
