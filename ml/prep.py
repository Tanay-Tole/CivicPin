"""Resize every photo once into a small cached array so training is fast.

Plain stretch to SIZExSIZE, same as the app's Bitmap.createScaledBitmap, so training sees what the phone sends.
"""
import glob, os, sys
from multiprocessing import Pool

import numpy as np
from PIL import Image, ImageOps

SIZE = 128
ROOT = sys.argv[1]  # CivicPin-training folder
D = os.path.join(ROOT, "downloads")

# app label order (labels.txt): 0 Garbage, 1 Water_Leak_Sewer, 2 Potholes, 3 Others
SOURCES = {
    0: ["qr4change/garbage-yes/*"],
    1: ["urbanflood/Deepflood/image/*", "urbanflood/Sazara/image/*", "urbanflood/WebCOOS/image/*"],
    2: ["qr4change/pothole-yes/*"],
    3: ["qr4change/pothole-no/*", "qr4change/garbage-no/*"],
}


def load(path):
    try:
        im = Image.open(path)
        im.draft("RGB", (SIZE * 2, SIZE * 2))  # fast JPEG downscale on decode
        im = ImageOps.exif_transpose(im).convert("RGB").resize((SIZE, SIZE), Image.BILINEAR)
        return np.asarray(im, dtype=np.uint8)
    except Exception:
        return None  # unreadable/corrupt file: skip


if __name__ == "__main__":
    paths, labels = [], []
    for label, globs in SOURCES.items():
        files = sorted(f for g in globs for f in glob.glob(os.path.join(D, g)) if not f.endswith(".part"))
        paths += files
        labels += [label] * len(files)
        print(label, len(files), flush=True)
    with Pool() as p:
        imgs = p.map(load, paths, chunksize=16)
    keep = [i for i, im in enumerate(imgs) if im is not None]
    x = np.stack([imgs[i] for i in keep])
    y = np.array([labels[i] for i in keep], dtype=np.int64)
    np.savez(os.path.join(ROOT, f"cache_{SIZE}.npz"), x=x, y=y, paths=np.array([paths[i] for i in keep]))
    print("cached", x.shape, "skipped", len(paths) - len(keep), "per class", np.bincount(y))
