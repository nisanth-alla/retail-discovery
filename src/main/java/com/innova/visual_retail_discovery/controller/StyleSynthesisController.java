package com.innova.visual_retail_discovery.controller;

import com.innova.visual_retail_discovery.model.SearchResult;
import com.innova.visual_retail_discovery.model.SmartSwapResult;
import com.innova.visual_retail_discovery.model.StyleRecommendationResponse;
import com.innova.visual_retail_discovery.service.detector.FashionDetectionApp;
import com.innova.visual_retail_discovery.service.style.StyleSynthesisService;
import com.innova.visual_retail_discovery.service.vector.ImageUtil;
import com.innova.visual_retail_discovery.service.vector.JsonVectorStore;
import com.innova.visual_retail_discovery.service.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Pillar 2 – Style Synthesis & Smart Swaps.
 *
 * POST /api/style/complete-look  →  visual search + "Complete the Look" panel
 * POST /api/style/smart-swaps    →  visual search + "Similarity-with-Benefits" panel
 * POST /api/style/full           →  both panels in one response
 */
@RestController
@RequestMapping("/api/style")
    @CrossOrigin(originPatterns = "${app.cors.origin}")
public class StyleSynthesisController {

    private static final Logger log = LoggerFactory.getLogger(StyleSynthesisController.class);
    private static final String STORE_PATH = "embeddings";

    private final StyleSynthesisService styleSynthesisService = new StyleSynthesisService();

    // ── 1. Complete-the-Look ──────────────────────────────────────────────────

    /**
     * Given a reference image, return the original matches PLUS complementary
     * products that "complete the outfit".
     *
     * curl -X POST http://localhost:8080/api/style/complete-look \
     *      -F "file=@/path/to/image.jpg"
     */
    @PostMapping(value = "/complete-look", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StyleRecommendationResponse> completeLook(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[complete-look] received image: {}, {} bytes",
                file.getOriginalFilename(), file.getSize());

        List<SearchResult> matches = runVisualSearch(file);

        VectorStore vectorStore = loadVectorStore();
        List<SearchResult> complementary = styleSynthesisService
                .getComplementaryProducts(matches, vectorStore);

        return ResponseEntity.ok(new StyleRecommendationResponse(matches, complementary, Collections.emptyList()));
    }

    // ── 2. Smart Swaps ────────────────────────────────────────────────────────

    /**
     * Given a reference image, return the original matches PLUS value-added
     * alternatives (better price / rating / promotion).
     *
     * curl -X POST http://localhost:8080/api/style/smart-swaps \
     *      -F "file=@/path/to/image.jpg"
     */
    @PostMapping(value = "/smart-swaps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StyleRecommendationResponse> smartSwaps(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[smart-swaps] received image: {}, {} bytes",
                file.getOriginalFilename(), file.getSize());

        List<SearchResult> matches = runVisualSearch(file);

        VectorStore vectorStore = loadVectorStore();
        List<SmartSwapResult> swaps = styleSynthesisService.getSmartSwaps(matches, vectorStore);

        return ResponseEntity.ok(new StyleRecommendationResponse(matches, Collections.emptyList(), swaps));
    }

    // ── 3. Full Style Synthesis (both panels) ─────────────────────────────────

    /**
     * One-shot endpoint: visual search + complete-look + smart-swaps.
     *
     * curl -X POST http://localhost:8080/api/style/full \
     *      -F "file=@/path/to/image.jpg"
     */
    @PostMapping(value = "/full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StyleRecommendationResponse> fullStyleSynthesis(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("[style/full] received image: {}, {} bytes",
                file.getOriginalFilename(), file.getSize());

        List<SearchResult> matches = runVisualSearch(file);

        VectorStore vectorStore = loadVectorStore();
        List<SearchResult>  complementary = styleSynthesisService.getComplementaryProducts(matches, vectorStore);
        List<SmartSwapResult> swaps       = styleSynthesisService.getSmartSwaps(matches, vectorStore);

        return ResponseEntity.ok(new StyleRecommendationResponse(matches, complementary, swaps));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<SearchResult> runVisualSearch(MultipartFile file) throws Exception {
        File jpeg = ImageUtil.convertToJpeg(file, "output_style.jpg");
        return FashionDetectionApp.searchImage(jpeg);
    }

    private VectorStore loadVectorStore() throws Exception {
        VectorStore store = new JsonVectorStore(STORE_PATH);
        if (!store.isEmpty()) {
            store.load();
        }
        return store;
    }
}
