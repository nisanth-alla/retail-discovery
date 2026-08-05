package com.innova.visual_retail_discovery.unittests;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import com.innova.visual_retail_discovery.service.translator.EnhancedYoloV11Translator;
import com.innova.visual_retail_discovery.utils.Utils;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.innova.visual_retail_discovery.utils.Utils.SAMPLE_IMAGE;
import static com.innova.visual_retail_discovery.utils.Utils.TRAINED_MODEL_PATH;

public class TestBasicYoloObjectClassification {

    public static void main(String[] args) throws IOException, MalformedModelException {
        String modelPath = TRAINED_MODEL_PATH;
        String imagePath = SAMPLE_IMAGE;

        // 1️Load the model
        try (Model model = Model.newInstance("yolo11n", "OnnxRuntime")) {

            // Load your ONNX model file
            model.load(Paths.get(modelPath));

            // 2 Create a predictor with your custom Translator
            Translator<Image, DetectedObjects> translator =
                    new EnhancedYoloV11Translator(List.of(
                            Utils.SUPPORTED_CLASSES
                    ));

            try (Predictor<Image, DetectedObjects> predictor = model.newPredictor(translator)) {

                // 3 Load the image
                Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));


                // Run prediction
                DetectedObjects detections = predictor.predict(img);

                // 5 Print results
                if (detections.items().isEmpty()) {
                    System.out.println("No detections found.");
                } else {
                    System.out.println("Detected objects:");

                    //                    detections.items().forEach(obj ->
                    //                            System.out.println(obj.toString())
                    //                            /*System.out.printf("Label: %s, Probability: %.2f%n",
                    //                                    obj.getClassName(), obj.getProbability());*/
                    //                    );
                    // Loop over detected objects
                    for (Classifications.Classification obj : detections.items()) {

                        // Cast to DetectedObject
                        DetectedObjects.DetectedObject obj2 = (DetectedObjects.DetectedObject) obj;
                        cropImage(obj2, imagePath);
                        System.out.println(obj2.getClassName());
                    }

                }
            } catch (TranslateException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void cropImage(DetectedObjects.DetectedObject obj2, String imagePath) throws Exception {
        // Load your original image
        BufferedImage originalImage = ImageIO.read(new File(imagePath));
        int origWidth = originalImage.getWidth();
        int origHeight = originalImage.getHeight();

        float modelInputWidth = 640;
        float modelInputHeight = 640;

        float scaleX = origWidth / modelInputWidth;   // 682 / 640 ≈ 1.065625
        float scaleY = origHeight / modelInputHeight; // 1024 / 640 ≈ 1.6

        // Assume you already have a detected object

        ai.djl.modality.cv.output.BoundingBox box = obj2.getBoundingBox();

        if (box instanceof ai.djl.modality.cv.output.Rectangle) {
            Rectangle rect = (Rectangle) box;
            int cropW = (int) rect.getWidth(), cropH = (int) rect.getHeight(), cropX = (int) rect.getX(), cropY = (int) rect.getY();
            if (cropW > 0 && cropH > 0) {
                BufferedImage cropped = originalImage.getSubimage(cropX, cropY, cropW-cropX, cropH-cropY);
                ImageIO.write(cropped, "jpg", new File("cropped_" + obj2.getClassName() + ".jpg"));
                System.out.println("Cropped image saved for " + obj2.getClassName());
            } else {
                System.out.println("Skipping invalid box for " + obj2.getClassName());
            }
        }
    }

}
