package com.innova.visual_retail_discovery.service.embeddings.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.innova.visual_retail_discovery.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

public class StandAloneYoloObjectClassification {

    private static final Logger log = LoggerFactory.getLogger(StandAloneYoloObjectClassification.class);

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String MODEL_PATH = Utils.TRAINED_MODEL_PATH;
    private static final String IMAGE_PATH = Utils.SAMPLE_IMAGE;
    private static final String OUTPUT_FILE = Utils.OUTPUT_FILE_PATH;
    private static final int IMG_SIZE = 640;
    private static final float CONF_THRESH = 0.1f;
    private static final float IOU_THRESH = 0.45f;

    // Must match the order in your dataset.yaml "names"
    private static final String[] CLASS_NAMES = Utils.SUPPORTED_CLASSES;


    public static List<File> createCroppedImageAndReturnFiles(String imagePath) throws OrtException {
        log.info("[Step 4] In StandAlone Cropped Image version......");
        log.info("Loading model: {}", MODEL_PATH);
        List<File> files = new ArrayList<>();
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession session = env.createSession(MODEL_PATH, cpuOptions())) {

//            System.out.println("Model loaded");
//            System.out.println("Input  : " + session.getInputNames());
//            System.out.println("Output : " + session.getOutputNames());

            // ── Load & pre-process image (pure Java, no OpenCV) ──────────────
            BufferedImage img = ImageIO.read(new File(imagePath));
            if (img == null) throw new RuntimeException("Cannot read image: " + IMAGE_PATH);

            int origW = img.getWidth(), origH = img.getHeight();
            //System.out.printf("Image size: %dx%d%n", origW, origH);

            float[] inputData = preProcess(img, IMG_SIZE);

            // ── Build input tensor ───────────────────────────────────────────
            long[] shape = {1, 3, IMG_SIZE, IMG_SIZE};  // NCHW
            OnnxTensor tensor = OnnxTensor.createTensor(
                    env, FloatBuffer.wrap(inputData), shape);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(session.getInputNames().iterator().next(), tensor);

            // ── Run inference ────────────────────────────────────────────────
            long t0 = System.currentTimeMillis();
            OrtSession.Result result = session.run(inputs);
            //System.out.printf("Inference time: %d ms%n", System.currentTimeMillis() - t0);

            // ── Post-process ─────────────────────────────────────────────────
            float[][][] raw = (float[][][]) result.get(0).getValue();
            List<Detection> dets = postProcess(raw, origW, origH, CLASS_NAMES.length);

            // ── Print results ─────────────────────────────────────────────────
            if (dets.isEmpty()) {
                log.info("No detections found.");
            } else {
                log.info("{} detections found.", dets.size());

                for (Detection d : dets) {
                    log.info("Class={} Conf={} BBox=({},{},{},{})",
                            CLASS_NAMES[d.classId], String.format("%.2f", d.confidence),
                            d.x1, d.y1, d.x2, d.y2);
                    try {
                        BufferedImage cropped = img.getSubimage(d.x1, d.y1, d.x2 - d.x1, d.y2 - d.y1);
                        int num = new Random().nextInt(100);
                        String fileName = "cropped-"+num+"_" + CLASS_NAMES[d.classId] + ".jpg".toUpperCase();
                        File file = new File("target\\classes\\static\\cropped",fileName);
                        ImageIO.write(cropped, "jpg",file );
                        log.info("[Step 4.1] Cropped image saved for {}", CLASS_NAMES[d.classId]);
                        files.add(file);

                    } catch (Exception ex) {
                        log.error("Error saving cropped image", ex);
                    }

                }

            }

            // ── (Optional) Save annotated image ──────────────────────────────
            saveAnnotated(img, dets, OUTPUT_FILE);

            log.info("[Step 4.2] Annotated image saved -> output.jpg");

            tensor.close();
            return files;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    // ── Pre-process: resize + normalize to float[3*H*W] CHW ──────────────────
    private static float[] preProcess(BufferedImage img, int size) {
        // Resize to size x size using Java2D
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();

        // Convert pixels → CHW float array, normalized 0–1
        float[] data = new float[3 * size * size];
        int planeSize = size * size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int rgb = resized.getRGB(x, y);
                int idx = y * size + x;
                data[idx] = ((rgb >> 16) & 0xFF) / 255.0f; // R
                data[planeSize + idx] = ((rgb >> 8) & 0xFF) / 255.0f; // G
                data[2 * planeSize + idx] = (rgb & 0xFF) / 255.0f; // B
            }
        }
        return data;
    }

    // ── Post-process: threshold + NMS ─────────────────────────────────────────
    private static List<Detection> postProcess(float[][][] raw,
                                               int origW, int origH,
                                               int numClasses) {
        float[][] pred = raw[0]; // shape: [4+numClasses][numAnchors]
        int numAnchors = pred[0].length;
        float scaleX = (float) origW / IMG_SIZE;
        float scaleY = (float) origH / IMG_SIZE;

        List<Detection> candidates = new ArrayList<>();
        for (int i = 0; i < numAnchors; i++) {
            float maxScore = 0;
            int bestClass = 0;
            for (int c = 0; c < numClasses; c++) {
                float s = pred[4 + c][i];
                if (s > maxScore) {
                    maxScore = s;
                    bestClass = c;
                }
            }
            if (maxScore < CONF_THRESH) continue;

            float cx = pred[0][i], cy = pred[1][i];
            float bw = pred[2][i], bh = pred[3][i];
            int x1 = Math.round((cx - bw / 2) * scaleX);
            int y1 = Math.round((cy - bh / 2) * scaleY);
            int x2 = Math.round((cx + bw / 2) * scaleX);
            int y2 = Math.round((cy + bh / 2) * scaleY);

//            int x1 = Math.round((cx - bw / 2) * 1);
//            int y1 = Math.round((cy - bh / 2) * 1);
//            int x2 = Math.round((cx + bw / 2) * 1);
//            int y2 = Math.round((cy + bh / 2) * 1);


            candidates.add(new Detection(bestClass, maxScore, x1, y1, x2, y2));
        }
        return nms(candidates, IOU_THRESH);
    }

    // ── NMS ───────────────────────────────────────────────────────────────────
    private static List<Detection> nms(List<Detection> dets, float iouThresh) {
        dets.sort((a, b) -> Float.compare(b.confidence, a.confidence));
        List<Detection> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[dets.size()];
        for (int i = 0; i < dets.size(); i++) {
            if (suppressed[i]) continue;
            kept.add(dets.get(i));
            for (int j = i + 1; j < dets.size(); j++) {
                if (!suppressed[j]
                        && dets.get(i).classId == dets.get(j).classId
                        && iou(dets.get(i), dets.get(j)) > iouThresh)
                    suppressed[j] = true;
            }
        }
        return kept;
    }

    private static float iou(Detection a, Detection b) {
        int ix1 = Math.max(a.x1, b.x1), iy1 = Math.max(a.y1, b.y1);
        int ix2 = Math.min(a.x2, b.x2), iy2 = Math.min(a.y2, b.y2);
        float inter = Math.max(0, ix2 - ix1) * (float) Math.max(0, iy2 - iy1);
        float areaA = (a.x2 - a.x1) * (float) (a.y2 - a.y1);
        float areaB = (b.x2 - b.x1) * (float) (b.y2 - b.y1);
        return inter / (areaA + areaB - inter + 1e-6f);
    }

    // ── Draw boxes and save image (pure Java2D) ────────────────────────────────
    private static void saveAnnotated(BufferedImage img,
                                      List<Detection> dets,
                                      String outPath) throws Exception {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.setStroke(new BasicStroke(2));
        g.setFont(new Font("Arial", Font.BOLD, 14));

        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA};
        for (Detection d : dets) {
            Color c = colors[d.classId % colors.length];
            g.setColor(c);
            g.drawRect(d.x1, d.y1, d.x2 - d.x1, d.y2 - d.y1);
            String label = CLASS_NAMES[d.classId] + " " + String.format("%.2f", d.confidence);
            g.fillRect(d.x1, d.y1 - 18, g.getFontMetrics().stringWidth(label) + 4, 18);
            g.setColor(Color.WHITE);
            g.drawString(label, d.x1 + 2, d.y1 - 4);
        }
        g.dispose();

        String folderName = "output"; // your folder name
        String path = System.getProperty("user.dir") + File.separator + folderName;

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ImageIO.write(out, "jpg", dir);
    }

    // ── CPU session options ────────────────────────────────────────────────────
    private static OrtSession.SessionOptions cpuOptions() throws OrtException {
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.addCPU(true);
        opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        return opts;
    }

    // ── Detection record ──────────────────────────────────────────────────────
    static class Detection {
        int classId, x1, y1, x2, y2;
        float confidence;

        Detection(int classId, float conf, int x1, int y1, int x2, int y2) {
            this.classId = classId;
            this.confidence = conf;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

}
