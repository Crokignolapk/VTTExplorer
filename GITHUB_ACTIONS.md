# Compiler VTT Explorer avec GitHub Actions

Build **gratuit** dans le cloud GitHub (pas besoin d’Android Studio ni de PC puissant).

Fichiers déjà dans le projet :
- `.github/workflows/build-apk.yml` → **APK debug**
- `.github/workflows/build-release.yml` → **release signé** (optionnel)

---

## Marche à suivre

### 1. Compte GitHub
https://github.com/signup

### 2. Créer le dépôt
https://github.com/new  
Nom : `VTTExplorer` — sans cocher README.

### 3. Envoyer le code

**PC :**
```bash
cd VTTExplorer
git init
git add .
git commit -m "VTT Explorer"
git branch -M main
git remote add origin https://github.com/VOTRE_USER/VTTExplorer.git
git push -u origin main
```

**Téléphone :** sur la page du dépôt → **Add file** → **Upload files** → tout le dossier (dont `.github/`).

### 4. Lancer le build
1. Ouvrir : `https://github.com/VOTRE_USER/VTTExplorer/actions`
2. Workflow **Build APK**
3. **Run workflow** → branche `main` → **Run workflow**
4. Attendre 10–20 min (point vert)

Le push sur `main` lance aussi le build automatiquement.

### 5. Télécharger l’APK
1. Cliquer sur le run terminé
2. En bas : **Artifacts** → **VTTExplorer-debug**
3. Télécharger le zip → extraire → `app-debug.apk`
4. Ouvrir l’APK sur le téléphone pour installer

---

## Secrets optionnels

**Settings** du dépôt → **Secrets and variables** → **Actions** → **New repository secret**

| Secret | Usage |
|--------|--------|
| `GRAPHHOPPER_API_KEY` | Routage VTT réel |
| `KEYSTORE_BASE64` | Keystore en base64 (release) |
| `KEYSTORE_PASSWORD` | Mot de passe keystore |
| `KEY_ALIAS` | Alias de la clé |
| `KEY_PASSWORD` | Mot de passe de la clé |

Encoder le keystore :
```bash
base64 -w0 vtt-explorer.keystore > keystore.b64
# coller le contenu dans le secret KEYSTORE_BASE64
```

---

## Liens

| Action | URL |
|--------|-----|
| Nouveau dépôt | https://github.com/new |
| Actions du dépôt | https://github.com/VOTRE_USER/VTTExplorer/actions |
| Secrets | https://github.com/VOTRE_USER/VTTExplorer/settings/secrets/actions |
| Doc GitHub Actions | https://docs.github.com/actions |

---

## Dépannage

| Problème | Solution |
|----------|----------|
| Pas d’onglet Actions | Activer Actions dans Settings → Actions → General |
| Workflow absent | Vérifier que `.github/workflows/build-apk.yml` est bien poussé |
| Build rouge | Ouvrir le job → logs → chercher `FAILURE` |
| Artifact vide | Le step Build a échoué |
| Quota | Compte free : généreux pour des builds publics ; limiter les relances |

---

## Comparaison Codemagic

Les deux sont configurés. **GitHub Actions** = un seul compte (GitHub).  
**Codemagic** = compte séparé + 500 min/mois. Voir `CODEMAGIC.md` et `PUBLISH.md`.
