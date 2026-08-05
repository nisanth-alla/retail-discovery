package com.innova.visual_retail_discovery.utils;

public class Utils {
    public static final String API_BASE_URL = "http://localhost:8080";
    public static final String[] SUPPORTED_CLASSES = {"shirt",
            "top",
            "sweater",
            "cardigan",
            "jacket",
            "vest",
            "pants",
            "shorts",
            "skirt",
            "coat",
            "bead",
            "watch",
            "cape",
            "glasses",
            "hat",
            "scarf"};
    public static final String TRAINED_MODEL_PATH = "binary_brains_m2.onnx";
    public static String SAMPLE_IMAGE = "/dataset/img2.jpg";
    public static String SAMPLE_IMAGE_LABEL = "/dataset/img2.txt";
    public static String OUTPUT_FILE_PATH = "results/output.jpg";


}
