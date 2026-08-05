package com.innova.visual_retail_discovery.unittests; /**
 * YOLO Inference in Java — ONNX Runtime ONLY (no OpenCV needed)
 * =============================================================
 * Maven dependency (pom.xml) — only this one:
 *
 * <dependency>
 * <groupId>com.microsoft.onnxruntime</groupId>
 * <artifactId>onnxruntime</artifactId>
 * <version>1.18.0</version>
 * </dependency>
 * <p>
 * Image loading uses pure Java ImageIO — no native DLL required.
 */

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;

import ai.djl.modality.cv.output.Rectangle;
import com.innova.visual_retail_discovery.service.embeddings.impl.CroppedImageEmbedder;
import com.innova.visual_retail_discovery.service.translator.YoloV11TranslatorV1;
import com.innova.visual_retail_discovery.utils.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestYoloObjectClassificationWithEmbeddingSearch {

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String MODEL_PATH = Utils.TRAINED_MODEL_PATH;
    private static final String IMAGE_PATH = Utils.SAMPLE_IMAGE;
    private static final String OUTPUT_FILE = Utils.OUTPUT_FILE_PATH;
    private static final int IMG_SIZE = 640;
    private static final float CONF_THRESH = 0.1f;
    private static final float IOU_THRESH = 0.45f;

    // Must match the order in your dataset.yaml "names"
    private static final String[] CLASS_NAMES = Utils.SUPPORTED_CLASSES;

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {


        // Load image
        Image img = ImageFactory.getInstance()
                .fromFile(Paths.get("361.jpg"));

        // Load ONNX model
        //System.setProperty("ai.djl.default_engine", "OnnxRuntime");
        Model model = Model.newInstance("yolo11n", "OnnxRuntime");
        model.load(Paths.get("binary_brains_m2.onnx"));

        // Create predictor (use your custom translator)
        try (Predictor<Image, DetectedObjects> predictor =
                     model.newPredictor(new YoloV11TranslatorV1(List.of(Utils.SUPPORTED_CLASSES)))) {

            long t0 = System.currentTimeMillis();

            // 🔥 This replaces your ONNX Runtime code
            DetectedObjects detections = predictor.predict(img);

//            long t1 = System.currentTimeMillis();
//
//            System.out.println("Inference time: " + (t1 - t0) + " ms");
//
//            System.out.println(detections.items().size() + " items detected");
//
//            // Print detections
//            for (Classifications.Classification obj : detections.items()) {
//                System.out.println(
//                        obj.getClassName() + " : " + obj.getProbability()
//                );
//                EmbeddingOnCroppedImages.searchEmbeddingsOnCroppedImages(obj);
//
//            }


            // Iterate over detected objects
            for (int i = 0; i < detections.getNumberOfObjects(); i++) {
                Classifications.Classification obj = detections.item(i);
                BoundingBox box =((DetectedObjects.DetectedObject) obj).getBoundingBox();
                Rectangle rect = (Rectangle) box;

                // Coordinates are normalized (0..1), convert to pixels
                int x = (int) (rect.getX() * img.getWidth());
                int y = (int) (rect.getY() * img.getHeight());
                int w = (int) (rect.getWidth() * img.getWidth());
                int h = (int) (rect.getHeight() * img.getHeight());

                System.out.println(x + "  "+y+"  "+w +"  "+h);

                // Clamp values to image bounds
                x = Math.max(0, x);
                y = Math.max(0, y);
                w = Math.min(w, img.getWidth() - x);
                h = Math.min(h, img.getHeight() - y);

                Image cropped = img.getSubImage(100, 100, 100, 100);

                // Save cropped image to disk
                String fileName = obj.getClassName() + "_" + i + ".jpg";
                Path outputPath = Paths.get(fileName);
                cropped.save(Files.newOutputStream(outputPath), "jpg");

                System.out.println("Saved: " + fileName + " | Confidence: " + obj.getProbability());
                CroppedImageEmbedder.searchEmbeddingsOnCroppedImages(null,null);
            }
        }
        }
}

