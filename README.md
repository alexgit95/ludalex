# LUDALEX

Application web (PWA) **mono-utilisateur** de gestion de ludothèque personnelle : jeux, prêts, parties, statistiques et écran d'affichage. Les joueurs sont de simples fiches déclarées dans l'application, sans compte. Cible de déploiement : Raspberry Pi.

Les spécifications détaillées sont dans le dossier `openspec/` ; le plan des changes est dans `docs/plan-des-changes.md`.

## État d'avancement

Version courante : **4.0.0**. Disponible : profils, sécurité, interface de base, PWA, conteneurisation, images avec copies locales automatiques et suivi admin, joueurs, ludothèque, parties, statistiques par jeu et tableau de bord global, réglages, parties en direct, écran Affichage, sauvegarde/restauration import MyLudo et charte graphique heroic fantasy.

## Parties en direct

L'écran **Parties > Partie en direct** (`/parties/direct`) suit une partie pendant qu'elle se joue.

- **Lancer** : choisir le jeu et les joueurs (équipes facultatives). Un lien/QR de fin de partie est alors généré.
- **Une seule partie à la fois** : lancer une seconde partie demande d'abord de terminer la précédente (gagnants, scores facultatifs ou victoire/défaite). Si le lancement est invalide, la partie précédente reste inchangée.
- **Pause** : indiquer à quel joueur c'était de jouer ; **Reprendre** relance la partie et génère un nouveau lien.
- **Terminer** : désigner le ou les gagnants (ou une équipe), scores en option ; pour un jeu coopératif, victoire ou défaite.

### Lien de fin de partie (`/fin/{token}`)

N'importe quel appareil peut terminer ou mettre en pause la partie via ce lien, **sans authentification Cloudflare**. Le token est la seule autorisation : aléatoire (256 bits), stocké haché, à usage unique, valable 7 jours, limité à sa partie, et il ne révèle rien d'autre que le jeu et les joueurs de cette partie. Un lien inconnu, expiré ou déjà utilisé donne toujours la même page neutre. Le nombre de requêtes par adresse est limité (30 par minute).

Il faut une règle Cloudflare Access de type Bypass limitée à `/fin/*` : voir `docs/fin-de-partie-qr.md`. Le QR code qui encode ce lien est affiché par l'écran Affichage (ci-dessous).

## Écran Affichage

`/affichage` est conçu pour tourner en continu sur une télé ou une tablette (plein écran, lisible de loin). Il se met à jour tout seul (interrogation légère toutes les 3 s) :

- **Partie en cours** : l'image du jeu sert de fond (floutée, assombrie) sur la couleur de fond du jeu ; titre du jeu en haut, QR code de fin de partie en coin, joueurs en grand sur toute la hauteur restante (jamais rognés). Un cartouche **VS** (épées croisées, pulsation lente, désactivée si le navigateur demande un mouvement réduit) matérialise l'affrontement : duel face à face, VS entre chaque paire de joueurs à partir de trois, parties par équipes en colonnes nommées séparées par le VS (joueurs sans équipe dans un groupe à part), libellé « Ensemble » pour un jeu coopératif, aucun VS pour un joueur seul.
- **Partie en pause** : le joueur à qui c'est de jouer, en grand, sans QR code, sur le même fond.
- **Victoire** : animation du ou des vainqueurs pendant 15 s après la fin de la partie (message de défaite pour une défaite coopérative), puis retour à la veille.
- **Veille** : diaporama en fondu d'illustrations (château, village, nœud celtique, menhirs). Pour en ajouter, déposer des images (`svg`, `png`, `jpg`, `webp`, 2 Mo maximum) dans `src/main/resources/static/diaporama/` et reconstruire ; sans image, l'écran affiche LUDALEX.

