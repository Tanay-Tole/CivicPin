"""Build docs/CivicPin_ML_Model_Report.pdf from the training run's report.txt and run.json.

usage: python make_report_pdf.py <training_out_dir> <output.pdf>
"""
import json, os, re, sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import KeepTogether, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

OUT_DIR, PDF = sys.argv[1], sys.argv[2]
run = json.load(open(os.path.join(OUT_DIR, "run.json")))
rep = open(os.path.join(OUT_DIR, "report.txt")).read()

# parse the test report written by train.py
overall = re.search(r"test photos: (\d+)\s+overall accuracy: ([\d.]+%)", rep)
per_class = re.findall(r"^\s+(\w+)\s+(\d+) / (\d+)\s+= ([\d.]+%)", rep, re.M)
cm = [list(map(int, l.split())) for l in rep.split("confusion")[1].splitlines()[1:5]]
auto = re.search(r"sure\): ([\d.]+%) of photos, and those are right ([\d.]+%)", rep)
assert overall and len(per_class) == 4 and len(cm) == 4 and auto, "report.txt format changed"

INK, ACCENT, GRAY, LINE, SOFT = "#1D1D1F", "#0E8A5F", "#6E6E73", "#D9D9DE", "#F5F5F7"
base = dict(fontName="Helvetica", textColor=colors.HexColor(INK), alignment=TA_LEFT)
S = {
    "title": ParagraphStyle("title", fontName="Helvetica-Bold", fontSize=26, leading=30, textColor=colors.HexColor(INK)),
    "sub": ParagraphStyle("sub", **{**base, "fontSize": 11.5, "leading": 16, "textColor": colors.HexColor(GRAY)}),
    "h": ParagraphStyle("h", fontName="Helvetica-Bold", fontSize=14, leading=18, textColor=colors.HexColor(INK), spaceBefore=16, spaceAfter=6),
    "p": ParagraphStyle("p", **{**base, "fontSize": 10, "leading": 14.5}),
    "li": ParagraphStyle("li", **{**base, "fontSize": 10, "leading": 14.5, "leftIndent": 12, "bulletIndent": 2}),
    "cell": ParagraphStyle("cell", **{**base, "fontSize": 8.8, "leading": 12}),
    "cellb": ParagraphStyle("cellb", fontName="Helvetica-Bold", fontSize=8.8, leading=12, textColor=colors.HexColor(INK)),
    "big": ParagraphStyle("big", fontName="Helvetica-Bold", fontSize=20, leading=24, textColor=colors.HexColor(ACCENT)),
    "cap": ParagraphStyle("cap", **{**base, "fontSize": 8.5, "leading": 11, "textColor": colors.HexColor(GRAY)}),
}
P = lambda t, s="p": Paragraph(t, S[s])


def table(rows, widths, header=True):
    data = [[Paragraph(str(c), S["cellb" if (header and r == 0) else "cell"]) for c in row] for r, row in enumerate(rows)]
    t = Table(data, colWidths=[w * mm for w in widths], repeatRows=1 if header else 0)
    style = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LINEBELOW", (0, 0), (-1, -1), 0.4, colors.HexColor(LINE)),
        ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING", (0, 0), (-1, -1), 6), ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ]
    if header:
        style.append(("BACKGROUND", (0, 0), (-1, 0), colors.HexColor(SOFT)))
    t.setStyle(TableStyle(style))
    return t


def code(text):
    t = Table([[Paragraph(text, ParagraphStyle("code", fontName="Courier", fontSize=8.8, leading=13, textColor=colors.HexColor(INK)))]], colWidths=[170 * mm])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), colors.HexColor(SOFT)), ("LEFTPADDING", (0, 0), (-1, -1), 10),
                           ("TOPPADDING", (0, 0), (-1, -1), 8), ("BOTTOMPADDING", (0, 0), (-1, -1), 8)]))
    return t


