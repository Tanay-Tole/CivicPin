package com.spot.civicpin.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.spot.civicpin.R;
import com.spot.civicpin.utils.LocaleHelper;

public class ProfileFragment extends Fragment {

    private SharedPreferences prefs;

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private RadioButton rbLangEn;
    private RadioButton rbLangKn;

    private int logoTapCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireContext().getSharedPreferences("civic_pin_prefs", Context.MODE_PRIVATE);

        etName = view.findViewById(R.id.et_profile_name);
        etEmail = view.findViewById(R.id.et_profile_email);
        rbLangEn = view.findViewById(R.id.rb_lang_en);
        rbLangKn = view.findViewById(R.id.rb_lang_kn);
        ImageView imgLogoEe = view.findViewById(R.id.img_app_logo_ee);

        MaterialButton btnSave = view.findViewById(R.id.btn_save_profile);

        etName.setText(prefs.getString("user_name", "Bengaluru Resident"));
        etEmail.setText(prefs.getString("user_email", ""));

        String savedLang = prefs.getString("user_lang", "en");
        if ("kn".equalsIgnoreCase(savedLang)) {
            rbLangKn.setChecked(true);
        } else {
            rbLangEn.setChecked(true);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String newLang = rbLangKn.isChecked() ? "kn" : "en";

            String currentLang = prefs.getString("user_lang", "en");

            prefs.edit()
                    .putString("user_name", name)
                    .putString("user_email", email)
                    .putString("user_lang", newLang)
                    .apply();

            LocaleHelper.setLocale(requireContext(), newLang);

            Toast.makeText(requireContext(), "Preferences Saved / ಆದ್ಯತೆಗಳನ್ನು ಉಳಿಸಲಾಗಿದೆ!", Toast.LENGTH_SHORT).show();

            if (!newLang.equalsIgnoreCase(currentLang)) {
                requireActivity().recreate();
            }
        });

        // Easter Egg 7 — Hidden About Screen (7 taps)
        imgLogoEe.setOnClickListener(v -> {
            logoTapCount++;
            if (logoTapCount >= 7) {
                logoTapCount = 0;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Civic Pin Developer Credits")
                        .setMessage("Civic Pin v1.0 — Bengaluru's AI Civic Reporting Application\n\nDeveloped with Java, TensorFlow Lite, Google Maps SDK & Room Database.\nNamma Bengaluru! ☕ 🚦")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        return view;
    }
}
