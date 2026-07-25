package com.nps.ultradim;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

/**
 * Tuile dans le panneau de réglages rapides (celui où se trouvent la lampe torche,
 * les données mobiles, etc. — accessible en glissant deux doigts depuis le haut).
 * Un appui active/désactive le filtre au dernier pourcentage utilisé.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class DimTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (!android.provider.Settings.canDrawOverlays(this)) {
            // Pas de permission "afficher par-dessus les autres applis" : on ouvre l'app
            // pour que l'utilisateur puisse l'accorder, impossible de le faire depuis la tuile.
            Intent openApp = new Intent(this, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityAndCollapse(openApp);
            return;
        }

        Intent serviceIntent = new Intent(this, DimOverlayService.class);
        serviceIntent.setAction(DimOverlayService.ACTION_TOGGLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // L'etat reel est mis a jour par le service ; on rafraichit l'affichage
        // de la tuile peu apres pour refleter le nouvel etat.
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(this::refreshTile, 300);
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = DimOverlayService.isActive;
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(active ? "UltraDim (" + DimOverlayService.currentPercent + "%)" : "UltraDim");
        tile.setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_view));
        tile.updateTile();
    }
}
