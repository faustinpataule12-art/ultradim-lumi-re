# UltraDim

Application Android qui réduit fortement la luminosité perçue de l'écran via
un voile noir semi-transparent superposé à **tout** l'écran (y compris les
autres applications) — utile en environnement très sombre où le minimum de
luminosité du système ne suffit pas.

**Important : le filtre est volontairement plafonné à 90%.** L'écran reste
toujours visible, il ne devient jamais totalement noir ni invisible.

## Fonctionnalités

- Curseur de réglage (0 à 90%) avec aperçu en direct
- Fonctionne même quand l'app est fermée / en dehors de l'app (overlay système)
- **Tuile de réglages rapides** (comme la lampe torche) pour activer/désactiver
  en un tap depuis le volet de notifications
- Bouton "Désactiver" directement dans la notification persistante, accessible
  même écran très sombre — filet de sécurité pour toujours pouvoir couper le
  filtre facilement

## Permissions requises

- `SYSTEM_ALERT_WINDOW` (« afficher par-dessus les autres applis ») — à
  accorder manuellement au premier lancement, Android l'exige pour ce type
  de fonctionnalité
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — pour que le
  filtre reste actif de façon fiable tant qu'il est activé
- `POST_NOTIFICATIONS` — pour afficher la notification de contrôle

## Structure du projet

```
UltraDim/
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/nps/ultradim/
│       │   ├── MainActivity.java       <- UI, curseur, permission
│       │   ├── DimOverlayService.java  <- voile noir superposé, plafonné à 90%
│       │   └── DimTileService.java     <- tuile de réglages rapides
│       └── res/...
├── build.gradle
└── settings.gradle
```

Créé par NPS.NELSON