def bullets(items):
    return [Paragraph(i, S["li"], bulletText="•") for i in items]


story = [
    P("CivicPin issue classifier", "title"),
    Spacer(1, 4),
    P(f"Model report: how it was built and every resource it uses. {run['date']}. Branch <b>own-ml-model</b>.", "sub"),
    Spacer(1, 12),
]

facts = [
    (overall.group(2), f"correct on {overall.group(1)} test photos it never saw in training"),
    ("0", "pretrained weights used. Every layer starts from random numbers"),
    (f"{run['tflite_mb']:.1f} MB", f"model file, {run['params']:,} trainable numbers, runs on the phone"),
]
ft = Table([[Paragraph(a, S["big"]) for a, _ in facts], [Paragraph(b, S["cap"]) for _, b in facts]], colWidths=[56.6 * mm] * 3)
ft.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor(SOFT)), ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 12), ("RIGHTPADDING", (0, 0), (-1, -1), 10),
    ("TOPPADDING", (0, 0), (-1, 0), 12), ("BOTTOMPADDING", (0, 1), (-1, 1), 12),
]))
story += [ft]

story += [P("1. What was replaced", "h")] + bullets([
    "<b>Removed:</b> <font face='Courier'>model_unquant.tflite</font>, made with Google Teachable Machine. That tool does not build a model. "
    "It takes Google's ready-made MobileNet network and only retrains the last layer, so most of that model was Google's work.",
    "<b>Added:</b> <font face='Courier'>civicpin_model.tflite</font>, a network we designed and trained ourselves, starting from random weights. "
    "No part of any existing model was copied, downloaded or fine-tuned.",
    "The app code around it is unchanged in behaviour: same four categories, same order, and the same rule that asks the person to confirm "
    "when the model is under 75% sure.",
])

story += [P("2. The model", "h"),
          P("A small convolutional neural network (CNN) written in Keras. Input is a 128 x 128 colour photo with pixel values scaled to 0..1. "
            "Output is four probabilities that add up to 1: Garbage, Water_Leak_Sewer, Potholes, Others."),
          Spacer(1, 6),
          table([
              ["Stage", "Layers", "Output size"],
              ["Input", "RGB photo, values 0..1", "128 x 128 x 3"],
              ["Block 1", "2 x (Conv 3x3, 32 filters, BatchNorm, ReLU), MaxPool", "64 x 64 x 32"],
              ["Block 2", "2 x (Conv 3x3, 64 filters, BatchNorm, ReLU), MaxPool", "32 x 32 x 64"],
              ["Block 3", "2 x (Conv 3x3, 128 filters, BatchNorm, ReLU), MaxPool", "16 x 16 x 128"],
              ["Block 4", "2 x (Conv 3x3, 192 filters, BatchNorm, ReLU), MaxPool", "8 x 8 x 192"],
              ["Head", "Conv 1x1 (256), BatchNorm, ReLU, Global average pool, Dropout 0.3", "256"],
              ["Output", "Dense 4, softmax", "4"],
          ], [24, 108, 38])]

story += [KeepTogether([P("3. How it was trained", "h"),
          table([
              ["Setting", "Value"],
              ["Starting weights", "Random (Keras default initialisers), seed 42. No transfer learning."],
              ["Photos", f"{run['n_total']:,} in total, split per category into 70% train, 15% validation, 15% test. The test photos were never used for training or tuning."],
              ["Augmentation", "Random flip, small rotation, zoom, shift, contrast and brightness, so the model copes with different phone angles and light."],
              ["Optimiser", "AdamW, learning rate 0.001 with cosine decay, weight decay 0.0001, batch size 64."],
              ["Loss", "Cross-entropy with label smoothing 0.05. Category weights balance the uneven photo counts."],
              ["Length", f"Up to {run['epochs_max']} passes over the data, with early stopping if validation stops improving. {run['train_minutes']:.0f} minutes."],
              ["Machine", "Apple M5 MacBook Pro, 16 GB, CPU only. No cloud service was used."],
              ["Export", "TensorFlow Lite, float32. A self-check confirms the phone format gives the same answers as the trained model."],
          ], [34, 136])])]

