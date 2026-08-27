# VTT Explorer

Application Android de navigation et de création de boucles VTT / VTC / vélo tout-terrain.

**Nom de travail :** VTT Explorer  
**Stack :** Kotlin • Jetpack Compose • Material 3 • MapLibre (OpenStreetMap) • Architecture MVVM + Repository • Koin • Room • Coroutines

---

## Architecture

```
app/
├── data/
│   ├── location/          # GPS FusedLocationProvider + service navigation
│   ├── maps/              # Abstraction MapProvider + MapLibre
│   ├── routing/           # Moteur de routage (GraphHopper-ready + générateur de boucles)
│   ├── database/          # Room (historique des sorties)
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/        # Interfaces
│   └── usecase/
├── presentation/
│   ├── map/
│   ├── navigation/
│   ├── route_generator/
│   ├── history/
│   ├── settings/
│   └── components/
└── di/                    # Koin modules
```

Le fournisseur de cartes est isolé (`MapProvider`) pour pouvoir remplacer MapLibre ultérieurement.

Le moteur de routage (`RoutingEngine`) est abstrait. L’implémentation MVP génère des boucles réalistes côté client. Elle est conçue pour être remplacée par :

- **GraphHopper** (API cloud ou self-hosted) avec profil `mtb` / custom model
- **BRouter** (excellent hors-ligne, très adapté VTT)
- **Valhalla** ou OSRM avec profil vélo

---

## Fonctionnalités MVP implémentées

- Carte interactive plein écran (MapLibre / OSM)
- Géolocalisation temps réel (Fused Location)
- Création de boucles avec distance, difficulté, type de vélo, préférences (chemins, pistes, dénivelé, éviter routes principales / goudronnées)
- Affichage distance réelle, durée estimée, dénivelé, % chemins / pistes / routes
- Mode navigation (instructions, stats vitesse / distance / D+)
- Historique des sorties (Room)
- Paramètres (voix, recalcul, préférences VTT)
- Gestion des permissions localisation avec rationale
- Architecture prête pour GPX, offline maps, recalcul intelligent, marqueur VTT rotatif

---

## Prérequis

- Android Studio Ladybug (2024.2) ou plus récent
- JDK 17
- SDK Android 35 (compileSdk)
- minSdk 26

---

## Ouverture du projet

1. Ouvrir Android Studio
2. **File → Open** → sélectionner le dossier `VTTExplorer`
3. Laisser Gradle synchroniser (première fois : téléchargement des dépendances)

Si la synchronisation échoue à cause de MapLibre ou d’une dépendance, vérifier la connexion Internet et invalider les caches (**File → Invalidate Caches**).

---

## Compilation

### Debug APK

```bash
./gradlew assembleDebug
```

L’APK se trouve dans :

