package com.innova.visual_retail_discovery.service.engine;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * YOLO Fashion Labeler — Plain Java
 * Sends images to Anthropic Claude, gets bounding boxes, saves YOLO .txt labels.
 *
 * Dependencies: org.json (json-20231013.jar)
 * Compile: javac -cp .:json-20231013.jar YoloFashionLabeler.java
 * Run:     java  -cp .:json-20231013.jar YoloFashionLabeler <image_dir> <output_dir>
 */
public class YoloFashionLabeler {

    private static final Logger log = LoggerFactory.getLogger(YoloFashionLabeler.class);

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String API_KEY   = System.getenv("ANTHROPIC_API_KEY");
    private static final String API_URL   = "https://api.anthropic.com/v1/messages";
    private static final String MODEL     = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 1000;

    // ── Class list (index = YOLO class id) ───────────────────────────────────
    private static final List<String> CLASSES = List.of(
            "shirt", "top", "sweater", "cardigan", "jacket", "vest",
            "pants", "shorts", "skirt", "coat", "bead", "watch",
            "cape", "glasses", "hat", "scarf"
    );

    private static final Set<String> SUPPORTED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("ERROR: Set ANTHROPIC_API_KEY environment variable.");
            System.exit(1);
        }

        String inputDir  = args.length > 0 ? args[0] : ".";
        String outputDir = args.length > 1 ? args[1] : "labels";

        Path inPath  = Path.of(inputDir);
        Path outPath = Path.of(outputDir);
        Files.createDirectories(outPath);

        // Write classes.txt
        writeClassesFile(outPath);

        // Process each image
        File[] images = inPath.toFile().listFiles(f ->
                SUPPORTED_EXT.contains(getExtension(f.getName()).toLowerCase())
        );

        if (images == null || images.length == 0) {
            log.warn("No images found in: {}", inputDir);
            return;
        }

        log.info("Found {} image(s). Processing...", images.length);

        HttpClient client = HttpClient.newHttpClient();
        int success = 0, failed = 0;

        for (File img : images) {
            log.info("Processing: {}", img.getName());
            try {
                List<Detection> detections = detect(client, img);
                String labelName = getBaseName(img.getName()) + ".txt";
                Path   labelPath = outPath.resolve(labelName);
                writeYoloLabel(labelPath, detections);
                log.info("{} detection(s) -> {}", detections.size(), labelPath);
                success++;
            } catch (Exception e) {
                log.error("FAILED: {}", e.getMessage());
                failed++;
            }

            // Brief pause to respect rate limits
            Thread.sleep(500);
        }

        log.info("Done. {} succeeded, {} failed.", success, failed);
        log.info("Labels saved to: {}", outPath.toAbsolutePath());
    }

    // ── Core detection ────────────────────────────────────────────────────────
    private static List<Detection> detect(HttpClient client, File imageFile) throws Exception {
        String base64  = encodeImage(imageFile);
        String mime    = getMimeType(imageFile.getName());
        String prompt  = buildPrompt();

        // Build JSON request body
        JSONObject imageSource = new JSONObject()
                .put("type", "base64")
                .put("media_type", mime)
                .put("data", base64);

        JSONObject imageBlock = new JSONObject()
                .put("type", "image")
                .put("source", imageSource);

        JSONObject textBlock = new JSONObject()
                .put("type", "text")
                .put("text", prompt);

        JSONObject message = new JSONObject()
                .put("role", "user")
                .put("content", new JSONArray().put(imageBlock).put(textBlock));

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("max_tokens", MAX_TOKENS)
                .put("messages", new JSONArray().put(message));

        // Send request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    // ── Parse API response ────────────────────────────────────────────────────
    private static List<Detection> parseResponse(String responseBody) {
        JSONObject json    = new JSONObject(responseBody);
        JSONArray  content = json.getJSONArray("content");

        // Collect all text blocks
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.getJSONObject(i);
            if ("text".equals(block.getString("type"))) {
                sb.append(block.getString("text"));
            }
        }

        String text = sb.toString().trim();

        // Strip possible markdown fences
        text = text.replaceAll("(?s)```json\\s*|```", "").trim();

        // Extract JSON array
        int start = text.indexOf('[');
        int end   = text.lastIndexOf(']');
        if (start == -1 || end == -1) return Collections.emptyList();

        JSONArray arr = new JSONArray(text.substring(start, end + 1));
        List<Detection> detections = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String cls = obj.optString("class", "");
            if (!CLASSES.contains(cls)) continue;   // ignore unknown classes

            detections.add(new Detection(
                    CLASSES.indexOf(cls),
                    cls,
                    obj.getDouble("x_center"),
                    obj.getDouble("y_center"),
                    obj.getDouble("width"),
                    obj.getDouble("height")
            ));
        }
        return detections;
    }

    // ── Write YOLO label file ─────────────────────────────────────────────────
    private static void writeYoloLabel(Path path, List<Detection> dets) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (Detection d : dets) {
                w.write(String.format(Locale.US, "%d %.6f %.6f %.6f %.6f%n",
                        d.classId, d.xCenter, d.yCenter, d.width, d.height));
            }
        }
    }

    // ── Write classes.txt ─────────────────────────────────────────────────────
    private static void writeClassesFile(Path outDir) throws IOException {
        Path p = outDir.resolve("classes.txt");
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            for (String cls : CLASSES) {
                w.write(cls);
                w.newLine();
            }
        }
        log.info("classes.txt written -> {}", p);
    }

    // ── Prompt ────────────────────────────────────────────────────────────────
    private static String buildPrompt() {
        return "Detect all visible fashion/clothing items in this image from this list: "
                + String.join(", ", CLASSES) + ".\n"
                + "For each detected item output a JSON array of objects with:\n"
                + "- \"class\": exact class name from the list\n"
                + "- \"x_center\": normalized x center (0-1)\n"
                + "- \"y_center\": normalized y center (0-1)\n"
                + "- \"width\": normalized width (0-1)\n"
                + "- \"height\": normalized height (0-1)\n"
                + "Respond ONLY with a valid JSON array, no markdown, no explanation.";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String encodeImage(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String getMimeType(String filename) {
        return switch (getExtension(filename).toLowerCase()) {
            case ".png"  -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif"  -> "image/gif";
            default      -> "image/jpeg";
        };
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private static String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    // ── Detection record ──────────────────────────────────────────────────────
    record Detection(int classId, String className,
                     double xCenter, double yCenter,
                     double width,   double height) {}
}