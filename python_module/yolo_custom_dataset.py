# ─── Step 1: Create dataset.yaml ─────────────────────────────────────────────
import yaml, os



# val_img_path = "dataset/images/val"
# val_lbl_path = "dataset/labels/val"

# imgs = set([f.split('.')[0] for f in os.listdir(val_img_path)])
# lbls = set([f.split('.')[0] for f in os.listdir(val_lbl_path)])

# print("Missing labels:", imgs - lbls)
# print("Extra labels:", lbls - imgs)


dataset_config = {
    "path": os.path.abspath("dataset"),   # root dir
    "train": "images/train",
    "val":   "images/val",
    "nc":    16,                            # number of classes
    "names": ["shirt","top","sweater","cardigan","jacket","vest","pants","shorts","skirt","coat","bead","watch","cape","glasses","hat","scarf"]           # your class names - , "jumpsuit", "cape", "glasses", "hat", "headband, head covering, hair accessory", "tie", "glove", "watch", "belt", "leg warmer", "tights, stockings", "sock", "shoe", "bag, wallet", "scarf", "umbrella", "hood", "collar", "lapel", "epaulette", "sleeve", "pocket", "neckline", "buckle", "zipper", "applique", "bead", "bow", "flower", "fringe", "ribbon", "rivet", "ruffle", "sequin", "tassel"
}

with open("dataset.yaml", "w") as f:
    yaml.dump(dataset_config, f, default_flow_style=False)

print("dataset.yaml created")


# ─── Step 2: Train YOLO on your dataset ──────────────────────────────────────
from ultralytics import YOLO

model = YOLO("yolo11n.pt")   # auto-downloads on first run

results = model.train(
    data      = "dataset.yaml",
    epochs    = 20,
    imgsz     = 640,
    batch     = 16,
    name      = "my_detector",
    project   = "runs/train",
    device    = "cpu",
    workers   = 4,
    patience  = 20,
    optimizer = "AdamW",
    lr0       = 0.001,
    augment   = True,
)

print(f"Training complete. Best model saved to: {results.save_dir}/weights/best.pt")


# ─── Step 3: Evaluate the model ──────────────────────────────────────────────
best_model_path = f"{results.save_dir}/weights/best.pt"
print(f"Loading best model from: {best_model_path}")
best_model = YOLO(best_model_path)
print(f"Best model loaded successfully from: {best_model_path}")

metrics = best_model.val(data="dataset.yaml")
print(f"mAP50    : {metrics.box.map50:.4f}")
print(f"mAP50-95 : {metrics.box.map:.4f}")
print(f"Precision: {metrics.box.mp:.4f}")
print(f"Recall   : {metrics.box.mr:.4f}")


# ─── Step 4: Run inference ────────────────────────────────────────────────────
import cv2

def detect_image(image_path: str, conf_threshold: float = 0.001):
    print(f"Running inference on: {image_path} with conf={conf_threshold}")
    preds = best_model.predict(
        source   = image_path,
        conf     = conf_threshold,
        save     = True,
        save_txt = True,
        project  = "runs/predict",
        name     = "output",
        device   = "cpu",
    )

    for r in preds:
        print(f"  Total boxes detected (before filtering): {len(r.boxes)}")
        if len(r.boxes) == 0:
            print("  No detections.")
        for box in r.boxes:
            cls_id     = int(box.cls[0])
            confidence = float(box.conf[0])
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            label = best_model.names[cls_id]
            print(f"  [{label}] conf={confidence:.2f}  bbox=({x1},{y1},{x2},{y2})")

    return preds


# ─── Step 5: Predict all images in the 'test' folder ─────────────────────────
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp", ".tiff"}
test_folder = "test"

if not os.path.isdir(test_folder):
    print(f"Test folder '{test_folder}' not found.")
else:
    test_images = [
        os.path.join(test_folder, f)
        for f in os.listdir(test_folder)
        if os.path.splitext(f)[1].lower() in IMAGE_EXTENSIONS
    ]

    if not test_images:
        print(f"No images found in '{test_folder}' folder.")
    else:
        print(f"\nFound {len(test_images)} image(s) in '{test_folder}'. Running detection...\n")
        for img_path in sorted(test_images):
            print(f"Image: {img_path}")
            detect_image(img_path)
            print()


# ─── Step 6: Export model to ONNX (used by Java) ─────────────────────────────
best_model.export(
    format   = "onnx",
    imgsz    = 640,
    opset    = 12,
    dynamic  = False,
    simplify = True,
     name="binary_brains_m2"
)
print("ONNX model exported  →  runs/train/my_detector/weights/binary_brains_m2.onnx")


# ─── Step 7: (Optional) Real-time webcam inference ───────────────────────────
def run_webcam():
    cap = cv2.VideoCapture(0)
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        for r in best_model(frame, stream=True):
            cv2.imshow("YOLO Detection", r.plot())
        if cv2.waitKey(1) & 0xFF == ord("q"):
            break
    cap.release()
    cv2.destroyAllWindows()

#run_webcam()  # uncomment to run