story += [P("4. Training photos and their sources", "h"),
          P("We did not take these photos. They come from two public research datasets, both released under the Creative Commons Attribution 4.0 "
            "licence (CC BY 4.0), which allows reuse with credit. This table is that credit."),
          Spacer(1, 6),
          table([
              ["Category", "Photos", "Dataset and authors", "Licence and link"],
              ["Garbage", str(run["counts"][0]),
               "Urban Civic Issues Image Dataset: Potholes and Garbage (QR4Change), folder garbage/yes. Maske, Jakate, Thakare, Lokhande. Mendeley Data, V2, 2025. Phone photos from Pune, India.",
               "CC BY 4.0<br/>doi.org/10.17632/zndzygc3p3.2"],
              ["Potholes", str(run["counts"][2]), "Same dataset, folder pothole/yes.", "CC BY 4.0<br/>same link"],
              ["Others", str(run["counts"][3]), "Same dataset, folders pothole/no (plain roads) and garbage/no (clean streets), 1,000 each.", "CC BY 4.0<br/>same link"],
              ["Water_Leak_Sewer", str(run["counts"][1]),
               "Urban Flood Image Dataset. Yidi Wang. HydroShare, 2024. Three folders of flooded-street photos: Deepflood (1,040), Sazara (253), WebCOOS (35). Only the photos were used, not the masks.",
               "CC BY 4.0<br/>hydroshare.org/resource/ 24866122a6ee456c8f7c80aa87a9abcb"],
          ], [30, 15, 80, 45]),
          Spacer(1, 4),
          P(f"{run['skipped']} photos in the garbage folder could not be opened and were skipped. Mendeley's listing returns at most 1,000 files per folder, "
            "so 1,000 of the 1,004 pothole, 1,962 plain-road and 1,259 clean-street photos were used.", "cap"),
          Spacer(1, 6),
          P("<b>One thing to know about the first dataset.</b> Its authors publish it as CC BY 4.0 and say part of it was gathered from the web. "
            "While testing we saw that some of its photos carry stock-photo watermarks (for example Shutterstock), so the authors may not have held the rights "
            "to every photo. The photos are used here only to train the model. None of them is stored in the app or shown to users. "
            "If this matters for a submission, replace those photos with your own and retrain.")]

names = [n for n, *_ in per_class]
res_rows = [["Category", "Correct", "Out of", "Accuracy"]] + [[n, a, b, c] for n, a, b, c in per_class] + [["All categories", "", overall.group(1), overall.group(2)]]
cm_rows = [["True category (rows) vs model answer (columns)"] + names] + [[names[i]] + cm[i] for i in range(4)]
story += [KeepTogether([
    P("5. Test results", "h"),
    P(f"Measured on the {overall.group(1)} held-out test photos."),
    Spacer(1, 6),
    table(res_rows, [60, 30, 30, 50]),
    Spacer(1, 10),
    table(cm_rows, [54, 26, 38, 26, 26]),
    Spacer(1, 6),
    P(f"In the app, {auto.group(1)} of these photos would pass the 75% confidence rule automatically, and the model was right on {auto.group(2)} of those. "
      "The rest go to the person to confirm.", "p"),
    Spacer(1, 6),
    P("<b>Checked inside the app too.</b> On an Android 15 emulator, test photos picked through the app's Gallery button gave: garbage dump: Garbage 90%, "
      "pothole: Potholes 94%, second pothole: Potholes 89%, flooded street: Water 98%. A first run showed lower confidence because Android shrank photos "
      "more roughly than the training script. The app now shrinks photos in smooth halving steps, which fixed it.", "p"),
])]

