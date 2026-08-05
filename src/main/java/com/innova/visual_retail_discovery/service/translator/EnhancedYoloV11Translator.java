package com.innova.visual_retail_discovery.service.translator;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * YOLOv11 Translator for DJL + ONNX Runtime
 */
public class EnhancedYoloV11Translator implements Translator<Image, DetectedObjects> {

    private final List<String> classes;
    private final float confThreshold;
    private final float nmsThreshold;
    private final int inputWidth;
    private final int inputHeight;

    public EnhancedYoloV11Translator(List<String> classes) {
        this(classes, 0.02f, 0.45f, 640, 640);
    }

    public EnhancedYoloV11Translator(List<String> classes, float confThreshold, float nmsThreshold,
                                     int inputWidth, int inputHeight) {
        this.classes = classes;
        this.confThreshold = confThreshold;
        this.nmsThreshold = nmsThreshold;
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
    }

    // --------------------- processInput ---------------------
//    @Override
//    public NDList processInput(TranslatorContext ctx, Image input) {
//        BufferedImage orig = (BufferedImage) input.getWrappedImage();
//
//        // Resize image to model input size
//        BufferedImage resized = new BufferedImage(inputWidth, inputHeight, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g = resized.createGraphics();
//        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        g.drawImage(orig, 0, 0, inputWidth, inputHeight, null);
//        g.dispose();
//
//        // Convert pixels to float array in CHW format (0-1)
//        float[] data = new float[3 * inputWidth * inputHeight];
//        int plane = inputWidth * inputHeight;
//        for (int y = 0; y < inputHeight; y++) {
//            for (int x = 0; x < inputWidth; x++) {
//                int rgb = resized.getRGB(x, y);
//                int idx = y * inputWidth + x;
//                data[idx] = ((rgb >> 16) & 0xFF) / 255f;          // R
//                data[plane + idx] = ((rgb >> 8) & 0xFF) / 255f;   // G
//                data[2 * plane + idx] = (rgb & 0xFF) / 255f;     // B
//            }
//        }
//
//        NDArray array = ctx.getNDManager().create(data);
//        return new NDList(array);
//    }
    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        Object wrapped = input.getWrappedImage();
        BufferedImage orig;

        // Save original image for later use in processOutput
        ctx.setAttachment("originalImage", input);

        // Convert to BufferedImage if needed
        if (wrapped instanceof BufferedImage) {
            orig = (BufferedImage) wrapped;
        } else if (wrapped instanceof org.opencv.core.Mat mat) {
            // Convert OpenCV Mat → BufferedImage
            int type = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
            int bufferSize = mat.channels() * mat.cols() * mat.rows();
            byte[] buffer = new byte[bufferSize];
            mat.get(0, 0, buffer);
            orig = new BufferedImage(mat.cols(), mat.rows(), type);
            byte[] targetPixels = ((java.awt.image.DataBufferByte) orig.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        } else {
            throw new IllegalArgumentException("Unsupported image type: " + wrapped.getClass());
        }

        // Simple resize to 640x640
        Image resized = ImageFactory.getInstance().fromImage(wrapped).resize(640, 640,false);

        // Convert to NDArray, normalize, HWC → CHW, add batch
        NDArray array = resized.toNDArray(ctx.getNDManager(), Image.Flag.COLOR)
                .toType(ai.djl.ndarray.types.DataType.FLOAT32, false)
                .div(255f)
                .transpose(2, 0, 1)
                .expandDims(0);

        return new NDList(array);
    }

    // --------------------- processOutput ---------------------
    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {

        Image originalImg = (Image) ctx.getAttachment("originalImage"); // original image

        int origWidth = originalImg.getWidth();
        int origHeight = originalImg.getHeight();

        NDArray result = list.get(0).squeeze(0); // shape: [num_boxes, 4+num_classes]
        List<String> names = new ArrayList<>();
        List<Double> probs = new ArrayList<>();
        List<BoundingBox> boxes = new ArrayList<>();

        if (result.isEmpty()) return new DetectedObjects(names, probs, boxes);

        int numClasses = classes.size();
        int numBoxes = (int) result.getShape().get(0);

        for (int i = 0; i < numBoxes; i++) {
            // Get confidence for each class
            float conf = 0f;
            int clsId = 0;
            for (int c = 0; c < numClasses; c++) {
                float score = result.getFloat(i, 4 + c);
                if (score > conf) { conf = score; clsId = c; }
            }

            if (conf < confThreshold) continue;

            // Bounding box coordinates
            float cx = result.getFloat(i, 0);
            float cy = result.getFloat(i, 1);
            float w  = result.getFloat(i, 2);
            float h  = result.getFloat(i, 3);


            // Convert center -> top-left & scale to original image
            float scaleX = (float) origWidth / 640;
            float scaleY = (float) origHeight / 640;

            float x1 = (cx - w/2f) * scaleX;
            float y1 = (cy - h/2f) * scaleY;
            float wScaled = w * scaleX;
            float hScaled = h * scaleY;
            Rectangle rect = new Rectangle(Math.round(x1), Math.round(y1),
                    Math.round(wScaled), Math.round(hScaled));



            boxes.add(rect);
            names.add(classes.get(clsId));
            probs.add((double) conf);

        }

        // Optional: simple NMS
        return nms(names, probs, boxes, nmsThreshold);
    }

    @Override
    public Batchifier getBatchifier() {
        return null;
    }

    // --------------------- Simple NMS ---------------------
    private DetectedObjects nms(List<String> names, List<Double> probs, List<BoundingBox> boxes, float iouThresh) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < probs.size(); i++) order.add(i);

        // Sort by probability descending
        order.sort((a, b) -> Double.compare(probs.get(b), probs.get(a)));

        boolean[] suppressed = new boolean[boxes.size()];
        List<String> finalNames = new ArrayList<>();
        List<Double> finalProbs = new ArrayList<>();
        List<BoundingBox> finalBoxes = new ArrayList<>();

        for (int i = 0; i < order.size(); i++) {
            int idx = order.get(i);
            if (suppressed[idx]) continue;

            finalNames.add(names.get(idx));
            finalProbs.add(probs.get(idx));
            finalBoxes.add(boxes.get(idx));

            Rectangle boxA = (Rectangle) boxes.get(idx);
            for (int j = i + 1; j < order.size(); j++) {
                int idxj = order.get(j);
                if (suppressed[idxj]) continue;
                Rectangle boxB = (Rectangle) boxes.get(idxj);
                if (iou(boxA, boxB) > iouThresh) suppressed[idxj] = true;
            }
        }
        return new DetectedObjects(finalNames, finalProbs, finalBoxes);
    }

    private float iou(Rectangle a, Rectangle b) {
        float xx1 = Math.max((float) a.getX(), (float) b.getX());
        float yy1 = Math.max((float) a.getY(), (float) b.getY());
        float xx2 = Math.min((float) (a.getX() + a.getWidth()), (float) (b.getX() + b.getWidth()));
        float yy2 = Math.min((float) (a.getY() + a.getHeight()), (float) (b.getY() + b.getHeight()));

        float w = Math.max(0f, xx2 - xx1);
        float h = Math.max(0f, yy2 - yy1);
        float inter = w * h;

        float areaA = (float) a.getWidth() * (float) a.getHeight();
        float areaB = (float) b.getWidth() * (float) b.getHeight();

        return inter / (areaA + areaB - inter + 1e-6f);
    }
}