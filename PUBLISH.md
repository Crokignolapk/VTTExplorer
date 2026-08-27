# Publication de VTT Explorer

Ce guide couvre :
1. APK debug (test)
2. Signature release (keystore)
3. Build signé sur Codemagic
4. Publication sur **Google Play**

---

## Vue d’ensemble

| Étape | Workflow Codemagic | Résultat |
|-------|--------------------|----------|
| Test personnel | `VTT Explorer — APK Debug` | `app-debug.apk` |
| Distribution / Play manuel | `VTT Explorer — Release signé` | `.aab` + `.apk` signés |
| Publication auto Play | `VTT Explorer — Publier sur Google Play` | Upload piste **internal** |

---

## 1. Keystore de signature (une seule fois)

Google Play exige que **toutes** les versions soient signées avec le **même** keystore.

### Créer le keystore (sur un PC ou Termux)

```bash
keytool -genkey -v \
  -keystore vtt-explorer.keystore \
  -alias vtt \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Retenez :
- mot de passe du keystore
- alias (`vtt`)
- mot de passe de la clé

**Sauvegardez le fichier `.keystore` hors du dépôt Git** (clé USB, coffre-fort).  
S’il est perdu, vous ne pourrez plus mettre à jour l’app sur le Play Store.

### Upload dans Codemagic

1. Codemagic → votre app → **Code signing identities** (ou Team settings)  
2. **Android keystores** → **Add keystore**  
3. Upload `vtt-explorer.keystore`  
4. Renseigner password, alias, key password  
5. **Reference name** : `vtt_keystore`  
   (doit correspondre à `android_signing: - vtt_keystore` dans `codemagic.yaml`)

---

## 2. Build release signé (sans publication auto)

1. Workflow **VTT Explorer — Release signé**  
2. **Start new build**  
3. Artifacts :
   - `app-release.aab` → format **obligatoire** pour Google Play
   - `app-release.apk` → installation directe / tests
   - `mapping.txt` → utile pour déchiffrer les crashes (ProGuard)

---

## 3. Google Play Console — préparation

### 3.1 Créer l’application

1. https://play.google.com/console  
2. **Créer une application**  
3. Nom : **VTT Explorer**  
4. Langue, type (application), gratuit/payant  
5. Accepter les déclarations

### 3.2 Remplir la fiche (obligatoire avant publication)

- Description courte / longue  
- Captures d’écran (téléphone)  
- Icône 512×512  
- Catégorie (ex. Santé et forme / Cartes)  
- Politique de confidentialité (URL)  
- Questionnaire contenu / confidentialité (localisation GPS)

### 3.3 Première version manuelle

Google exige souvent d’**uploader la première AAB à la main** :

1. Codemagic → build **Release signé** → télécharger le `.aab`  
2. Play Console → **Production** ou **Tests internes** → **Créer une version**  
3. Upload du `.aab`  
4. Notes de version → **Enregistrer**

Ensuite, les builds Codemagic `vtt-explorer-play` pourront publier automatiquement.

---

## 4. Compte de service Google Play (API)

Pour la publication automatique via Codemagic :

### 4.1 Google Cloud

1. https://console.cloud.google.com  
2. Projet lié au compte Play (ou nouveau)  
3. **APIs & Services** → activer **Google Play Android Developer API**

### 4.2 Compte de service

1. **IAM** → **Comptes de service** → **Créer**  
2. Nom : `codemagic-play`  
3. **Créer une clé** → JSON → télécharger le fichier  
4. **Ne pas committer** ce JSON

### 4.3 Lier au Play Console

1. Play Console → **Paramètres** → **Accès à l’API**  
2. **Lier** le projet Cloud si besoin  
3. **Comptes de service** → inviter l’email du compte de service  
4. Droits : **Accès pour publier** (versions, tests internes au minimum)

### 4.4 Variable Codemagic

1. Codemagic → app → **Environment variables**  
2. Nom : `GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIALS`  
3. Valeur : **contenu entier** du fichier JSON  
4. Secure : oui  
5. Groupe : `google_play`  
   (doit correspondre à `groups: - google_play` dans le yaml)

---

## 5. Publication automatique

1. Workflow **VTT Explorer — Publier sur Google Play**  
2. **Start new build**  
3. Codemagic :
   - compile l’AAB signé  
   - incrémente le `versionCode`  
   - upload sur la piste **internal** (tests internes)  
4. Play Console → **Tests internes** → vérifier la version → éventuelle promotion vers Production

### Pistes Google Play

| Track dans le yaml | Usage |
|--------------------|--------|
| `internal` | Jusqu’à 100 testeurs internes (rapide) |
| `alpha` / `beta` | Tests ouverts / fermés |
| `production` | Public |

Pour passer en production plus tard, dans `codemagic.yaml` :

```yaml
google_play:
  credentials: $GOOGLE_PLAY_SERVICE_ACCOUNT_CREDENTIALS
  track: production
  submit_as_draft: false
```

---

## 6. Checklist avant publication publique

- [ ] Keystore sauvegardé hors Git  
- [ ] AAB signé testé (install via internal track)  
- [ ] Politique de confidentialité (GPS, traces locales)  
- [ ] Texte Store + captures  
- [ ] Permission localisation justifiée dans le formulaire Play  
- [ ] Avertissement praticabilité des chemins (déjà dans l’app)  
- [ ] `targetSdk` récent (35)  
- [ ] Pas de clé API secrète dans le code source  

---

## 7. Installation locale d’un AAB (test)

Les AAB ne s’installent pas directement. Options :

```bash
# Via bundletool (PC)
bundletool build-apks --bundle=app-release.aab --output=app.apks
bundletool install-apks --apks=app.apks
```

Ou utiliser l’**APK release** généré en parallèle par le workflow Release.

---

## 8. Fichiers concernés dans le projet

| Fichier | Rôle |
|---------|------|
| `codemagic.yaml` | Workflows debug / release / Play |
| `app/build.gradle.kts` | `signingConfigs` + `versionCode` CI |
| `CODEMAGIC.md` | Prise en main Codemagic |
| `PUBLISH.md` | Ce guide publication |

---

## Rappel sécurité

- Jamais de keystore ni de JSON Google dans le dépôt Git  
- `keystore.properties` et `*.keystore` sont à ajouter au `.gitignore`  
- La première version Play se fait souvent **manuellement** ; les suivantes peuvent être automatisées
