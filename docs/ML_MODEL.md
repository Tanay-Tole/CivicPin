# Issue classifier (our own model)

`app/src/main/assets/civicpin_model.tflite` is a small CNN designed and trained for this project from random
weights. No pretrained network and no Teachable Machine export is used.

- Input: 128 x 128 RGB, values 0..1. Output: 4 softmax scores in `labels.txt` order.
- Test accuracy: 94.8% on 755 held-out photos (see `ml/test_report.txt`).
- Full write-up with every data source and licence: `docs/CivicPin_ML_Model_Report.pdf`.

## Rebuild
```bash
python ml/prep.py <training_folder>     # resizes the photos in <training_folder>/downloads once
python ml/train.py <training_folder>    # trains, tests, exports out/civicpin_model.tflite
python ml/make_report_pdf.py <training_folder>/out docs/CivicPin_ML_Model_Report.pdf
```
Needs Python 3.12 with `tensorflow pillow numpy reportlab`.

## Training photos (CC BY 4.0, credit required)
- Maske, Jakate, Thakare, Lokhande (2025). Urban Civic Issues Image Dataset: Potholes and Garbage (QR4Change).
  Mendeley Data, V2. doi:10.17632/zndzygc3p3.2. Used for Garbage, Potholes, Others.
- Wang, Y. (2024). Urban Flood Image Dataset. HydroShare.
  hydroshare.org/resource/24866122a6ee456c8f7c80aa87a9abcb. Used for Water_Leak_Sewer.

Note: some photos in the first dataset carry stock-photo watermarks. They are used for training only and are not
shipped in the app. Replace them with your own photos if that matters for your submission.
