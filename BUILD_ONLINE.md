# Compiler VTT Explorer en ligne (sans PC puissant)

Ce projet est une app **native Kotlin + Jetpack Compose**.  
Les sites « web → APK » ne conviennent pas.  
La méthode gratuite adaptée : **GitHub Actions** (build dans le cloud).

---

## Méthode recommandée : GitHub Actions (gratuit)

### 1. Compte GitHub
Créez un compte gratuit sur https://github.com si besoin.

### 2. Créer un dépôt
1. Sur GitHub : **New repository**
2. Nom : `VTTExplorer` (public ou privé)
3. **Ne cochez pas** “Add README” (le projet existe déjà)

### 3. Envoyer le code

Sur un téléphone (avec une app Git comme MGit / Termux) ou un PC :

```bash
cd VTTExplorer
git init
git add .
git commit -m "VTT Explorer initial"
git branch -M main
git remote add origin https://github.com/VOTRE_USER/VTTExplorer.git
git push -u origin main
```

Ou uploader le dossier via l’interface web GitHub (Add file → Upload files).

### 4. Lancer le build
1. Onglet **Actions** du dépôt
2. Workflow **Build APK**
3. **Run workflow** → Run

Attendez 5–15 minutes (première fois plus long : téléchargement des dépendances).

### 5. Télécharger l’APK
1. Quand le workflow est vert (✓)
2. Cliquez sur le run
3. Section **Artifacts** → **VTTExplorer-debug**
4. Téléchargez le zip → dedans : `app-debug.apk`

### 6. Installer sur le téléphone
- Ouvrir le fichier `.apk`
- Autoriser l’installation depuis sources inconnues si demandé

---

## Optionnel : clé GraphHopper

1. GitHub → Settings du dépôt → **Secrets and variables** → Actions
2. New secret : `GRAPHHOPPER_API_KEY` = votre clé
3. Relancer le workflow

Sans clé, l’app utilise le mode fallback (boucles locales).

---

## Alternative : Codemagic

1. Compte sur https://codemagic.io (plan free individuel)
2. Connecter le même dépôt GitHub
3. Workflow Android natif → `./gradlew assembleDebug`
4. Télécharger l’artefact APK

---

## Ce qui ne fonctionne PAS pour ce projet

| Service | Pourquoi non |
|---------|----------------|
| Web → APK / AppMint / RigForge | WebView seulement, pas de Kotlin/Compose |
| Fastshot / builders AI React Native | Autre stack (pas notre code) |
| Compilateurs Java en ligne | Pas de SDK Android / Gradle |
| Build dans cet environnement Grok | RAM insuffisante (1,2 Go) |

---

## Résumé

| Étape | Où |
|-------|-----|
| Code source | Dossier `VTTExplorer` |
| Build cloud | GitHub Actions (fichier `.github/workflows/build-apk.yml`) |
| APK | Artifact du workflow → `app-debug.apk` |
| Installation | Ouvrir l’APK sur le smartphone |
