package com.nps.ultradim;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private SeekBar seekBar;
    private TextView tvPercent, tvStatus;
    private Button btnToggle, btnPermission;

    private final ActivityResultLauncher<Intent> overlayPermLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                refreshPermissionState();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekBar = findViewById(R.id.seekBar);
        tvPercent = findViewById(R.id.tvPercent);
        tvStatus = findViewById(R.id.tvStatus);
        btnToggle = findViewById(R.id.btnToggle);
        btnPermission = findViewById(R.id.btnPermission);

        seekBar.setMax(DimOverlayService.MAX_PERCENT);
        seekBar.setProgress(DimOverlayService.currentPercent > 0
                ? DimOverlayService.currentPercent : DimOverlayService.DEFAULT_PERCENT);
        updatePercentLabel(seekBar.getProgress());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                updatePercentLabel(value);
                if (fromUser && DimOverlayService.isActive) {
                    startOverlay(value);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });

        btnToggle.setOnClickListener(v -> {
            if (!hasOverlayPermission()) {
                Toast.makeText(this, "Autorise d'abord l'affichage par-dessus les autres applis.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (DimOverlayService.isActive) {
                stopOverlay();
            } else {
                startOverlay(seekBar.getProgress());
            }
            updateToggleUi();
        });

        btnPermission.setOnClickListener(v -> requestOverlayPermission());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
        updateToggleUi();
    }

    private void updatePercentLabel(int value) {
        tvPercent.setText("Réduction de luminosité : " + value + "%");
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermLauncher.launch(intent);
        }
    }

    private void refreshPermissionState() {
        boolean granted = hasOverlayPermission();
        btnPermission.setVisibility(granted ? android.view.View.GONE : android.view.View.VISIBLE);
        btnToggle.setEnabled(granted);
        seekBar.setEnabled(granted);
    }

    private void startOverlay(int percent) {
        Intent intent = new Intent(this, DimOverlayService.class);
        intent.setAction(DimOverlayService.ACTION_START);
        intent.putExtra(DimOverlayService.EXTRA_PERCENT, percent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent);
        } else {
            startService(intent);
        }
    }

    private void stopOverlay() {
        Intent intent = new Intent(this, DimOverlayService.class);
        intent.setAction(DimOverlayService.ACTION_STOP);
        startService(intent);
    }

    private void updateToggleUi() {
        boolean active = DimOverlayService.isActive;
        btnToggle.setText(active ? "Désactiver le filtre" : "Activer le filtre");
        tvStatus.setText(active
                ? "Filtre actif — visible même en dehors de l'app."
                : "Filtre inactif.");
    }
}
