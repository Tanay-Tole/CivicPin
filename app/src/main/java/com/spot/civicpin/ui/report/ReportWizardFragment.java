package com.spot.civicpin.ui.report;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.spot.civicpin.R;
import com.spot.civicpin.ai.CivicIssueClassifier;
import com.spot.civicpin.ai.ClassificationResult;
import com.spot.civicpin.camera.PhotoRepository;
import com.spot.civicpin.data.CivicReportEntity;
import com.spot.civicpin.data.CivicReportRepository;
import com.spot.civicpin.email.ComplaintEmailGenerator;
import com.spot.civicpin.email.EmailIntentHelper;
import com.spot.civicpin.location.BengaluruLocalityResolver;
import com.spot.civicpin.location.CivicLocationManager;
import com.spot.civicpin.location.LocationResult;
import com.spot.civicpin.routing.CivicDepartmentRouter;
import com.spot.civicpin.routing.DepartmentRoute;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReportWizardFragment extends Fragment {

    private static final String TAG = "ReportWizardFragment";

    private CivicReportRepository repository;
    private PhotoRepository photoRepository;
    private CivicIssueClassifier classifier;
    private CivicLocationManager locationManager;
    private CivicDepartmentRouter departmentRouter;

    private CivicReportEntity currentReport;

    private LinearProgressIndicator wizardProgress;

    // Containers
    private LinearLayout step1Container, step2Container, step3Container, step4Container, step5Container;

    // Step 1
    private PreviewView cameraPreview;
    private ImageView imgPreview;
    private MaterialButton btnSnapPhoto, btnStep1Next;
    private ImageCapture imageCapture;

    // Step 2
    private ProgressBar pbAiLoading;
    private TextView tvAiResult, tvAiConfidence, tvAiQuestion;
    private LinearLayout llCategorySelector;

    // Step 3
    private MapView mapView;
    private GoogleMap googleMap;
    private TextView tvLocationCoords, tvLocationAddress;
    private AutoCompleteTextView actvLocality;

    // Step 4
    private TextView tvDeptName, tvDeptVerification;
    private AutoCompleteTextView actvSubtype;
    private TextInputEditText etToEmails, etCcEmails, etUserDesc;

    // Step 5
    private TextInputEditText etEmailSubject, etEmailBody;

    private Bitmap capturedBitmap;
    private String savedImagePath;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCameraXPreview();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required. You can also select photo from Gallery.", Toast.LENGTH_LONG).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    savedImagePath = photoRepository.processAndSaveImage(selectedUri);
                    if (savedImagePath != null) {
                        capturedBitmap = BitmapFactory.decodeFile(savedImagePath);
                        if (capturedBitmap != null && imgPreview != null) {
                            cameraPreview.setVisibility(View.GONE);
                            imgPreview.setVisibility(View.VISIBLE);
                            btnSnapPhoto.setVisibility(View.GONE);
                            imgPreview.setImageBitmap(capturedBitmap);
                            btnStep1Next.setEnabled(true);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> fetchLocationAndSetupMap()
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_wizard, container, false);

        repository = new CivicReportRepository(requireContext());
        photoRepository = new PhotoRepository(requireContext());
        locationManager = new CivicLocationManager(requireContext());
        departmentRouter = new CivicDepartmentRouter(requireContext());

        try {
            classifier = new CivicIssueClassifier(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TFLite classifier", e);
        }

        currentReport = new CivicReportEntity();

        wizardProgress = view.findViewById(R.id.wizard_progress);

        step1Container = view.findViewById(R.id.step1_container);
        step2Container = view.findViewById(R.id.step2_container);
        step3Container = view.findViewById(R.id.step3_container);
        step4Container = view.findViewById(R.id.step4_container);
        step5Container = view.findViewById(R.id.step5_container);

        // Step 1 init
        cameraPreview = view.findViewById(R.id.camera_preview);
        imgPreview = view.findViewById(R.id.img_preview);
        btnSnapPhoto = view.findViewById(R.id.btn_snap_photo);
        btnStep1Next = view.findViewById(R.id.btn_step1_next);
        MaterialButton btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        MaterialButton btnSelectGallery = view.findViewById(R.id.btn_select_gallery);
        MaterialButton btnSkipToManual = view.findViewById(R.id.btn_skip_to_manual);

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCameraXPreview();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnSnapPhoto.setOnClickListener(v -> snapPhotograph());

        btnSelectGallery.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Gallery unavailable. Use fast manual report.", Toast.LENGTH_SHORT).show();
            }
        });

        btnStep1Next.setOnClickListener(v -> goToStep2());
        btnSkipToManual.setOnClickListener(v -> {
            step1Container.setVisibility(View.GONE);
            step2Container.setVisibility(View.VISIBLE);
            llCategorySelector.setVisibility(View.VISIBLE);
            tvAiResult.setText("Fast Manual Reporting Mode");
            tvAiConfidence.setText("Self-Category & Pin Selection");
        });

        // Step 2 init
        pbAiLoading = view.findViewById(R.id.pb_ai_loading);
        tvAiResult = view.findViewById(R.id.tv_ai_result);
        tvAiConfidence = view.findViewById(R.id.tv_ai_confidence);
        tvAiQuestion = view.findViewById(R.id.tv_ai_question);
        llCategorySelector = view.findViewById(R.id.ll_category_selector);

        MaterialButton btnAiConfirm = view.findViewById(R.id.btn_ai_confirm);
        MaterialButton btnAiChange = view.findViewById(R.id.btn_ai_change);

        btnAiConfirm.setOnClickListener(v -> goToStep3());
        btnAiChange.setOnClickListener(v -> llCategorySelector.setVisibility(View.VISIBLE));

        view.findViewById(R.id.btn_cat_garbage).setOnClickListener(v -> selectManualCategory("Garbage"));
        view.findViewById(R.id.btn_cat_water).setOnClickListener(v -> selectManualCategory("Water_Leak_Sewer"));
        view.findViewById(R.id.btn_cat_potholes).setOnClickListener(v -> selectManualCategory("Potholes"));
        view.findViewById(R.id.btn_cat_others).setOnClickListener(v -> selectManualCategory("Others"));

        // Step 3 init
        mapView = view.findViewById(R.id.map_view);
        try {
            MapsInitializer.initialize(requireContext().getApplicationContext(), MapsInitializer.Renderer.LATEST, null);
            mapView.onCreate(savedInstanceState);
        } catch (Exception e) {
            Log.e(TAG, "Error in MapView onCreate", e);
        }

        tvLocationCoords = view.findViewById(R.id.tv_location_coords);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        actvLocality = view.findViewById(R.id.actv_locality);

        ArrayAdapter<String> localityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, BengaluruLocalityResolver.KNOWN_LOCALITIES);
        actvLocality.setAdapter(localityAdapter);

        view.findViewById(R.id.btn_confirm_location).setOnClickListener(v -> goToStep4());
        view.findViewById(R.id.btn_direct_email_draft).setOnClickListener(v -> {
            goToStep4();
            goToStep5();
        });

        // Step 4 init
        tvDeptName = view.findViewById(R.id.tv_dept_name);
        tvDeptVerification = view.findViewById(R.id.tv_dept_verification);
        actvSubtype = view.findViewById(R.id.actv_subtype);
        etToEmails = view.findViewById(R.id.et_to_emails);
        etCcEmails = view.findViewById(R.id.et_cc_emails);
        etUserDesc = view.findViewById(R.id.et_user_desc);

        List<String> subtypes = Arrays.asList(
                getString(R.string.subtype_water_leak),
                getString(R.string.subtype_sewage),
                getString(R.string.subtype_drain),
                getString(R.string.subtype_unsure)
        );
        ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subtypes);
        actvSubtype.setAdapter(subtypeAdapter);

        view.findViewById(R.id.btn_generate_email).setOnClickListener(v -> goToStep5());

        // Step 5 init
        etEmailSubject = view.findViewById(R.id.et_email_subject);
        etEmailBody = view.findViewById(R.id.et_email_body);

        view.findViewById(R.id.btn_open_email_app).setOnClickListener(v -> launchEmailApp());

        return view;
    }

    private void startCameraXPreview() {
        if (!isAdded()) return;
        cameraPreview.setVisibility(View.VISIBLE);
        imgPreview.setVisibility(View.GONE);
        btnSnapPhoto.setVisibility(View.VISIBLE);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "CameraX preview binding failed", e);
                Toast.makeText(requireContext(), "Camera preview unavailable. Please select from Gallery.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void snapPhotograph() {
        if (imageCapture == null) return;
        File photoFile = photoRepository.createPhotoFile();
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                if (!isAdded()) return;
                savedImagePath = photoFile.getAbsolutePath();
                capturedBitmap = BitmapFactory.decodeFile(savedImagePath);

                cameraPreview.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                btnSnapPhoto.setVisibility(View.GONE);

                if (capturedBitmap != null) {
                    imgPreview.setImageBitmap(capturedBitmap);
                    btnStep1Next.setEnabled(true);
                    Toast.makeText(requireContext(), "Photograph captured successfully!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Photo capture failed. Try selecting from Gallery.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goToStep2() {
        step1Container.setVisibility(View.GONE);
        step2Container.setVisibility(View.VISIBLE);
        wizardProgress.setProgress(40);

        currentReport.imagePath = savedImagePath;

        if (classifier != null && capturedBitmap != null) {
            pbAiLoading.setVisibility(View.VISIBLE);
            tvAiResult.setText("Analyzing image... Faster than Indiranagar rent rises! 🚀");
            new Thread(() -> {
                ClassificationResult result = classifier.classifyImage(capturedBitmap);
                if (!isAdded() || getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    pbAiLoading.setVisibility(View.GONE);
                    currentReport.aiPredictedCategory = result.getInternalLabel();
                    currentReport.aiConfidence = result.getConfidence();
                    currentReport.confirmedCategory = result.getInternalLabel();

                    tvAiResult.setText(getString(R.string.ai_detected, result.getDisplayName()));
                    tvAiConfidence.setText(getString(R.string.ai_confidence, result.getConfidencePercentage()));

                    if (result.getConfidence() < 0.75f || "Others".equals(result.getInternalLabel())) {
                        tvAiQuestion.setText(R.string.ai_low_confidence);
                        llCategorySelector.setVisibility(View.VISIBLE);
                    }
                });
            }).start();
        } else {
            pbAiLoading.setVisibility(View.GONE);
            tvAiResult.setText("Manual Mode Active. Select category below:");
            llCategorySelector.setVisibility(View.VISIBLE);
        }
    }

    private void selectManualCategory(String category) {
        currentReport.confirmedCategory = category;
        String displayName = CivicIssueClassifier.getDisplayNameForLabel(category);
        tvAiResult.setText(getString(R.string.ai_detected, displayName));
        llCategorySelector.setVisibility(View.GONE);
        goToStep3();
    }

    private void goToStep3() {
        step2Container.setVisibility(View.GONE);
        step3Container.setVisibility(View.VISIBLE);
        wizardProgress.setProgress(60);

        if (mapView != null) {
            mapView.onResume();
            mapView.requestLayout();
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            fetchLocationAndSetupMap();
        }
    }

    private void fetchLocationAndSetupMap() {
        locationManager.getCurrentLocation(new CivicLocationManager.LocationCallback() {
            @Override
            public void onSuccess(LocationResult loc) {
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    currentReport.latitude = loc.latitude;
                    currentReport.longitude = loc.longitude;
                    currentReport.locationAccuracy = loc.accuracy;
                    currentReport.locationTimestamp = loc.timestamp;
                    currentReport.locationSource = loc.source;
                    currentReport.address = loc.address;
                    currentReport.locality = loc.locality;

                    tvLocationCoords.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f (Accuracy: %.1fm)", loc.latitude, loc.longitude, loc.accuracy));
                    tvLocationAddress.setText(loc.address != null ? loc.address : "Bengaluru, Karnataka");

                    if (loc.locality != null && actvLocality != null) {
                        actvLocality.setText(loc.locality, false);
                    }

                    if (mapView != null && isGooglePlayServicesAvailable()) {
                        try {
                            mapView.getMapAsync(map -> {
                                googleMap = map;
                                LatLng pos = new LatLng(loc.latitude, loc.longitude);
                                googleMap.clear();
                                googleMap.addMarker(new MarkerOptions().position(pos).title("Report Location").draggable(true));
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));

                                // Interactive Map Pinpoint on Click
                                googleMap.setOnMapClickListener(latLng -> {
                                    googleMap.clear();
                                    googleMap.addMarker(new MarkerOptions().position(latLng).title("Self Pinned Location").draggable(true));
                                    currentReport.latitude = latLng.latitude;
                                    currentReport.longitude = latLng.longitude;
                                    currentReport.locationSource = "USER_MAP_PIN";
                                    tvLocationCoords.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f (Self Pin)", latLng.latitude, latLng.longitude));

                                    Location dummyLoc = new Location("USER_MAP_PIN");
                                    dummyLoc.setLatitude(latLng.latitude);
                                    dummyLoc.setLongitude(latLng.longitude);
                                    locationManager.processLocation(dummyLoc, "USER_MAP_PIN", new CivicLocationManager.LocationCallback() {
                                        @Override
                                        public void onSuccess(LocationResult res) {
                                            if (!isAdded() || getActivity() == null) return;
                                            requireActivity().runOnUiThread(() -> {
                                                if (!isAdded()) return;
                                                currentReport.address = res.address;
                                                currentReport.locality = res.locality;
                                                tvLocationAddress.setText(res.address != null ? res.address : "Bengaluru, Karnataka");
                                                if (res.locality != null && actvLocality != null) {
                                                    actvLocality.setText(res.locality, false);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(String errorMsg) {}
                                    });
                                });

                                googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                                    @Override
                                    public void onMarkerDragStart(@NonNull com.google.android.gms.maps.model.Marker marker) {}

                                    @Override
                                    public void onMarkerDrag(@NonNull com.google.android.gms.maps.model.Marker marker) {}

                                    @Override
                                    public void onMarkerDragEnd(@NonNull com.google.android.gms.maps.model.Marker marker) {
                                        LatLng newPos = marker.getPosition();
                                        currentReport.latitude = newPos.latitude;
                                        currentReport.longitude = newPos.longitude;
                                        currentReport.locationSource = "USER_MAP_PIN";
                                        tvLocationCoords.setText(String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f (User Pin)", newPos.latitude, newPos.longitude));
                                    }
                                });
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error in getMapAsync", e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean isGooglePlayServicesAvailable() {
        if (getContext() == null) return false;
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(getContext());
        return resultCode == ConnectionResult.SUCCESS;
    }

    private void goToStep4() {
        if (actvLocality != null && actvLocality.getText() != null) {
            currentReport.locality = actvLocality.getText().toString().trim();
        }
        step3Container.setVisibility(View.GONE);
        step4Container.setVisibility(View.VISIBLE);
        wizardProgress.setProgress(80);

        DepartmentRoute route = departmentRouter.routeReport(currentReport.confirmedCategory, currentReport.locality, null);
        if (route != null) {
            currentReport.departmentName = route.departmentName;
            currentReport.toEmails = route.toEmails;
            currentReport.ccEmails = route.ccEmails;

            tvDeptName.setText(route.departmentName);
            tvDeptVerification.setText("Status: " + route.verificationStatus + " • " + route.notes);
            etToEmails.setText(route.toEmails);
            etCcEmails.setText(route.ccEmails);
        }
    }

    private void goToStep5() {
        currentReport.issueSubtype = (actvSubtype != null && actvSubtype.getText() != null) ? actvSubtype.getText().toString() : "";
        currentReport.toEmails = (etToEmails != null && etToEmails.getText() != null) ? etToEmails.getText().toString().trim() : "";
        currentReport.ccEmails = (etCcEmails != null && etCcEmails.getText() != null) ? etCcEmails.getText().toString().trim() : "";
        currentReport.description = (etUserDesc != null && etUserDesc.getText() != null) ? etUserDesc.getText().toString().trim() : "";

        String subject = ComplaintEmailGenerator.generateSubject(currentReport);
        String body = ComplaintEmailGenerator.generateBody(currentReport, "Bengaluru Resident");

        currentReport.emailSubject = subject;
        currentReport.emailBody = body;

        if (etEmailSubject != null) etEmailSubject.setText(subject);
        if (etEmailBody != null) etEmailBody.setText(body);

        step4Container.setVisibility(View.GONE);
        step5Container.setVisibility(View.VISIBLE);
        wizardProgress.setProgress(100);

        repository.insertReport(currentReport, "Report created as DRAFT");
    }

    private void launchEmailApp() {
        currentReport.emailSubject = (etEmailSubject != null && etEmailSubject.getText() != null) ? etEmailSubject.getText().toString() : currentReport.emailSubject;
        currentReport.emailBody = (etEmailBody != null && etEmailBody.getText() != null) ? etEmailBody.getText().toString() : currentReport.emailBody;
        currentReport.status = "EMAIL_COMPOSER_OPENED";
        currentReport.emailComposerOpenedAt = System.currentTimeMillis();

        repository.updateReport(currentReport, "EMAIL_OPENED", "Email composer opened");

        Intent emailIntent = EmailIntentHelper.createEmailIntent(requireContext(), currentReport);
        try {
            startActivity(emailIntent);
            showSubmissionConfirmationDialog();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_no_email_app, Toast.LENGTH_LONG).show();
        }
    }

    private void showSubmissionConfirmationDialog() {
        if (!isAdded() || getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_submission_title)
                .setMessage(R.string.dialog_submission_msg)
                .setPositiveButton(R.string.btn_mark_submitted, (dialog, which) -> {
                    currentReport.status = "USER_MARKED_SUBMITTED";
                    currentReport.userConfirmedSubmittedAt = System.currentTimeMillis();
                    repository.updateReport(currentReport, "SUBMITTED", "User marked report as submitted");
                    Toast.makeText(requireContext(), R.string.ee_coffee_toast, Toast.LENGTH_LONG).show();
                    resetWizard();
                })
                .setNegativeButton(R.string.btn_keep_draft, (dialog, which) -> {
                    currentReport.status = "DRAFT";
                    repository.updateReport(currentReport, "DRAFT_KEPT", "Report kept as draft");
                    resetWizard();
                })
                .setNeutralButton(R.string.btn_reopen_email, (dialog, which) -> launchEmailApp())
                .setCancelable(false)
                .show();
    }

    private void resetWizard() {
        step5Container.setVisibility(View.GONE);
        step1Container.setVisibility(View.VISIBLE);
        wizardProgress.setProgress(20);
        if (imgPreview != null) imgPreview.setImageDrawable(null);
        if (btnStep1Next != null) btnStep1Next.setEnabled(false);
        currentReport = new CivicReportEntity();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (classifier != null) classifier.close();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
