# Compiler VTT Explorer avec Codemagic

Codemagic est un service cloud spécialisé apps mobiles.  
**Plan free (individuel) : 500 minutes de build / mois** — largement suffisant pour quelques APK.

Fichier déjà présent à la racine du projet : `codemagic.yaml`

---

## Étapes (depuis un téléphone ou un PC)

### 1. Mettre le code sur GitHub (obligatoire)

Codemagic se connecte à un dépôt Git.

1. Compte GitHub : https://github.com  
2. **New repository** → nom `VTTExplorer`  
3. Envoyer le dossier du projet (upload web ou `git push`)

Le dépôt doit contenir au minimum :
- `codemagic.yaml` (à la racine)
- `gradlew` + `gradle/`
- `app/`
- `settings.gradle.kts`, `build.gradle.kts`

### 2. Compte Codemagic

1. Aller sur https://codemagic.io  
2. **Sign up** avec le **même compte GitHub**  
3. Autoriser l’accès aux dépôts

### 3. Ajouter l’application

1. Dashboard Codemagic → **Add application**  
2. Choisir le dépôt `VTTExplorer`  
3. Project type : **Android App** (ou “Android” / native)  
4. Confirmer

Codemagic détecte automatiquement `codemagic.yaml`.

### 4. Lancer le build

1. Sélectionner le workflow **VTT Explorer — APK Debug**  
2. **Start new build**  
3. Branche : `main` (ou `master`)  
4. Attendre 8–20 minutes (1ʳᵉ fois plus long)

### 5. Télécharger l’APK

1. Build terminé (vert)  
2. Section **Artifacts**  
3. Télécharger `app-debug.apk` (ou le zip d’artifacts)  
4. Ouvrir le fichier sur le téléphone → installer  
   (autoriser « sources inconnues » si demandé)

---

## Clé GraphHopper (optionnel)

1. Codemagic → Application → **Environment variables**  
2. Variable : `GRAPHHOPPER_API_KEY`  
3. Valeur : votre clé  
4. Cocher **Secure**  
5. Relancer un build  

Sans clé → mode fallback (boucles générées localement).

---

## Dépannage

| Problème | Solution |
|----------|----------|
| Workflow non détecté | Vérifier que `codemagic.yaml` est à la **racine** du dépôt |
| `gradlew: Permission denied` | Déjà géré dans le yaml (`chmod +x`) |
| Build failed (SDK) | Relancer ; le script écrit `local.properties` automatiquement |
| Minutes épuisées | Attendre le mois suivant (500 min recharge) ou GitHub Actions |
| App ne s’installe pas | Utiliser bien l’APK **debug** ; Android 8+ (API 26) |

---

## Comparaison rapide

| | Codemagic | GitHub Actions |
|--|-----------|----------------|
| Compte | codemagic.io + GitHub | GitHub seul |
| Minutes free | 500 / mois | ~2000 / mois (public) |
| Config | `codemagic.yaml` | `.github/workflows/build-apk.yml` |
| Téléchargement APK | Artifacts du build | Artifacts du workflow |

Les deux sont déjà configurés dans ce projet. Choisissez celui qui vous arrange.

## Publication Google Play

Voir le guide complet : **[PUBLISH.md](PUBLISH.md)**

Workflows disponibles après configuration du keystore :
- **VTT Explorer — Release signé** → AAB + APK
- **VTT Explorer — Publier sur Google Play** → upload piste internal