```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (signé)

1. Créer un keystore (une seule fois) :

```bash
keytool -genkey -v -keystore vtt-explorer.keystore -alias vtt -keyalg RSA -keysize 2048 -validity 10000
```

2. Ajouter dans `app/build.gradle.kts` (ou mieux via `local.properties` + `signingConfigs`) :

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../vtt-explorer.keystore")
            storePassword = "VOTRE_MOT_DE_PASSE"
            keyAlias = "vtt"
            keyPassword = "VOTRE_MOT_DE_PASSE"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

3. Compiler :

```bash
./gradlew assembleRelease
```

### Android App Bundle (.aab)

```bash
./gradlew bundleRelease
```

Le fichier se trouve dans `app/build/outputs/bundle/release/`.

---

## Installation sur smartphone

1. Activer le **mode développeur** + **débogage USB** sur le téléphone
2. Brancher en USB **ou** transférer l’APK
3. Avec ADB :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

4. Ou ouvrir le fichier APK directement sur le téléphone (autoriser les sources inconnues si nécessaire)

---

## Clés API (optionnel pour production)

| Service              | Usage                          | Où l’obtenir                          | Où la renseigner                  |
|----------------------|--------------------------------|---------------------------------------|-----------------------------------|
| GraphHopper          | Routage vélo/VTT réel          | https://www.graphhopper.com/          | `BuildConfig.GRAPHHOPPER_API_KEY` |
| MapTiler / OpenMapTiles | Styles de carte de qualité  | https://www.maptiler.com/             | Style URI dans `MapLibreMapComposable` |
| Nominatim (OSM)      | Géocodage (déjà gratuit)       | Respecter la politique d’usage        | Aucune clé                        |

**Ne jamais committer de clé secrète.** Utiliser `local.properties` + `buildConfigField` ou un fichier `secrets.properties` non versionné.

Exemple dans `local.properties` :

```
GRAPHHOPPER_API_KEY=votre_clé
MAPTILER_KEY=votre_clé
```

Puis lire ces valeurs dans le `build.gradle.kts` pour les injecter dans `BuildConfig`.

---

## Marqueur VTT (position + cap)

Dans la version complète, remplacer le point standard par un marqueur vectoriel (VTT vu du dessus) qui pivote selon `Location.bearing`.  
Le `MapLibre` Annotation plugin ou un `SymbolLayer` avec rotation est déjà prévu dans l’architecture.

---

## Fonctionnement hors ligne

L’architecture prévoit :

- Téléchargement de tuiles MapLibre / MBTiles
- Stockage local des parcours (Room + fichiers GPX)
- Remplacement du moteur de routage par **BRouter** (recommandé pour VTT offline)

Le MVP actuel nécessite Internet pour les tuiles de démonstration. Les boucles sont générées localement.

---

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

Tests unitaires prioritaires à compléter :

- Calcul de distance (haversine)
- Pondération des préférences VTT
- Génération de boucle (tolérance distance)
- Import/export GPX

---

## Prochaines phases recommandées

1. Brancher un vrai moteur GraphHopper / BRouter avec custom model VTT (`mtb:scale`, `surface`, `tracktype`, `bicycle=…`)
2. Marqueur VTT rotatif + variante « pas de cap »
3. Géocodage Nominatim + écran Destination
4. Enregistrement de sortie en temps réel + export GPX
5. Profil altimétrique interactif
6. Cartes hors ligne (MapLibre offline packs)
7. Navigation vocale (TTS Android)
8. Recalcul intelligent avec détection d’écart

---

## Avertissement sécurité

Les données cartographiques OpenStreetMap peuvent être incomplètes ou obsolètes.  
**Vérifiez toujours les conditions d’accès et la praticabilité des chemins avant de vous engager.**  
L’application n’encourage jamais à emprunter un chemin explicitement interdit aux vélos.

---

## Licence

Code fourni à titre d’exemple pédagogique / base de projet.  
Adapter les licences des dépendances (MapLibre, etc.) avant distribution publique.

---

**VTT Explorer** — Conçu pour les pratiquants de VTT qui veulent des boucles intelligentes et une navigation claire.

---

## Configuration GraphHopper (routage VTT réel)

### 1. Obtenir une clé API

1. Créer un compte sur [https://www.graphhopper.com/](https://www.graphhopper.com/)
2. Aller dans le Dashboard → **API Keys**
3. Créer une clé

### 2. Renseigner la clé (sécurisé)

```bash
cp local.properties.example local.properties
# Éditer local.properties et coller :
GRAPHHOPPER_API_KEY=votre_cle_ici
```

`local.properties` est dans `.gitignore` → la clé n’est jamais commitée.

Au build, Gradle injecte la valeur dans `BuildConfig.GRAPHHOPPER_API_KEY`.

### 3. Comportement

| Situation                         | Comportement                                      |
|-----------------------------------|---------------------------------------------------|
| Clé présente et valide            | Appels réels GraphHopper (profil mtb / bike)      |
| Clé absente ou vide               | Fallback local (génération géométrique)           |
| Erreur réseau / quota             | Message d’erreur clair dans l’UI                  |

### 4. Profils et custom model VTT

- **VTT** → profil `mtb`
- **VTC / loisir** → profil `bike`
- **Route** → profil `racingbike`

Custom model généré dynamiquement selon les préférences utilisateur :

- `road_class == MOTORWAY / TRUNK` → priorité 0
- `PRIMARY` fortement pénalisé si « Éviter grands axes »
- Surfaces ASPHALT / CONCRETE pénalisées si « Éviter goudronnées »
- `TRACK / PATH / CYCLEWAY` boostés selon les curseurs
- `mtb_rating` plafonné selon la difficulté (Facile → Expert)
- `hike_rating` élevé exclu en mode Facile / Intermédiaire

### 5. Boucles

Utilisation de `algorithm=round_trip` + `round_trip.distance` (mètres) + seed aléatoire pour proposer des parcours différents à chaque « Régénérer ».

### 6. Test rapide (curl)

```bash
curl -X POST "https://graphhopper.com/api/1/route?key=VOTRE_CLE" \
  -H "Content-Type: application/json" \
  -d '{
    "profile": "mtb",
    "points": [[0.34, 46.58]],
    "algorithm": "round_trip",
    "round_trip.distance": 20000,
    "elevation": true,
    "instructions": true,
    "ch.disable": true,
    "custom_model": {
      "priority": [
        { "if": "road_class == MOTORWAY || road_class == TRUNK", "multiply_by": "0" },
        { "if": "road_class == TRACK || road_class == PATH", "multiply_by": "1.8" }
      ]
    }
  }'
```

### 7. Plans GraphHopper

- **Free** : souvent limité à `car`, `bike`, `foot`
- **Starter / Standard** : accès à `mtb`, `racingbike`, custom models plus souples

Si `mtb` renvoie une erreur de profil, changez temporairement dans  
`VttCustomModelBuilder.profileFor()` → retourner `"bike"` pour tous les types.

### 8. Alternative offline (Phase 4)

Pour le hors-ligne, remplacer l’implémentation par **BRouter** (excellent pour VTT) tout en gardant l’interface `RoutingEngine`.
