# CIVIC PIN — Architecture & System Design

## Application Overview
Civic Pin is built using modern native Android architecture in **Java + XML Layouts**, following the MVVM and Repository patterns with a local Room database and on-device AI classification via TensorFlow Lite.

## Key Architectural Layers

```text
com.spot.civicpin
│
├── MainActivity.java                  (BottomNavigationView & Fragment Container)
│
├── ai/                                (TensorFlow Lite On-Device Inference)
│   ├── CivicIssueClassifier.java
│   └── ClassificationResult.java
│
├── camera/                            (CameraX & Photo Storage)
│   └── PhotoRepository.java
│
├── location/                          (Fused Location & Locality Resolution)
│   ├── CivicLocationManager.java
│   ├── BengaluruLocalityResolver.java
│   └── LocationResult.java
│
├── routing/                           (Civic Department Routing)
│   ├── CivicDepartmentRouter.java
│   └── DepartmentRoute.java
│
├── email/                             (Complaint Generation & Intent Handoff)
│   ├── ComplaintEmailGenerator.java
│   └── EmailIntentHelper.java
│
├── data/                              (Room Persistence Library)
│   ├── CivicDatabase.java
│   ├── CivicReportEntity.java
│   ├── CivicReportDao.java
│   ├── ReportEventEntity.java
│   ├── ReportEventDao.java
│   └── CivicReportRepository.java
│
└── ui/                                (User Interface & Navigation)
    ├── HomeFragment.java
    ├── ReportWizardFragment.java
    ├── MapFragment.java
    ├── HistoryFragment.java
    ├── ProfileFragment.java
    ├── ReportAdapter.java
    ├── EventTimelineAdapter.java
    └── ReportDetailActivity.java
```

## Data Model & Status State Machine
Reports progress through explicit state transitions:
1. `DRAFT`: Report created, photograph taken, AI classification run, location selected.
2. `READY_TO_SEND`: Department route & email body generated.
3. `EMAIL_COMPOSER_OPENED`: External email application launched via `Intent.ACTION_SEND`.
4. `USER_MARKED_SUBMITTED`: User explicitly confirms in the dialog that they sent the email.
5. `USER_MARKED_RESOLVED`: User marks issue as fixed by authorities.
6. `CANCELLED`: User deletes or cancels the report.
