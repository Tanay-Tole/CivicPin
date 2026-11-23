package com.spot.civicpin;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.spot.civicpin.ui.history.HistoryFragment;
import com.spot.civicpin.ui.home.HomeFragment;
import com.spot.civicpin.ui.map.MapFragment;
import com.spot.civicpin.ui.profile.ProfileFragment;
import com.spot.civicpin.ui.report.ReportWizardFragment;
import com.spot.civicpin.utils.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private boolean openReportInManualMode;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Android 15 draws edge to edge: keep content clear of the status bar.
        View host = findViewById(R.id.nav_host_fragment);
        ViewCompat.setOnApplyWindowInsetsListener(host, (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_report) {
                selectedFragment = new ReportWizardFragment();
                if (openReportInManualMode) {
                    Bundle args = new Bundle();
                    args.putBoolean(ReportWizardFragment.ARG_MANUAL, true);
                    selectedFragment.setArguments(args);
                    openReportInManualMode = false;
                }
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    public void navigateToReportTab() {
        navigateToReportTab(false);
    }

    /** manual = open the report flow straight at the category picker, skipping the photo. */
    public void navigateToReportTab(boolean manual) {
        if (bottomNav != null) {
            openReportInManualMode = manual;
            bottomNav.setSelectedItemId(R.id.nav_report);
        }
    }

    private void loadFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) return;
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.screen_in, R.anim.screen_out)
                .replace(R.id.nav_host_fragment, fragment)
                .commitAllowingStateLoss();
    }
}
