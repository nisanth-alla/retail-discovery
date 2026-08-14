package com.innova.visual_retail_discovery.controller;

import com.innova.visual_retail_discovery.service.tryon.ReplicateVirtualTryOnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * POST /api/tryon
 *
 * Accepts a person photo + garment image, calls the Replicate IDM-VTON model,
 * and returns a URL to the AI-generated try-on result.
 */
@RestController
@RequestMapping("/api")
    @CrossOrigin(originPatterns = "${app.cors.origin}")
public class TryOnController {

    @Autowired
    private ReplicateVirtualTryOnService tryOnService;

    /**
     * @param personImage  The user's photo (any format)
     * @param garmentImage A photo of the garment to try on
     * @param category     "upper_body" | "lower_body" | "dresses"  (default: upper_body)
     * @param garmentDesc  Short text description of the garment     (default: "a garment")
     * @return JSON: { "result_url": "https://..." }
     */
    @PostMapping(value = "/tryon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> tryOn(
            @RequestParam("person_image")  MultipartFile personImage,
            @RequestParam("garment_image") MultipartFile garmentImage,
            @RequestParam(value = "category",     defaultValue = "upper_body") String category,
            @RequestParam(value = "garment_desc", defaultValue = "a garment")  String garmentDesc
    ) {
        try {
            String resultUrl = tryOnService.generateTryOn(
                    personImage, garmentImage, category, garmentDesc);
            return ResponseEntity.ok(Map.of("result_url", resultUrl));
        } catch (IllegalStateException e) {
            // Token not configured
            return ResponseEntity.status(503)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
