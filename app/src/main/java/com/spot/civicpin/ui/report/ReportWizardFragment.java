package com.spot.civicpin.ui.report;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

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
import com.spot.civicpin.ui.ReportAdapter;
import com.spot.civicpin.ui.map.OsmMap;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReportWizardFragment extends Fragment {

    private static final String TAG = "ReportWizardFragment";
    public static final String ARG_MANUAL = "manual_mode";
    private static final int[] STEP_TITLES = {R.string.step_photo, R.string.step_ai, R.string.step_location, R.string.step_route, R.string.step_email};

    private CivicReportRepository repository;
    private PhotoRepository photoRepository;
    private CivicIssueClassifier classifier;
    private CivicLocationManager locationManager;
    private CivicDepartmentRouter departmentRouter;

    private CivicReportEntity currentReport;

    private LinearProgressIndicator wizardProgress;
    private NestedScrollView wizardScroll;
    private TextView tvWizardStep, tvWizardTitle;

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
    private View aiIconBg, llAiActions;
    private ImageView imgAiIcon;

    // Step 3
    private MapView mapView;
    private Marker pinMarker;
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
    /** Where a gallery photo was taken, from its GPS tag; null for camera shots or untagged photos. */
    private double[] photoLatLng;

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
                    photoLatLng = photoRepository.readPhotoLatLong(selectedUri);
                    if (photoLatLng == null) {
                        Toast.makeText(requireContext(), "This photo has no saved location. You can set the spot on the map in step 3.", Toast.LENGTH_LONG).show();
                    }
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

    // Opens the gallery whatever the answer; without it photos just come without GPS.
    private final ActivityResultLauncher<String> mediaLocationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> openGallery()
    );

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> fetchLocationAndSetupMap()
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        OsmMap.init(requireContext());
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
        wizardScroll = view.findViewById(R.id.wizard_scroll);
        tvWizardStep = view.findViewById(R.id.tv_wizard_step);
        tvWizardTitle = view.findViewById(R.id.tv_wizard_title);

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mediaLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION);
            } else {
                openGallery();
            }
        });

        btnStep1Next.setOnClickListener(v -> goToStep2());
        btnSkipToManual.setOnClickListener(v -> {
            step1Container.setVisibility(View.GONE);
            step2Container.setVisibility(View.VISIBLE);
            setStep(2, step2Container);
            showManualPicker();
        });

        // Step 2 init
        pbAiLoading = view.findViewById(R.id.pb_ai_loading);
        tvAiResult = view.findViewById(R.id.tv_ai_result);
        tvAiConfidence = view.findViewById(R.id.tv_ai_confidence);
        tvAiQuestion = view.findViewById(R.id.tv_ai_question);
        llCategorySelector = view.findViewById(R.id.ll_category_selector);
        aiIconBg = view.findViewById(R.id.ai_icon_bg);
        imgAiIcon = view.findViewById(R.id.img_ai_icon);
        llAiActions = view.findViewById(R.id.ll_ai_actions);

        MaterialButton btnAiConfirm = view.findViewById(R.id.btn_ai_confirm);
        MaterialButton btnAiChange = view.findViewById(R.id.btn_ai_change);

        btnAiConfirm.setOnClickListener(v -> goToStep3());
        btnAiChange.setOnClickListener(v -> {
            llCategorySelector.setVisibility(View.VISIBLE);
            wizardScroll.post(() -> wizardScroll.smoothScrollTo(0, step2Container.getTop() + llCategorySelector.getTop()));
        });

        view.findViewById(R.id.btn_cat_garbage).setOnClickListener(v -> selectManualCategory("Garbage"));
        view.findViewById(R.id.btn_cat_water).setOnClickListener(v -> selectManualCategory("Water_Leak_Sewer"));
        view.findViewById(R.id.btn_cat_potholes).setOnClickListener(v -> selectManualCategory("Potholes"));
        view.findViewById(R.id.btn_cat_others).setOnClickListener(v -> selectManualCategory("Others"));

        // Step 3 init
        mapView = view.findViewById(R.id.map_view);
        OsmMap.setup(mapView);
        mapView.getController().setZoom(11.5);
        mapView.getController().setCenter(new GeoPoint(12.9716, 77.5946));
        // Tapping the map drops the pin there, even when GPS never answers.
        mapView.getOverlays().add(0, new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                placePin(p, false);
                onUserPinned(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        // The map sits inside a scroll view: let the map keep its own drag gestures.
        mapView.setOnTouchListener((v, e) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

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

        setStep(1, null);
        if (getArguments() != null && getArguments().getBoolean(ARG_MANUAL, false)) {
            btnSkipToManual.performClick();
        }

        return view;
    }

    /** Updates the header for step n (1..5) and fades the step's content in. */
    private void setStep(int n, @Nullable View container) {
        tvWizardStep.setText(getString(R.string.wizard_step, n));
        tvWizardTitle.setText(STEP_TITLES[n - 1]);
        wizardProgress.setProgressCompat(n * 20, true);
        wizardScroll.scrollTo(0, 0);
        if (container != null) {
            container.setAlpha(0f);
            container.setTranslationY(24f);
            container.animate().alpha(1f).translationY(0f).setDuration(240).start();
        }
    }

    private void showAiCategory(String category) {
        imgAiIcon.setVisibility(View.VISIBLE);
        imgAiIcon.setImageResource(ReportAdapter.iconFor(category));
        imgAiIcon.setImageTintList(ColorStateList.valueOf(ReportAdapter.colorFor(requireContext(), category)));
        aiIconBg.setBackgroundTintList(ColorStateList.valueOf(ReportAdapter.softColorFor(requireContext(), category)));
    }

    /** No photo to classify: the person picks the category themselves. */
    private void showManualPicker() {
        pbAiLoading.setVisibility(View.GONE);
        imgAiIcon.setVisibility(View.VISIBLE);
        tvAiResult.setText(R.string.ai_manual_title);
        tvAiConfidence.setVisibility(View.GONE);
        tvAiQuestion.setText(R.string.ai_manual_body);
        llAiActions.setVisibility(View.GONE);
        llCategorySelector.setVisibility(View.VISIBLE);
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

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Gallery unavailable. Use fast manual report.", Toast.LENGTH_SHORT).show();
        }
    }

    private void snapPhotograph() {
        if (imageCapture == null) return;
        File photoFile = photoRepository.createPhotoFile();
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                if (!isAdded()) return;
                photoLatLng = null; // taken right here, so the phone's GPS is the right spot
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
        setStep(2, step2Container);

        currentReport.imagePath = savedImagePath;

        if (classifier != null && capturedBitmap != null) {
            pbAiLoading.setVisibility(View.VISIBLE);
            imgAiIcon.setVisibility(View.INVISIBLE);
            llAiActions.setVisibility(View.INVISIBLE);
            tvAiConfidence.setVisibility(View.INVISIBLE);
            tvAiQuestion.setText(R.string.ai_is_correct);
            tvAiResult.setText(R.string.ai_detecting);
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
                    tvAiConfidence.setVisibility(View.VISIBLE);
                    llAiActions.setVisibility(View.VISIBLE);
                    showAiCategory(result.getInternalLabel());

                    if (result.getConfidence() < 0.75f || "Others".equals(result.getInternalLabel())) {
                        tvAiQuestion.setText(R.string.ai_low_confidence);
                        llCategorySelector.setVisibility(View.VISIBLE);
                    }
                });
            }).start();
        } else {
            showManualPicker();
        }
    }

    private void selectManualCategory(String category) {
        currentReport.confirmedCategory = category;
        String displayName = CivicIssueClassifier.getDisplayNameForLabel(category);
        tvAiResult.setText(getString(R.string.ai_detected, displayName));
        showAiCategory(category);
        llCategorySelector.setVisibility(View.GONE);
        goToStep3();
    }

    private void goToStep3() {
        step2Container.setVisibility(View.GONE);
        step3Container.setVisibility(View.VISIBLE);
        setStep(3, step3Container);

        if (mapView != null) {
            mapView.onResume();
            mapView.requestLayout();
        }

        if (photoLatLng != null) {
            Location fromPhoto = new Location("PHOTO_EXIF");
            fromPhoto.setLatitude(photoLatLng[0]);
            fromPhoto.setLongitude(photoLatLng[1]);
            locationManager.processLocation(fromPhoto, "PHOTO_EXIF", showLocation);
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            fetchLocationAndSetupMap();
        }
    }

    private void fetchLocationAndSetupMap() {
        locationManager.getCurrentLocation(showLocation);
    }

    /** Fills step 3 (pin, coordinates, address, locality) from a phone GPS or photo location. */
    private final CivicLocationManager.LocationCallback showLocation = new CivicLocationManager.LocationCallback() {
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

                    String how = "PHOTO_EXIF".equals(loc.source) ? "from photo"
                            : String.format(Locale.getDefault(), "accurate to %.0f m", loc.accuracy);
                    tvLocationCoords.setText(String.format(Locale.getDefault(), "%.5f, %.5f  ·  %s", loc.latitude, loc.longitude, how));
                    tvLocationAddress.setText(loc.address != null ? loc.address : "Bengaluru, Karnataka");

                    if (loc.locality != null && actvLocality != null) {
                        actvLocality.setText(loc.locality, false);
                    }

                    placePin(new GeoPoint(loc.latitude, loc.longitude), true);
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
    };

    /** Puts the report pin on the map; tapping the map or dragging the pin moves it. */
    private void placePin(GeoPoint pos, boolean recenter) {
        if (mapView == null || !isAdded()) return;
        if (pinMarker == null) {
            pinMarker = new Marker(mapView);
            pinMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pinMarker.setInfoWindow(null);
            pinMarker.setDraggable(true);
            pinMarker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {}

                @Override
                public void onMarkerDragStart(Marker marker) {}

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    onUserPinned(marker.getPosition());
                }
            });
            mapView.getOverlays().add(pinMarker);
        }
        pinMarker.setIcon(OsmMap.pin(requireContext(), OsmMap.colorFor(requireContext(), currentReport.confirmedCategory)));
        pinMarker.setPosition(pos);
        if (recenter) {
            mapView.getController().setZoom(16.5);
            mapView.getController().setCenter(pos);
        }
        mapView.invalidate();
    }

    private void onUserPinned(GeoPoint p) {
        currentReport.latitude = p.getLatitude();
        currentReport.longitude = p.getLongitude();
        currentReport.locationSource = "USER_MAP_PIN";
        tvLocationCoords.setText(String.format(Locale.getDefault(), "%.5f, %.5f  ·  pinned by you", p.getLatitude(), p.getLongitude()));

        Location pinned = new Location("USER_MAP_PIN");
        pinned.setLatitude(p.getLatitude());
        pinned.setLongitude(p.getLongitude());
        locationManager.processLocation(pinned, "USER_MAP_PIN", new CivicLocationManager.LocationCallback() {
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
    }

    private void goToStep4() {
        if (actvLocality != null && actvLocality.getText() != null) {
            currentReport.locality = actvLocality.getText().toString().trim();
        }
        step3Container.setVisibility(View.GONE);
        step4Container.setVisibility(View.VISIBLE);
        setStep(4, step4Container);

        DepartmentRoute route = departmentRouter.routeReport(currentReport.confirmedCategory, currentReport.locality, null);
        if (route != null) {
            currentReport.departmentName = route.departmentName;
            currentReport.toEmails = route.toEmails;
            currentReport.ccEmails = route.ccEmails;

            tvDeptName.setText(route.departmentName);
            tvDeptVerification.setText(route.verificationStatus.replace("_", " ").toLowerCase(Locale.ROOT) + "  ·  " + route.notes);
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
        setStep(5, step5Container);

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
        setStep(1, step1Container);
        if (imgPreview != null) imgPreview.setImageDrawable(null);
        if (btnStep1Next != null) btnStep1Next.setEnabled(false);
        capturedBitmap = null;
        savedImagePath = null;
        photoLatLng = null;
        llCategorySelector.setVisibility(View.GONE);
        llAiActions.setVisibility(View.VISIBLE);
        currentReport = new CivicReportEntity();
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
    public void onDestroyView() {
        if (mapView != null) mapView.onDetach();
        mapView = null;
        pinMarker = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (classifier != null) classifier.close();
        super.onDestroy();
    }
}
