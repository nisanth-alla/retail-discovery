package com.innova.visual_retail_discovery.service.translator;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YoloV11TranslatorV1 implements Translator<Image, DetectedObjects> {

    private static final Logger log = LoggerFactory.getLogger(YoloV11TranslatorV1.class);

    private List<String> classNames;
    private float confThreshold = 0.25f;
    private float nmsThreshold  = 0.45f;

    public YoloV11TranslatorV1(List<String> classNames) {
        this.classNames = classNames;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        int size = 640; // model input size

        // 1) Resize image
        Image resized = input.resize(size, size,true);
       // System.out.println("Before transpose: " + resized.toNDArray(ctx.getNDManager()).getShape());
        // 2) Convert to NDArray
        NDArray array = resized.toNDArray(ctx.getNDManager())   // [H, W, C]
                .transpose(2, 0, 1)   // → [C, H, W]
                .div(255.0f)          // normalize
                .expandDims(0);       // → [1, 3, H, W]
      //  System.out.println("Afetr transpose: " + array.getShape());
        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        NDArray preds = list.get(0); // model output
        preds = preds.squeeze();     // remove batch dim → [num_boxes, attrs]

        float[] data = preds.toFloatArray();
        long[] shape = preds.getShape().getShape(); // [num_boxes, num_attrs]
        int numBoxes = (int) shape[0];
        int numAttrs = (int) shape[1];

        List<BoundingBox> boxes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Double> probs = new ArrayList<>();

        for (int i = 0; i < numBoxes; i++) {
            int offset = i * numAttrs;
            float conf = data[offset + 4];
            if (conf < confThreshold) continue;

            // find class
            int classId = 0;
            float maxProb = 0f;
            for (int c = 5; c < numAttrs; c++) {
                if (data[offset + c] > maxProb) {
                    maxProb = data[offset + c];
                    classId = c - 5;
                }
            }

            log.debug("classId in processOutput: {}", classId);

            float score = conf * maxProb;
            if (score < confThreshold) continue;

            float cx = data[offset + 0];
            float cy = data[offset + 1];
            float w  = data[offset + 2];
            float h  = data[offset + 3];

            // Convert centerx,centery,width,height → rectangle
            float x = cx - w / 2f;
            float y = cy - h / 2f;

            boxes.add(new Rectangle(x, y, w, h));
            names.add(classNames.get(0));//TODO
            probs.add((double) score);
        }

        //  NMS
        List<Integer> keep = nonMaxSuppression(boxes, probs, nmsThreshold);

        List<BoundingBox> finalBoxes = new ArrayList<>();
        List<String> finalNames = new ArrayList<>();
        List<Double> finalProbs = new ArrayList<>();

        for (int i : keep) {
            finalBoxes.add(boxes.get(i));
            finalNames.add(names.get(i));
            finalProbs.add(probs.get(i));
        }

        return new DetectedObjects(finalNames, finalProbs, finalBoxes);
    }

    @Override
    public Batchifier getBatchifier() {
        return null;
    }

    // Standard NMS
    private List<Integer> nonMaxSuppression(
            List<BoundingBox> boxes,
            List<Double> scores,
            float iouThreshold) {

        List<Integer> idxs = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) idxs.add(i);

        Collections.sort(idxs, (a, b) -> Double.compare(scores.get(b), scores.get(a)));

        List<Integer> keep = new ArrayList<>();
        while (!idxs.isEmpty()) {
            int i = idxs.remove(0);
            keep.add(i);

            idxs.removeIf(j -> computeIoU(boxes.get(i), boxes.get(j)) > iouThreshold);
        }
        return keep;
    }

    private float computeIoU(BoundingBox a, BoundingBox b) {
        Rectangle ra = (Rectangle) a;
        Rectangle rb = (Rectangle) b;

        float x1 = (float) Math.max(ra.getX(), rb.getX());
        float y1 = (float) Math.max(ra.getY(), rb.getY());

        float x2 = (float) Math.min(ra.getX() + ra.getWidth(), rb.getX() + rb.getWidth());
        float y2 = (float) Math.min(ra.getY() + ra.getHeight(), rb.getY() + rb.getHeight());

        float interW = Math.max(0, x2 - x1);
        float interH = Math.max(0, y2 - y1);
        float interArea = interW * interH;

        float areaA = (float) (ra.getWidth() * ra.getHeight());
        float areaB = (float) (rb.getWidth() * rb.getHeight());
        return interArea / (areaA + areaB - interArea);
    }
}