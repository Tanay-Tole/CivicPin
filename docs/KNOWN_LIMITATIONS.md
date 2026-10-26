# CIVIC PIN — Known Limitations

1. **Government Email Delivery Tracking:** Android email intents launch external mail apps (e.g. Gmail, Outlook). Due to OS security boundaries, Android apps cannot directly verify whether the user pressed 'Send' inside an external email app. Civic Pin handles this by presenting an explicit post-composer confirmation dialog asking the user to confirm whether they sent the email.
2. **Pothole Authority Mappings:** The supplied civic contact directory contains verified routes for Water, Sewage, and Garbage. Potholes are routed to default BBMP Road Maintenance contacts and marked as `UNVERIFIED_DEFAULT`, requiring user verification before sending.
3. **Offline Geocoding:** Reverse geocoding requires internet connectivity or cached system geocoder data. If offline, Civic Pin falls back to exact GPS coordinates and allows manual locality selection from the pre-loaded Bengaluru locality directory.
4. **On-Device AI Model Scope:** The embedded Teachable Machine TensorFlow Lite model is trained specifically on Garbage, Water/Sewage, and Potholes. Unrecognized civic issues default to 'Others' for manual category assignment.
