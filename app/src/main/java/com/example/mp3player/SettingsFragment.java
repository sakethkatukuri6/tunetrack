package com.example.mp3player;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.mp3player.lan.LanSyncManager;

public class SettingsFragment extends Fragment {

    private RadioGroup rgTheme;
    private RadioButton rbLight;
    private RadioButton rbDark;
    private RadioButton rbSystem;
    private TextView tvLanIp;
    private Button btnOpenLanModal;
    private TextView tvEqStatus;
    private Button btnEqBass;
    private Button btnEqRock;
    private Button btnEqFlat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        rgTheme = view.findViewById(R.id.rgTheme);
        rbLight = view.findViewById(R.id.rbLight);
        rbDark = view.findViewById(R.id.rbDark);
        rbSystem = view.findViewById(R.id.rbSystem);
        tvLanIp = view.findViewById(R.id.tvLanIp);
        btnOpenLanModal = view.findViewById(R.id.btnOpenLanModal);
        tvEqStatus = view.findViewById(R.id.tvEqStatus);
        btnEqBass = view.findViewById(R.id.btnEqBass);
        btnEqRock = view.findViewById(R.id.btnEqRock);
        btnEqFlat = view.findViewById(R.id.btnEqFlat);

        setupThemeGroup();
        setupLanSection();
        setupEqSection();

        return view;
    }

    private void setupThemeGroup() {
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            rbLight.setChecked(true);
        } else if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            rbDark.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rbDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.rbSystem) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
    }

    private void setupLanSection() {
        if (getContext() != null) {
            String serverUrl = LanSyncManager.getServerUrl(getContext());
            tvLanIp.setText("Server URL: " + serverUrl);
        }

        btnOpenLanModal.setOnClickListener(v -> {
            if (getContext() != null) {
                String serverUrl = LanSyncManager.getServerUrl(getContext());
                new AlertDialog.Builder(requireContext())
                        .setTitle("LAN Audio Cast")
                        .setMessage("Scan this QR code from another device on the same Wi-Fi network to sync playback.\n\nServer: " + serverUrl + "\n\nAny browser on Wi-Fi can connect to stream and sync live audio.")
                        .setPositiveButton("Done", null)
                        .show();
            }
        });
    }

    private void setupEqSection() {
        btnEqBass.setOnClickListener(v -> {
            tvEqStatus.setText("Active Preset: Deep Bass Booster");
            Toast.makeText(getContext(), "Bass Booster enabled", Toast.LENGTH_SHORT).show();
        });

        btnEqRock.setOnClickListener(v -> {
            tvEqStatus.setText("Active Preset: Rock & Live Stage");
            Toast.makeText(getContext(), "Rock preset enabled", Toast.LENGTH_SHORT).show();
        });

        btnEqFlat.setOnClickListener(v -> {
            tvEqStatus.setText("Active Preset: Balanced Flat Studio");
            Toast.makeText(getContext(), "Flat studio preset enabled", Toast.LENGTH_SHORT).show();
        });
    }
}

