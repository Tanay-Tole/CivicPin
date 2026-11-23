"""Train CivicPin's issue classifier from scratch (random init, no pretrained weights) and export TFLite.

Output matches what the app's CivicIssueClassifier expects:
  input  float32 [1, H, W, 3], RGB scaled to [0, 1]
  output float32 [1, 4] softmax, order = labels.txt (Garbage, Water_Leak_Sewer, Potholes, Others)
"""
import os, sys, time

import numpy as np
import tensorflow as tf
import keras
from keras import layers

ROOT = sys.argv[1]
SIZE = 128
EPOCHS = int(os.environ.get("EPOCHS", 35))
NAMES = ["Garbage", "Water_Leak_Sewer", "Potholes", "Others"]
OUT = os.path.join(ROOT, "out")
os.makedirs(OUT, exist_ok=True)
keras.utils.set_random_seed(42)

data = np.load(os.path.join(ROOT, f"cache_{SIZE}.npz"))
x, y = data["x"], data["y"]

# stratified 70 / 15 / 15 split, fixed seed so the test set never leaks into training
rng = np.random.default_rng(42)
tr, va, te = [], [], []
for c in range(4):
    idx = rng.permutation(np.where(y == c)[0])
    n = len(idx)
    te += list(idx[: int(n * 0.15)])
    va += list(idx[int(n * 0.15): int(n * 0.30)])
    tr += list(idx[int(n * 0.30):])
tr, va, te = map(np.array, (tr, va, te))
print("train/val/test", len(tr), len(va), len(te), flush=True)

augment = keras.Sequential([
    layers.RandomFlip("horizontal"),
    layers.RandomRotation(0.05),
    layers.RandomZoom((-0.2, 0.1)),
    layers.RandomTranslation(0.08, 0.08),
    layers.RandomContrast(0.25),
    layers.RandomBrightness(0.2, value_range=(0, 1)),
])


def ds(idx, train):
    d = tf.data.Dataset.from_tensor_slices((x[idx], keras.utils.to_categorical(y[idx], 4)))
    if train:
        d = d.shuffle(len(idx), seed=42)
    d = d.batch(64).map(lambda a, b: (tf.cast(a, tf.float32) / 255.0, b), num_parallel_calls=tf.data.AUTOTUNE)
    if train:
        d = d.map(lambda a, b: (tf.clip_by_value(augment(a, training=True), 0, 1), b), num_parallel_calls=tf.data.AUTOTUNE)
    return d.prefetch(tf.data.AUTOTUNE)


def block(t, f):
    for _ in range(2):
        t = layers.Conv2D(f, 3, padding="same", use_bias=False)(t)
        t = layers.BatchNormalization()(t)
        t = layers.ReLU()(t)
    return layers.MaxPooling2D()(t)


inp = keras.Input((SIZE, SIZE, 3))
t = inp
for f in (32, 64, 128, 192):
    t = block(t, f)
t = layers.Conv2D(256, 1, use_bias=False)(t)
t = layers.BatchNormalization()(t)
t = layers.ReLU()(t)
t = layers.GlobalAveragePooling2D()(t)
t = layers.Dropout(0.3)(t)
out = layers.Dense(4, activation="softmax")(t)
model = keras.Model(inp, out, name="civicpin_scratch")
print("params", model.count_params(), flush=True)

counts = np.bincount(y[tr], minlength=4)
class_weight = {c: len(tr) / (4 * counts[c]) for c in range(4)}
steps = EPOCHS * int(np.ceil(len(tr) / 64))
model.compile(
    optimizer=keras.optimizers.AdamW(keras.optimizers.schedules.CosineDecay(1e-3, steps, warmup_target=None), weight_decay=1e-4),
    loss=keras.losses.CategoricalCrossentropy(label_smoothing=0.05),
    metrics=["accuracy"],
)
t0 = time.time()
model.fit(
    ds(tr, True), validation_data=ds(va, False), epochs=EPOCHS, class_weight=class_weight, verbose=2,
    callbacks=[keras.callbacks.EarlyStopping("val_loss", patience=8, restore_best_weights=True)],
)
print(f"trained in {(time.time() - t0) / 60:.1f} min", flush=True)

# honest test on photos never seen in training or validation
probs = model.predict(ds(te, False), verbose=0)
pred = probs.argmax(1)
cm = np.zeros((4, 4), int)
for a, b in zip(y[te], pred):
    cm[a, b] += 1
lines = [f"test photos: {len(te)}  overall accuracy: {(pred == y[te]).mean():.1%}", "", "per category (right / total):"]
for c in range(4):
    lines.append(f"  {NAMES[c]:<17} {cm[c, c]:>4} / {cm[c].sum():<4} = {cm[c, c] / cm[c].sum():.1%}")
lines += ["", "confusion (rows = true, cols = predicted): " + ", ".join(NAMES)] + ["  " + " ".join(f"{v:>5}" for v in r) for r in cm]
conf = probs.max(1)
sure = conf >= 0.75
right_when_sure = (pred[sure] == y[te][sure]).mean() if sure.any() else 0.0
lines += ["", f"app auto-accepts (>=75% sure): {sure.mean():.1%} of photos, and those are right {right_when_sure:.1%} of the time"]
report = "\n".join(lines)
print(report)
open(os.path.join(OUT, "report.txt"), "w").write(report + "\n")

# export float32 TFLite (the app feeds float input, like the old model_unquant)
model.export(os.path.join(OUT, "saved"), verbose=False)
tfl = tf.lite.TFLiteConverter.from_saved_model(os.path.join(OUT, "saved")).convert()
path = os.path.join(OUT, "civicpin_model.tflite")
open(path, "wb").write(tfl)

# self-check: shapes the app expects, and TFLite agrees with Keras
it = tf.lite.Interpreter(model_path=path)
it.allocate_tensors()
i_d, o_d = it.get_input_details()[0], it.get_output_details()[0]
assert list(i_d["shape"]) == [1, SIZE, SIZE, 3] and i_d["dtype"] == np.float32, i_d
assert list(o_d["shape"]) == [1, 4] and o_d["dtype"] == np.float32, o_d
for k in te[:20]:
    a = (x[k:k + 1].astype(np.float32) / 255.0)
    it.set_tensor(i_d["index"], a)
    it.invoke()
    assert np.allclose(it.get_tensor(o_d["index"]), model(a, training=False).numpy(), atol=1e-4)
print(f"self-check OK, tflite {os.path.getsize(path) / 1e6:.1f} MB -> {path}")
