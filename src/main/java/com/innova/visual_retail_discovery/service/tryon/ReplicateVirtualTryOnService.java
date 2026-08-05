package com.innova.visual_retail_discovery.service.tryon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

/**
 * Calls the Replicate IDM-VTON model to produce a photorealistic virtual try-on image.
 *
 * Model: cuuupid/idm-vton (Improving Diffusion Models for Authentic Virtual Try-on)
 * Docs: https://replicate.com/cuuupid/idm-vton
 *
 * Get a free API token at https://replicate.com/account/api-tokens
 * Set replicate.api.token in application.properties
 */
@Service
public class ReplicateVirtualTryOnService {

    private static final String REPLICATE_BASE = "https://api.replicate.com/v1";
    // Use the model endpoint so it always picks the latest published version
    private static final String MODEL_PREDICTIONS_URL =
            REPLICATE_BASE + "/models/cuuupid/idm-vton/predictions";

    @Value("${replicate.api.token:}")
    private String replicateToken;

    private final ObjectMapper objectMapper;

    // Dedicated RestTemplate with a 150-second read timeout (AI inference takes 30–120 s)
    private final RestTemplate restTemplate;

    public ReplicateVirtualTryOnService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(150_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateTryOn(MultipartFile personImage,
                                MultipartFile garmentImage,
                                String category,
                                String garmentDesc) throws Exception {
        if (replicateToken == null || replicateToken.isBlank()
                || replicateToken.equals("YOUR_REPLICATE_API_TOKEN_HERE")) {
            throw new IllegalStateException(
                    "Replicate API token not configured. " +
                    "Get a free token at https://replicate.com/account/api-tokens and " +
                    "set replicate.api.token in application.properties");
        }

        String personDataUri  = toDataUri(personImage);
        String garmentDataUri = toDataUri(garmentImage);

        // Build Replicate prediction request
        ObjectNode input = objectMapper.createObjectNode();
        input.put("human_img",     personDataUri);
        input.put("garm_img",      garmentDataUri);
        input.put("garment_des",   garmentDesc != null && !garmentDesc.isBlank() ? garmentDesc : "a garment");
        input.put("is_checked",    true);
        input.put("is_checked_crop", false);
        input.put("denoise_steps", 30);
        input.put("seed",          42);
        input.put("category",      mapCategory(category));

        ObjectNode body = objectMapper.createObjectNode();
        body.set("input", input);

        HttpHeaders headers = buildHeaders();
        // "Prefer: wait" tells Replicate to respond synchronously (up to 60 s)
        headers.set("Prefer", "wait=60");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> createResp;
        try {
            createResp = restTemplate.exchange(
                    MODEL_PREDICTIONS_URL, HttpMethod.POST, entity, String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String replicateError = ex.getResponseBodyAsString();
            throw new RuntimeException("Replicate API error " + ex.getStatusCode() + ": " + replicateError);
        }

        JsonNode predNode = objectMapper.readTree(createResp.getBody());
        String status = predNode.path("status").asText();

        // Synchronous response completed immediately
        if ("succeeded".equals(status)) {
            return extractOutput(predNode);
        }

        // If still processing, fall back to polling
        String predId = predNode.path("id").asText();
        if (predId == null || predId.isBlank()) {
            throw new RuntimeException("Replicate returned no prediction ID. Response: " + createResp.getBody());
        }
        return pollForResult(predId);
    }

    private String pollForResult(String predictionId) throws Exception {
        String pollUrl = REPLICATE_BASE + "/predictions/" + predictionId;
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        long deadline = System.currentTimeMillis() + 120_000L; // 2-minute absolute timeout

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(3_000);
            ResponseEntity<String> resp = restTemplate.exchange(
                    pollUrl, HttpMethod.GET, entity, String.class);
            JsonNode node = objectMapper.readTree(resp.getBody());
            String status = node.path("status").asText();

            switch (status) {
                case "succeeded" -> { return extractOutput(node); }
                case "failed", "canceled" -> throw new RuntimeException(
                        "Prediction " + status + ": " + node.path("error").asText("(no detail)"));
                // "starting" / "processing" — keep waiting
            }
        }
        throw new RuntimeException("Prediction did not complete within 2 minutes");
    }

    private String extractOutput(JsonNode node) {
        JsonNode output = node.path("output");
        if (output.isArray() && !output.isEmpty()) {
            return output.get(0).asText();
        }
        if (output.isTextual()) {
            return output.asText();
        }
        throw new RuntimeException("Unexpected Replicate output format: " + output);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", "Bearer " + replicateToken);
        return h;
    }

    private String toDataUri(MultipartFile file) throws Exception {
        String mime = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String b64  = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + mime + ";base64," + b64;
    }

    private String mapCategory(String category) {
        if (category == null) return "upper_body";
        return switch (category.toLowerCase()) {
            case "lower_body", "bottom", "pants", "skirt" -> "lower_body";
            case "dresses", "dress", "jumpsuit", "full"   -> "dresses";
            default                                         -> "upper_body";
        };
    }
}