Le QR change à chaque ouverture ou rechargement de la page (un QR scanné avant le rechargement n'est plus valable). L'écran demande au navigateur de rester allumé lorsque c'est possible.

La session Cloudflare Access dure un mois : à l'expiration, après trois échecs de mise à jour, la page se recharge et Cloudflare redemande la connexion ; une intervention humaine est donc nécessaire une fois par mois.

Réglages : `ludalex.affichage.url-publique` (origine du QR, déduite de la requête par défaut ; utile en `dev` pour qu'un téléphone joigne le poste), `ludalex.affichage.duree-victoire` (`15s`), `ludalex.affichage.renouvellement-token` (`24h`).

## Sauvegarde et restauration

L'écran **Admin > Sauvegarde et restauration** (`/admin/sauvegarde`) permet de ne rien perdre si la carte SD du Raspberry Pi meurt ou si le moteur de base change.

- **Sauvegarde** : « Télécharger la sauvegarde » fournit un seul fichier `ludalex-sauvegarde-<date>.zip` avec les données (joueurs, jeux, prêts, parties, participations, équipes, réglages), les images uploadées et les copies disponibles des images par URL avec leurs associations. Les copies partagées sont incluses une seule fois ; les URL sans copie restent exportées. Les tokens de fin de partie, la configuration et les erreurs techniques de collecte ne sont pas sauvegardés. Le fichier est écrit en flux (lecture par lots de 500 enregistrements), sans charger la base en mémoire.
- **Restauration** : on ré-uploade le ZIP **tel quel**, sans le modifier. Elle **remplace tout** (données et images) et n'est lancée qu'après saisie du mot `RESTAURER` et avec un jeton CSRF valide. Une sauvegarde faite en `dev` (SQLite) se restaure en `prod` (PostgreSQL) et inversement. Les archives v1 à v3 restent acceptées (les réglages reviennent alors aux valeurs par défaut) ; les nouvelles archives v4 nécessitent LUDALEX 3.0.0 ou ultérieur. Les copies absentes sont automatiquement planifiées ; les copies restaurées ne sont pas retéléchargées. Gardez la sauvegarde et le volume d'avant mise à jour pour revenir à une ancienne version. Les liens de fin de partie existants deviennent invalides.
- **Sûreté** : le ZIP est entièrement validé avant la moindre suppression (chemins dangereux ou « zip slip », entrées hors `manifest.json`, `donnees/`, `images/`, nombre d'entrées, taille et taux de décompression, images réelles PNG/JPEG/GIF/WebP de 2 Mo maximum, version du format, références cohérentes, compteurs du manifeste). Le remplacement des données est une seule transaction : en cas d'échec, données et images précédentes sont conservées. Une seule restauration à la fois.
- **Limites** : le ZIP est reçu comme corps brut de la requête (pas en multipart) et écrit dans un fichier temporaire. Réglages : `ludalex.sauvegarde.taille-max` (512MB), `ludalex.sauvegarde.taille-decompressee-max` (1GB), `ludalex.sauvegarde.entrees-max` (10000), `ludalex.sauvegarde.ratio-max` (200) et `ludalex.sauvegarde.repertoire-temporaire` (répertoire temporaire du système par défaut, variable Docker `LUDALEX_SAUVEGARDE_REPERTOIRE_TEMPORAIRE`). Derrière Cloudflare, l'envoi est limité par l'offre (100 Mo en gratuit).

### Format de sauvegarde (version 4)

```
manifest.json            formatVersion, appVersion, date, compteurs par type
donnees/joueurs.json     un tableau JSON par type : joueurs, jeux, parties, equipes, participations, prets, reglages
images/<nom>.<ext>       uploads et copies disponibles référencés par les données
```

`donnees/reglages.json` (obligatoire à partir de la v4) contient les paires `{cle, valeur}` de la table `reglage` : tout réglage présent en base, y compris ajouté plus tard, est exporté sans changer le format. Une clé inconnue, une valeur invalide ou un compteur incohérent font refuser l'archive avant toute suppression. Les dates sont en ISO-8601, les décimaux en chaînes, les images par `{type: UPLOAD|URL, reference, copie}` : `copie` est le nom facultatif du fichier de secours pour une source URL. Les associations de `copie_image` sont intégrées aux images, pas à un tableau de travaux techniques. `jeux.json` inclut l'indicateur `plusieursVariantes`; `parties.json` inclut le libellé facultatif `variante`. Les identifiants d'origine (`idSauvegarde`) ne servent que de références : de nouveaux identifiants sont générés à la restauration, ce qui rend le format indépendant du moteur. Les archives v1/v2 sans les nouvelles propriétés sont restaurées avec les valeurs par défaut. **Règle d'évolution** : toute nouvelle entité ou colonne doit être ajoutée au format (un test vérifie que chaque entité JPA est exportée) ; un changement incompatible incrémente `formatVersion`, la restauration refuse toute version inconnue, et une évolution du format est un breaking change documenté dans le changelog.

## Import MyLudo

L'écran **Admin > Import MyLudo** (`/admin/import-myludo`) reprend votre historique MyLudo : jeux possédés, joueurs et parties (date, lieu, scores, équipes, résultat coopératif) ; les statistiques se recalculent ensuite depuis les parties.

1. **Fichiers** : envoyer l'export de la **collection** et/ou celui des **parties** (JSON, 2 Mo maximum chacun). Exemples anonymisés : `docs/exemple-my-ludo/`.
2. **Analyse** (rien n'est écrit) : nombre de jeux et de parties à ajouter, déjà présentes ou ignorées (avec la raison), avertissements, et **correspondance des joueurs** : chaque nom de l'export est associé par défaut à la fiche de même nom (casse, accents et espaces ignorés), au choix à une autre fiche ou à un nouveau joueur.
3. **Import** avec barre de progression (phase, `n sur total`, élément en cours), puis un message de réussite avec les compteurs, ou un message d'erreur précis : fichier, numéro d'entrée, jeu et date, champ, cause, et ligne/colonne d'un JSON mal formé. En cas d'erreur, rien n'est enregistré (une seule transaction).

**Delta seulement, rien n'est écrasé.** Une partie du fichier est considérée comme déjà présente si, pour le même jeu et le même jour, une partie en base partage au moins un joueur avec elle ; chaque partie en base ne « couvre » qu'une seule partie du fichier, si bien que plusieurs manches identiques le même jour sont conservées. Réimporter le même fichier n'ajoute rien, un export plus récent n'ajoute que les nouvelles parties. Les parties existantes ne sont jamais modifiées ; un jeu existant n'est pas modifié (seuls son prix et sa date d'achat vides sont complétés).

**Conversion** : un jeu est coopératif si une de ses parties l'est (ou par le mécanisme « Coopération ») ; une partie coopérative devient une victoire ou une défaite ; scores tous nuls = pas de score ; deux joueurs de même nom dans une partie (« Anonyme ») deviennent « Anonyme » et « Anonyme 2 » ; un prix de 0 est considéré inconnu. **Parties ignorées** (listées avec leur raison) : exclues dans MyLudo, datées dans le futur, sans joueur, ou sans gagnant dans un jeu non coopératif. **Non importés** : durée, commentaire, notes, images et métadonnées des jeux (éditeurs, thèmes...).

L'analyse ou le rapport est conservé en mémoire 30 minutes (`ludalex.import-myludo.duree-session`), les listes détaillées sont limitées à 200 lignes (`ludalex.import-myludo.lignes-max`). Une seule analyse ou un seul import à la fois.

## Charte graphique

Thème « heroic fantasy » sobre, sur toute l'application (y compris l'écran Affichage) : fond crème et parchemin très discret, petits lierres en filigrane (coin de l'en-tête, séparateurs de titres, coins des cartes), titres en police à empattements et texte en sans-serif pour rester très lisible. Aucune police ni ressource externe : polices du système uniquement.

- Les couleurs sont des variables CSS au début de `static/css/ludalex.css` (`:root`) ; `affichage.css` les réutilise. Les contrastes texte/fond respectent WCAG AA (vérifié par `CharteGraphiqueTest`).
- **Menu burger** : sur petit écran (< 40 rem) avec JavaScript, la navigation est repliée derrière un bouton (icône en trois lianes) qui ouvre un panneau parchemin sous l'en-tête ; fermeture au clic extérieur ou sur Échap ; sans JavaScript, la navigation reste affichée.
- Les décors sont de petits SVG dans `static/decor/` (`parchemin.svg`, `lierre-coin.svg`, `lierre-separateur.svg`) ; pour en ajouter un, le déposer dans ce dossier et le référencer par une variable `--decor-*`.
- La couleur de fond propre à chaque jeu est conservée comme cadre ; le texte repose toujours sur un cartouche clair.
- Pour changer de police, modifier `--police-titre` / `--police-texte` (une police embarquée devra être placée dans `static/`, jamais chargée depuis un CDN).

## Statistiques

### Tableau de bord (onglet Statistiques, `/statistiques`)

Vue globale en chiffres et diagrammes (barres, anneaux, calendrier de chaleur, courbes) rendus **en SVG et CSS par le serveur**, sans JavaScript ni bibliothèque supplémentaire, aux couleurs de la charte. Chaque diagramme a une alternative textuelle et un tableau de données. Seules les parties **terminées** sont comptées.

- **Période** : 30 derniers jours, 12 derniers mois (défaut), année civile ou tout l'historique ; elle s'applique à tous les blocs. Une période inconnue revient à 12 mois.
- **Chiffres clés** : parties (avec l'évolution par rapport à la période précédente de même durée), jeux joués sur le total, joueurs actifs, ludothèque (dont coopératifs), valeur et prix moyen, prêts, série de semaines consécutives avec au moins une partie.
- **Activité** : parties par mois (par jour sur 30 jours), par jour de la semaine, calendrier de chaleur, cumul, répartition par lieu.
- **Jeux** : plus joués, coopératifs les plus gagnés, jamais joués, dormants, compétitif/coopératif, taux de victoire coopératif et jeu le plus difficile, variantes, coût par partie (jeux avec prix, sur tout l'historique).
- **Joueurs** : classement par taux de victoire (nombre minimal de parties), parties par joueur, jeux différents joués, records de score par jeu, duos les plus fréquents. Les joueurs anonymisés comptent dans les totaux mais pas dans les classements.
- **Ludothèque** : achats par mois et par année (jeux avec date d'achat), investissement cumulé, jeux les plus chers, jeux sans prix ou sans date d'achat.
- **Prêts** : par mois, principaux emprunteurs, durée moyenne des prêts rendus, prêts en cours dont ceux dépassant le seuil de prêt long.
- **Hors périmètre** (données absentes) : durée des parties, heure de jeu, retards de prêt.

**Cache** : un instantané par période est conservé 10 minutes en mémoire (`ludalex.statistiques.cache-ttl`) ; tous les blocs d'un affichage proviennent du même instantané et l'heure de mise à jour est indiquée. Le cache est vidé dès qu'une donnée change (parties, jeux, prêts, joueurs, réglages, restauration, import MyLudo) : il n'y a donc pas de bouton d'actualisation. Le premier accès après expiration ou modification recalcule les chiffres. Calculs par requêtes agrégées en nombre constant, compatibles SQLite et PostgreSQL.

### Réglages (Admin > Réglages, `/admin/reglages`)

Seuils des statistiques, modifiables et rétablissables : jeu dormant (6 mois sans partie), nombre minimal de parties au classement (5), prêt long (30 jours). Ils sont stockés dans la table générique `reglage` (seules les valeurs modifiées) et inclus dans la sauvegarde et la restauration.

### Statistiques d'un jeu

La fiche d'un jeu affiche ses statistiques, calculées sur les parties **terminées** :

- **Classement** : parties jouées, victoires et taux de victoire (victoires / parties) de chaque joueur, sans minimum de parties. Jeux coopératifs et compétitifs sont traités de la même façon : une victoire coopérative compte pour chaque participant.
- **Champion** : le joueur qui a le plus de victoires (au moins une). En cas d'égalité, tous les premiers sont champions. Un joueur anonymisé reste dans le classement mais ne peut pas être champion.
- **Liste des jeux** : chaque vignette porte l'avatar du ou des champions.
- **Dix dernières parties** : date, lieu, joueurs, scores et gagnants.

## Parties

L'écran **Parties** (`/parties`) liste les parties de la plus récente à la plus ancienne (filtre par jeu) et permet d'en déclarer après coup.

- **Déclaration** : choisir le jeu (son image s'affiche dès le choix pour confirmer visuellement), cocher les joueurs, indiquer la date (aujourd'hui par défaut, jamais dans le futur), le lieu (facultatif), la campagne/enquête/version si le jeu en propose, et les scores (facultatifs).
- **Jeu compétitif** : désigner au moins un gagnant parmi les participants ; plusieurs gagnants sont possibles.
- **Jeu coopératif** : choisir « Victoire » ou « Défaite » ; une victoire compte pour chaque participant.
- **Équipes** : jusqu'à 4 équipes propres à la partie (nom facultatif) ; désigner une équipe gagnante rend gagnants tous ses membres.
- **Lieu** : texte libre ; en tapant, les lieux déjà saisis sont proposés (10 au plus).
- **Modification et suppression** : une partie peut être corrigée ou supprimée (confirmation). Un joueur anonymisé reste dans ses anciennes parties mais ne peut pas être ajouté à une nouvelle.
- Un jeu ayant des parties ne peut pas être supprimé ; un joueur ayant des parties est anonymisé au lieu d'être supprimé.

## Ludothèque

L'écran **Ludothèque** (`/jeux`) liste les jeux en vignettes (image, titre dessous, fond de la couleur du jeu), 2 par ligne sur petit écran et 3 sur grand écran. L'ouverture de `/` (logo ou lancement PWA) redirige vers cette liste; son titre et l'action « Ajouter un jeu » sont alignés sur le bord gauche de la grille centrée. Sous le titre de chaque vignette, un petit parchemin affiche la coupe, l'avatar et le nom du ou des champions (à la ligne en cas d'égalité) ; la hauteur est réservée pour les jeux sans champion.

- **Jeu** : nom (obligatoire), image (upload ou URL), prix (positif, deux décimales), date d'achat (pas dans le futur), jeu coopératif ou non, et indicateur de campagnes/enquêtes/versions. Sans image, une image par défaut est affichée.
- **Couleur de fond** : calculée à l'enregistrement depuis l'image ; « Choisir la couleur manuellement » permet de la corriger. Elle est conservée tant que l'image ne change pas.
- **Fiche** : date d'achat, prix, type, prêt en cours et historique des prêts ; zone réservée aux statistiques de parties.
- **Prêts** : « Prêter » demande l'emprunteur (texte libre) et la date (aujourd'hui par défaut) ; « Enregistrer le retour » clot le prêt. Un jeu ne peut être prêché qu'à une personne à la fois ; l'historique est conservé, l'emprunteur courant apparaît sur la vignette.
- **Suppression** : avec confirmation ; refusée si le jeu a des parties. Elle supprime aussi l'historique des prêts et l'image uploadée.

## Administration des joueurs

La page **Admin** (`/admin`) affiche la version du POM et les 7 premiers caractères du SHA de commit lorsqu'il est disponible. En exécution locale sans SHA, seule la version est affichée.

L'écran **Admin > Joueurs** (`/admin/joueurs`) gère les fiches de joueurs, sans compte ni lien avec Cloudflare.

- Un joueur a un **nom** (obligatoire, 100 caractères, unique sans tenir compte de la casse), un **avatar** et une **image de victoire** facultatifs (upload ou URL, voir Images).
- Sans image, une image par défaut est affichée.
- La **suppression** demande une confirmation. Un joueur qui a des parties est **anonymisé** : son nom devient « Joueur supprimé », ses images sont effacées et ses parties restent consultables. Sans partie, il est supprimé définitivement.
- Un joueur anonymisé n'est plus modifiable ni proposé pour une nouvelle partie.

## Images

Chaque image (jeu, joueur) est soit un **fichier uploadé**, soit une **URL**.

- **Upload** : JPEG, PNG, GIF ou WebP, 2 Mo maximum, type vérifié sur le contenu. Le fichier est stocké sur le volume (`ludalex.images.repertoire`, par défaut `./data/images`, `/data/images` en Docker) sous un nom généré et servi par `/images/{nom}`.
- **URL** : l'adresse d'origine reste prioritaire. Toutes les images distantes référencées (jeux, avatars et images de victoire) sont copiées préventivement sur le même volume, une fois par URL exacte, sous un nom `copie-<uuid>.<ext>`. Le navigateur essaie la copie disponible sur toute erreur de chargement de l'image (404, refus, réseau, contenu indécodable), puis l'image par défaut ; les images décoratives sont retirées seulement après les échecs. Aucun autre type de ressource n'est concerné, et aucune boucle de repli n'est possible.
- **Collecte automatique** : découverte par lots au démarrage puis toutes les minutes, nouvelles références signalées après commit, un téléchargement à la fois en flux. Le **téléchargement est en pause à chaque démarrage** : les copies sont inscrites « en attente » (sans accès réseau ni tentative consommée) et ne sont récupérées qu'après activation depuis **Admin > Copies des images distantes** (bouton Activer / Mettre en pause, sans redémarrage). La mise en pause laisse finir le téléchargement déjà commencé sans en démarrer d'autre ; l'état n'est pas mémorisé (un redémarrage remet en pause). Pour démarrer actif, définir `ludalex.images.copies.demarrage-actif=true` (variable Docker `LUDALEX_IMAGES_COPIES_DEMARRAGE_ACTIF=true`). Les copies et états survivent au redémarrage et à la recréation du conteneur avec le même volume et la même base. Les copies devenues inutiles sont nettoyées ; une copie partagée est conservée tant qu'une fiche référence son URL. Une copie dont le fichier disparaît est replanifiée.
- **Suivi** : **Admin > Copies des images distantes** (`/admin/images`) affiche l'état de la récupération (active ou en pause), les compteurs en attente/en cours/disponibles/en échec, l'espace occupé, les fiches concernées, dates et erreurs, par pages de 20, avec actualisation toutes les 5 secondes. Relance individuelle ou de tous les échecs, protégée par CSRF et Cloudflare Access en prod.
- **Tentatives et limites** : au plus trois tentatives pour une erreur transitoire, avec au moins une minute puis quinze minutes d'attente ; refus SSRF et format invalide sans relance automatique. Une URL déjà morte avant la première copie reste irrécupérable. Une URL copiée n'est pas rafraîchie automatiquement si son contenu change. Le repli attend l'événement d'erreur du navigateur : une connexion suspendue n'entraîne pas un repli immédiat. Le réglage `ludalex.images.copies.actif=false` désactive totalement le worker (notamment dans les tests), sans supprimer les copies ; la page d'administration l'indique alors sans proposer de bouton.
- **Couleur de fond** : calculée une seule fois à l'enregistrement (image réduite, couleur la plus fréquente, hors blanc, noir et transparent), stockée et modifiable à la main ; `#3a3a3a` en cas d'échec. Le téléchargement ponctuel servant au calcul est distinct de la copie en arrière-plan, qui ne modifie pas la couleur choisie.
- **Sécurité SSRF** : tout téléchargement refuse les adresses locales ou privées (y compris après redirection ou résolution DNS) et plafonne délai et taille. Une URL du réseau local peut être enregistrée, mais sa couleur reste neutre.

### Mise à jour vers 4.0.0

La récupération automatique des copies d'images démarre désormais **en pause**. Après la mise à jour, aucune image n'est téléchargée tant qu'on n'a pas cliqué sur « Activer la récupération » dans Admin > Copies des images distantes, ou défini `LUDALEX_IMAGES_COPIES_DEMARRAGE_ACTIF=true` pour retrouver l'ancien comportement. Aucun changement de base, de volume ni de format de sauvegarde.

### Mise à jour vers 3.0.0

Sauvegarder la base et le volume des images avant déploiement. Hibernate ajoute la table `reglage` et l'index des dates de parties sans outil de migration ; aucune donnée à migrer, les seuils valent leurs valeurs par défaut. Les nouveaux ZIP v4 ne sont pas lisibles par les versions antérieures : pour revenir en arrière, utiliser un ZIP v1 à v3 pris avant la mise à jour.

### Mise à jour vers 2.0.0

Sauvegarder la base et le volume des images avant déploiement. Hibernate ajoute la table `copie_image` sans outil de migration ; la collecte des URL existantes démarre automatiquement. Prévoir l'espace disque supplémentaire (2 Mo maximum par copie) et conserver le volume `/data/images` avec les données. Les anciens ZIP v1/v2 restent lisibles ; les ZIP v3 ne sont pas lisibles par les anciennes versions. Un retour arrière doit utiliser les données et le volume d'avant mise à jour, pas un ZIP v3.

## Technologies

- Java 25, Spring Boot 4.x, Spring Security, Spring Data JPA
- Thymeleaf + HTMX (servi localement), CSS maison, PWA minimale
- SQLite (dev) et PostgreSQL (prod), schéma généré par Hibernate (pas de migration)
- Docker / docker-compose

## Profils

| Profil | Usage | Base | Authentification |
|---|---|---|---|
| `dev` (défaut) | Développement local, HTTP | SQLite (`ludalex-dev.db`) | **Aucune** |
| `prod` | Docker, HTTPS terminé par Cloudflare | PostgreSQL | **Cloudflare Access obligatoire** |

### Variables d'environnement (prod)

| Variable | Rôle |
|---|---|
| `DB_URL` | URL JDBC PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | Identifiants de la base |
| `CF_ACCESS_TEAM_DOMAIN` | Domaine d'équipe Cloudflare (`xxx.cloudflareaccess.com`) |
| `CF_ACCESS_AUD` | Audience (AUD) de l'application Cloudflare Access |
| `JAVA_OPTS` | Optionnelle, options JVM (défaut : `-Xmx384m -XX:+UseSerialGC -Djava.awt.headless=true`) |
| `LUDALEX_IMAGES_REPERTOIRE` | Optionnelle, volume des images (défaut Docker : `/data/images`) |

Toutes les variables obligatoires manquantes font échouer le démarrage avec un message explicite.

## Authentification

L'application ne gère ni compte, ni mot de passe, ni rôle.

- **prod** : chaque requête doit porter un JWT Cloudflare Access valide (header `Cf-Access-Jwt-Assertion`) : signature, expiration, émetteur et audience. Sans JWT valide : 401 ; mauvaise audience : 403. Aucune identité n'est conservée.
- **dev** : tout est autorisé, sans login ni JWT. Ce mode ne peut pas être activé en `prod`.
- **Exception** : le chemin `/fin/**` (lien/QR de fin de partie) n'exige pas de JWT ; il est protégé par un token à usage unique. Voir `docs/fin-de-partie-qr.md` pour la règle Cloudflare à créer.
- Le CSRF est actif sur toutes les requêtes modifiantes.

## Lancer en local (dev)

```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn spring-boot:run
```

L'application est servie sur <http://localhost:8181>.

## Lancer avec Docker (prod)

```bash
cp .env.example .env   # puis renseigner les valeurs
docker compose up --build
```

- La JVM est limitée par `-Xmx` (Raspberry Pi) et tourne en mode headless.
- Les images uploadées seront stockées sur le volume `/data/images`.
- Cloudflare doit publier l'application derrière Access, avec la règle Bypass limitée à `/fin/*`.

### Portainer / Arcane

`docker-compose.portainer.yml` est une stack prête à coller (Portainer : Stacks > Add stack ; Arcane : Projects) qui utilise l'image Docker Hub `<DOCKERHUB_USERNAME>/ludalex` (arm64). Les deux services ont un healthcheck : PostgreSQL via `pg_isready`, l'application via une requête HTTP sur son port 8181 (sans curl, avec 120 s de démarrage toléré), visible comme « healthy » dans Portainer ou Arcane. Variables à renseigner dans l'interface : `DOCKERHUB_USERNAME`, `DB_USERNAME`, `DB_PASSWORD`, `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` ; facultatives : `LUDALEX_TAG` (défaut `latest`), `LUDALEX_PORT` (défaut `8181`), `POSTGRES_DB`, `JAVA_OPTS`.

## Tests

```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn test
```

## Documentation

- `docs/database.puml` : modèle de données (PlantUML)
- `docs/fin-de-partie-qr.md` : fin de partie par QR code et configuration Cloudflare
- `docs/exemple-my-ludo/` : exports MyLudo anonymisés servant de jeu d'essai
- `docs/plan-des-changes.md` : plan et décisions du projet
- `CHANGELOG.md` : journal des modifications