story += [P("6. Honest limits", "h")] + bullets([
    "<b>The test photos come from the same datasets as the training photos.</b> Real photos from Bengaluru phones will look different, so expect lower "
    "accuracy in real use until local photos are added.",
    "<b>Water is the weakest category in practice.</b> It was trained only on flooded streets. It has never seen a burst pipe or sewage overflow. "
    "Because all its photos come from a different dataset than the other three, the model may partly be recognising that dataset's look.",
    "<b>Others</b> only covers clean roads and streets. A photo of something unrelated (a person, a room) can still be given one of the three issue labels.",
    "A model trained from scratch on about 5,000 photos is less accurate than one built on a large pretrained network. That is the price of owning all of it.",
    "Fix for all four: collect 50 to 100 photos per category around Bengaluru and retrain with the commands in section 9.",
])

story += [KeepTogether([P("7. Software used", "h"),
          table([
              ["Tool", "Version", "Used for", "Licence"],
              ["Python", "3.12", "Training scripts", "PSF"],
              ["TensorFlow + Keras", f"{run['tf']} / {run['keras']}", "Building, training and exporting the model", "Apache 2.0"],
              ["NumPy, Pillow", "current", "Loading and resizing photos", "BSD / HPND"],
              ["TensorFlow Lite runtime", "2.16.1", "Runs the model inside the Android app", "Apache 2.0"],
              ["ReportLab", "5.1", "This PDF", "BSD"],
          ], [44, 30, 62, 34]),
          Spacer(1, 4),
          P("These are tools, the same way a calculator is a tool. None of them contains a trained model that we reused.", "cap")])]

story += [P("8. Other changes made in the same round", "h")] + bullets([
    "<b>Map:</b> Google Maps was replaced with OpenStreetMap through the osmdroid library 6.1.20 (Apache 2.0). No API key is needed. "
    "Map data and tiles: copyright OpenStreetMap contributors, Open Database Licence, credited on every map in the app. "
    "The Google Maps key that was visible in the public code has been removed from the project.",
    "<b>Design:</b> all seven screens were redesigned with one colour, type and spacing system, light and dark mode, and a new report flow. "
    "The 22 icons, the logo and the map pins were drawn for this app. The font is Android's system font. No UI kit or template was used.",
])

story += [KeepTogether([P("9. How to rebuild the model", "h"),
          P("From the <font face='Courier'>ml</font> folder of the project, with the two datasets downloaded into a <font face='Courier'>downloads</font> folder:"),
          Spacer(1, 4),
          code("python prep.py &lt;training_folder&gt;<br/>python train.py &lt;training_folder&gt;<br/>"
               "cp &lt;training_folder&gt;/out/civicpin_model.tflite app/src/main/assets/"),
          Spacer(1, 4),
          P("To add your own photos, put them in a new folder and add that folder to the SOURCES list at the top of prep.py.", "cap")])]

story += [KeepTogether([P("10. Statement of originality", "h"),
          P("The network design, the training code and the trained weights in <font face='Courier'>civicpin_model.tflite</font> were produced for this project. "
            "The model was trained from random initial weights. No pretrained model, no Teachable Machine export and no third-party model file is part of it. "
            "The training photos are third-party work, used under CC BY 4.0 and credited in section 4. "
            "The training scripts were written with the help of an AI coding assistant (Claude) working under the project owner's direction.")])]


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor(GRAY))
    canvas.drawString(20 * mm, 12 * mm, "CivicPin issue classifier: model report")
    canvas.drawRightString(190 * mm, 12 * mm, f"Page {doc.page}")
    canvas.restoreState()


SimpleDocTemplate(PDF, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm, topMargin=20 * mm, bottomMargin=20 * mm,
                  title="CivicPin issue classifier: model report", author="CivicPin").build(story, onFirstPage=footer, onLaterPages=footer)
print("wrote", PDF)
