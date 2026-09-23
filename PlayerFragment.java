package com.example.mp3player;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PlayerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Assume you have a fragment_player.xml with your buttons
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        // Find the QR share button and attach the listener
        ImageButton btnLanShare = view.findViewById(R.id.btnLanShare);
        if (btnLanShare != null) {
            btnLanShare.setOnClickListener(v -> showQrModal());
        }

        return view;
    }

    private void showQrModal() {
        // Pop up the simulated LAN cast instructions
        new AlertDialog.Builder(requireContext())
            .setTitle("LAN Audio Cast")
            .setMessage("Scan this QR code from another device on the same Wi-Fi network to sync playback.\n\nServer: 192.168.1.45:8080")
            .setPositiveButton("Done", null)
            .show();
    }
}