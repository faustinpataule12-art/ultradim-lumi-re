package com.nps.ultradim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Affiche un voile noir semi-transparent par-dessus TOUT l'écran (y compris les
 * autres applications), avec une opacité réglable jusqu'à quasi-invisibilité totale.
 *
 * Fonctionne comme un Service en premier plan (avec notification obligatoire côté
 * Android) qui possède sa propre fenêtre système de type "overlay". Le voile est
 * volontairement configuré pour NE JAMAIS intercepter les touchers
 * (FLAG_NOT_TOUCHABLE + FLAG_NOT_FOCUSABLE) : l'utilisateur continue d'utiliser son
 * téléphone normalement en dessous, seule la luminosité perçue change.
 *
 * SÉCURITÉ : la notification persistante contient un bouton "Désactiver" qui
 * fonctionne même quand l'écran est quasi noir (le volet de notifications reste
 * accessible par-dessus l'overlay). C'est le filet de sécurité si jamais l'écran
 * devient trop sombre pour retrouver l'icône de l'app.
 */
public class DimOverlayService extends Service {

    public static final String ACTION_START = "com.nps.ultradim.action.START";
    public static final String ACTION_UPDATE = "com.nps.ultradim.action.UPDATE";
    public static final String ACTION_STOP = "com.nps.ultradim.action.STOP";
    public static final String ACTION_TOGGLE = "com.nps.ultradim.action.TOGGLE";
    public static final String EXTRA_PERCENT = "percent"; // 0..MAX_PERCENT

    // Plafond volontaire : l'écran doit rester visible, jamais totalement noir/invisible.
    public static final int MAX_PERCENT = 90;
    public static final int DEFAULT_PERCENT = 75;

    private static final String CHANNEL_ID = "ultradim_overlay";
    private static final int NOTIF_ID = 7171;

    public static volatile boolean isActive = false;
    public static volatile int currentPercent = 0;

    private WindowManager windowManager;
    private View overlayView;
    private NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createChannelIfNeeded();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            removeOverlay();
            isActive = false;
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_TOGGLE.equals(action)) {
            if (isActive) {
                removeOverlay();
                isActive = false;
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            // sinon on continue ci-dessous pour demarrer avec le dernier pourcentage connu
        }

        int percent = intent.getIntExtra(EXTRA_PERCENT, currentPercent > 0 ? currentPercent : DEFAULT_PERCENT);
        percent = Math.max(0, Math.min(MAX_PERCENT, percent));
        currentPercent = percent;
        isActive = true;

        startForeground(NOTIF_ID, buildNotification(percent));
        showOrUpdateOverlay(percent);

        return START_STICKY;
    }

    private void showOrUpdateOverlay(int percent) {
        float alpha = percent / 100f;

        if (overlayView == null) {
            overlayView = new View(this);
            overlayView.setBackgroundColor(Color.BLACK);

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;

            try {
                windowManager.addView(overlayView, params);
            } catch (Exception e) {
                // Permission "afficher par-dessus les autres applis" non accordée.
                overlayView = null;
                isActive = false;
                stopSelf();
                return;
            }
        }

        overlayView.setAlpha(alpha);
    }

    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) { }
            overlayView = null;
        }
    }

    private Notification buildNotification(int percent) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentPI = PendingIntent.getActivity(this, 0, openIntent, piFlags);

        Intent stopIntent = new Intent(this, DimOverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(this, 1, stopIntent, piFlags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UltraDim actif — " + percent + "%")
                .setContentText("Touche pour ouvrir, ou désactive directement ici.")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentPI)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Désactiver", stopPI)
                .build();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Filtre d'obscurcissement", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notification persistante tant que le filtre est actif");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
        isActive = false;
    }
}
