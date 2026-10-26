# CIVIC PIN — Complete Test Manifest

This manifest documents all test cases for verifying the Civic Pin application.

| Test ID | Category | Preconditions | Test Steps | Expected Result | Status | Notes |
|---|---|---|---|---|---|---|
| AI-001 | AI | `model_unquant.tflite` present | Initialize `CivicIssueClassifier` | Model loads without exception | PASS | Verified in Unit/Runtime |
| AI-002 | AI | `labels.txt` present | Load labels in `CivicIssueClassifier` | Preserves 4 exact labels in order | PASS | Verified |
| AI-003 | AI | Model loaded | Inspect input tensor | Shape (1, 224, 224, 3) | PASS | Verified |
| AI-004 | AI | Model loaded | Inspect output tensor | Shape (1, 4) | PASS | Verified |
| AI-005 | AI | Model classifier test | Run `CivicIssueClassifierModelTest` | Label & probability data structure verified | PASS | Verified |
| AI-006 | AI | Sample image | Classify garbage image | Prediction probability returned | PASS | Verified |
| AI-007 | AI | Sample image | Classify water/sewage image | Prediction probability returned | PASS | Verified |
| AI-008 | AI | Sample image | Classify pothole image | Prediction probability returned | PASS | Verified |
| AI-009 | AI | User interaction | Tap 'Change Category' | Opens manual category selector | PASS | Verified in UI |
| AI-010 | AI | Low confidence | Run prediction < 75% | Displays warning & category selector | PASS | Verified in UI |
| CAM-001 | Camera | Camera permission granted | Launch camera capture via FileProvider | Captures image file safely on device & emulator | PASS | Verified |
| CAM-002 | Camera | Gallery photo | Select photo via Photo Picker | Decodes URI safely with downsampling & applies EXIF rotation | PASS | Verified |
| LOC-001 | Location | GPS enabled | Request location via Fused Location | Retrieves Lat/Lng & accuracy | PASS | Verified |
| LOC-002 | Location | Map view open | Drag marker or tap anywhere on map | Updates coordinates to self-pinned location | PASS | Verified |
| LOC-003 | Location | Map tap | Tap map in self-pin mode | Drops new pin & updates locality/address | PASS | Verified |
| ROUTE-001| Routing | Koramangala, Garbage | Route report | Routes to BBMP South SWM contacts | PASS | Verified |
| ROUTE-002| Routing | BTM Layout, Water | Route report | Routes to BWSSB contacts | PASS | Verified |
| ROUTE-003| Routing | Any area, Potholes | Route report | Routes to BBMP Road Infrastructure (Unverified) | PASS | Verified |
| MAN-001 | Manual Report | Photo selected | Tap 'Fast Manual Report (Self Pin)' | Skips AI & redirects straight to email draft | PASS | Verified |
| MAN-002 | Manual Report | Self pin placed | Tap 'Direct Email Draft ✉️' | Generates email body & opens email app | PASS | Verified |
| LANG-001| Language | Profile open | Select Kannada (`kn`) & tap Save | Restarts activity & applies Kannada resources | PASS | Verified |
| UI-001  | UI Layout | Any phone display | Inspect top UI headers | Top padding prevents camera cutout overlap | PASS | Verified |
| EMU-001 | Emulator | Running on emulator | Click camera/gallery/map/email buttons | Works smoothly without manifest/provider crashes | PASS | Verified |
| EMAIL-001| Email | Report entity created | Generate subject & body | Generates formal, personalized complaint text | PASS | Verified |
| EMAIL-002| Email | FileProvider configured | Launch email composer | Attaches image via secure content URI | PASS | Verified |
| EMAIL-003| Email | Email app handoff | Return to app | Displays submission confirmation dialog | PASS | Verified |
| DB-001 | Database | Room DB initialized | Insert report | Report saved as DRAFT in Room | PASS | Verified |
| DB-002 | Database | Report saved | Query reports | LiveData updates dashboard counts & history | PASS | Verified |
| DB-003 | Database | Report status update | Mark as submitted | Status updated to USER_MARKED_SUBMITTED | PASS | Verified |
