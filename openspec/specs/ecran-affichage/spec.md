# ecran-affichage Specification

## Purpose
Écran Affichage : partie en cours, QR de fin de partie, pause, victoire et mise à jour automatique.

## Requirements

### Requirement: Affichage de la partie en cours
Le système SHALL afficher sur `/affichage`, lorsqu'une partie est en cours, une vue de confrontation en alternance avec les autres vues de la rotation. Cette vue SHALL afficher le titre du jeu, l'avatar et le nom de chaque joueur en grand, et un QR code, sur un fond reprenant l'image du jeu (floutée, sous un voile léger) et sa couleur de fond. Les joueurs SHALL toujours être entièrement visibles, sans rognage, quelle que soit la taille de l'image du jeu, et leurs avatars SHALL être dimensionnés selon la hauteur de l'écran. Un cartouche « VS » SHALL matérialiser l'affrontement selon la composition de la partie.

#### Scenario: Partie en cours
- **WHEN** la vue de confrontation d'une partie « en cours » de « Catan » avec trois joueurs est active
- **THEN** l'écran montre le titre de Catan, les trois avatars entièrement visibles, le QR code, sur un fond issu de l'image de Catan et de sa couleur

#### Scenario: Image absente
- **WHEN** le jeu n'a pas d'image
- **THEN** le fond est uniquement de la couleur du jeu et le titre du jeu est affiché en grand

#### Scenario: Échec du chargement de l'image
- **WHEN** l'image du jeu ne peut pas être chargée par le navigateur
- **THEN** le fond reste de la couleur du jeu, sans erreur visible

#### Scenario: Avatar absent
- **WHEN** un joueur n'a pas d'avatar
- **THEN** l'écran affiche l'avatar par défaut avec son nom

#### Scenario: Avatars non rognés
- **WHEN** une partie est en cours sur une tablette en paysage, avec une image de jeu de grande hauteur
- **THEN** les avatars et noms des joueurs sont entièrement visibles dans la hauteur de l'écran

#### Scenario: Duel
- **WHEN** une partie compétitive sans équipe oppose deux joueurs
- **THEN** les deux joueurs sont face à face avec le cartouche « VS » entre eux

#### Scenario: Plus de deux joueurs sans équipe
- **WHEN** une partie compétitive sans équipe réunit trois joueurs ou plus
- **THEN** les joueurs sont en rangée avec un cartouche « VS » entre chaque paire de joueurs adjacents

#### Scenario: Parties par équipes
- **WHEN** une partie compétitive est jouée par deux équipes
- **THEN** chaque équipe est affichée en colonne avec son nom et les avatars de ses membres, et un cartouche « VS » sépare les équipes

#### Scenario: Plus de deux équipes
- **WHEN** une partie est jouée par trois équipes
- **THEN** un cartouche « VS » sépare chaque paire d'équipes adjacentes

#### Scenario: Joueur sans équipe dans une partie par équipes
- **WHEN** un joueur de la partie n'est affecté à aucune équipe alors que d'autres le sont
- **THEN** il est affiché dans un groupe distinct sans nom d'équipe, et un « VS » le sépare des équipes

#### Scenario: Jeu coopératif
- **WHEN** la partie en cours est d'un jeu coopératif
- **THEN** aucun « VS » n'est affiché et un libellé « Ensemble » rassemble les joueurs

#### Scenario: Joueur seul
- **WHEN** la partie en cours ne compte qu'un seul joueur
- **THEN** aucun « VS » n'est affiché

#### Scenario: Pulsation discrète
- **WHEN** le cartouche « VS » est affiché
- **THEN** il pulse lentement et faiblement (variation d'échelle d'au plus 4 %, cycle de 2,5 secondes ou plus), en n'animant que `transform` et `opacity`

#### Scenario: Mouvement réduit
- **WHEN** le navigateur demande un mouvement réduit (`prefers-reduced-motion: reduce`)
- **THEN** le cartouche « VS » n'est pas animé

### Requirement: QR code de fin de partie
Le système SHALL afficher sur chaque vue de la rotation d'une partie en cours un QR code encodant l'URL `/fin/{token}` de l'origine publique de la requête, généré côté serveur ; le token en clair n'est jamais stocké en base (il n'est conservé qu'en mémoire du processus pour être réutilisé tant qu'il est valide). L'origine peut être imposée par `ludalex.affichage.url-publique`.

#### Scenario: QR code valide
- **WHEN** l'écran affiche une vue de la rotation d'une partie en cours
- **THEN** le QR code visible encode `https://<domaine>/fin/<token>` avec un token valide non utilisé de cette partie

#### Scenario: Nouveau token à l'affichage
- **WHEN** la page est ouverte ou rechargée
- **THEN** un nouveau token est généré et l'ancien token non utilisé est invalidé

#### Scenario: Renouvellement avant expiration
- **WHEN** le token a moins de 24 heures de validité restante
- **THEN** le système en génère un nouveau lors de la mise à jour suivante de l'écran

#### Scenario: Origine publique
- **WHEN** l'écran est servi derrière Cloudflare avec les headers `X-Forwarded-*`
- **THEN** l'URL du QR utilise le schéma et le domaine publics, pas l'adresse interne

### Requirement: Partie en pause
Le système SHALL afficher, lorsque la partie est en pause, en grand le joueur à qui c'est de jouer (avatar et nom) avec la mention de la reprise, sur le même fond que la partie en cours (image du jeu et couleur de fond).

#### Scenario: Pause
- **WHEN** la partie est en pause avec Marie comme joueur suivant
- **THEN** l'écran affiche l'avatar de Marie en grand avec son nom

#### Scenario: Pas de QR pendant la pause
- **WHEN** la partie est en pause
- **THEN** aucun QR code n'est affiché (le token a été consommé)

#### Scenario: Fond en pause
- **WHEN** la partie d'un jeu doté d'une image est en pause
- **THEN** le fond reprend l'image et la couleur du jeu

### Requirement: Animation de victoire
Lorsqu'une partie se termine alors que l'écran est ouvert, le système SHALL déclencher une animation sur l'avatar et l'image de victoire de chaque vainqueur pendant environ 15 secondes, puis revenir à la veille.

#### Scenario: Fin par scan du QR
- **WHEN** un invité termine la partie via le QR en désignant Marie
- **THEN** l'écran, dans les secondes qui suivent, anime l'avatar et l'image de victoire de Marie

#### Scenario: Plusieurs vainqueurs
- **WHEN** deux joueurs gagnent
- **THEN** les deux avatars sont animés

#### Scenario: Défaite coopérative
- **WHEN** une partie coopérative est perdue
- **THEN** l'écran affiche un message de défaite sans animation de victoire

#### Scenario: Écran ouvert après coup
- **WHEN** l'écran est ouvert plus de 15 secondes après la fin d'une partie
- **THEN** il n'affiche pas l'animation

### Requirement: Mise à jour automatique sobre
L'écran SHALL se mettre à jour par interrogation du serveur toutes les 3 secondes, avec une réponse légère, sans recharger la page entière.

#### Scenario: Nouvelle partie lancée
- **WHEN** une partie est lancée pendant que l'écran est en veille
- **THEN** l'écran passe à l'affichage de la partie en moins de 5 secondes

#### Scenario: Charge serveur
- **WHEN** l'écran reste ouvert une journée
- **THEN** les requêtes de mise à jour ne chargent aucune collection volumineuse

### Requirement: Reprise après expiration de la session Cloudflare
Si les mises à jour échouent de façon répétée (session Cloudflare expirée, réseau coupé), l'écran SHALL recharger la page entière afin que Cloudflare Access redemande la connexion, puis reprendre normalement.

#### Scenario: Session expirée
- **WHEN** trois mises à jour consécutives échouent
- **THEN** la page est rechargée en entier

#### Scenario: Retour du réseau
- **WHEN** le réseau revient après une coupure
- **THEN** l'écran reprend ses mises à jour sans intervention

### Requirement: Écran maintenu allumé
L'écran SHALL demander au navigateur de ne pas se mettre en veille lorsque l'API correspondante est disponible.

#### Scenario: API indisponible
- **WHEN** le navigateur ne propose pas l'API de maintien d'écran
- **THEN** l'affichage fonctionne sans erreur

### Requirement: Rotation pendant une partie
Pendant une partie en cours, l'écran SHALL alterner toutes les dix secondes les vues confrontation, historique du jeu en cours avec champions et historique tous jeux, en intercalant une image adaptée du diaporama entre chaque vue. La rotation SHALL commencer par la confrontation. Une vue d'historique vide SHALL être sautée et le diaporama SHALL avancer à chaque passage. Le QR SHALL rester visible et lisible sur toutes les vues, y compris le fond neutre.

#### Scenario: Rotation complète
- **WHEN** les deux historiques contiennent des parties terminées
- **THEN** la séquence est confrontation, image, historique du jeu, image, historique global, image, puis confrontation, avec dix secondes par vue

#### Scenario: Historique du jeu vide
- **WHEN** aucune partie terminée n'existe pour le jeu en cours
- **THEN** la vue d'historique du jeu est sautée sans écran vide

#### Scenario: Aucun historique
- **WHEN** aucune partie terminée n'existe
- **THEN** la rotation alterne confrontation et diaporama sans vue d'historique vide

#### Scenario: Partie mise en pause ou terminée
- **WHEN** la partie est mise en pause ou terminée pendant une vue de la rotation
- **THEN** la rotation s'interrompt et l'écran affiche l'état de pause ou de victoire selon les règles existantes

#### Scenario: Rafraîchissement sans changement
- **WHEN** une interrogation périodique ne révèle pas de changement pertinent
- **THEN** la rotation et le QR ne sont pas réinitialisés

### Requirement: Historiques et champions sur l'écran
La vue du jeu en cours SHALL afficher les parties terminées de ce jeu, de la plus récente à la plus ancienne selon la date puis l'identifiant : au plus trois en portrait et au plus cinq en paysage. Elle SHALL présenter leur date, leurs joueurs et gagnants, ou le résultat victoire/défaite pour une partie coopérative. En portrait ou fenêtre carrée, elle SHALL disposer verticalement le podium, les parties et le QR de fin, dans cet ordre. En paysage, elle SHALL conserver la présentation existante. La vue du jeu SHALL afficher en grand le nom et l'avatar des champions visibles selon la limite du podium, ainsi que le nombre d'autres champions ex æquo ; elle ne SHALL désigner aucun champion lorsqu'il n'y en a pas. Les informations SHALL rester lisibles et être actualisées après modification des données sans rechargement manuel.

#### Scenario: Champion et parties du jeu
- **WHEN** le jeu en cours a un champion et des parties terminées
- **THEN** la vue affiche ce champion en grand et au plus trois parties en portrait ou cinq en paysage

#### Scenario: Vue du jeu en portrait
- **WHEN** la fenêtre est en portrait ou carrée et que la vue du jeu est affichée
- **THEN** le podium occupe seul la zone supérieure, au-dessus des trois dernières parties au maximum, elles-mêmes au-dessus du QR de fin, sans chevauchement ni contenu masqué

#### Scenario: Vue du jeu en paysage
- **WHEN** la fenêtre est en paysage et que la vue du jeu est affichée
- **THEN** la disposition existante est conservée et au plus cinq dernières parties sont visibles

#### Scenario: Champions à égalité
- **WHEN** deux joueurs non anonymisés sont champions à égalité
- **THEN** chacun apparaît comme champion dans la vue du jeu

#### Scenario: Aucune victoire
- **WHEN** le jeu possède des parties terminées mais aucun champion éligible
- **THEN** la vue affiche les parties sans champion

#### Scenario: Résultat coopératif
- **WHEN** une partie récente concerne un jeu coopératif
- **THEN** son résultat indique victoire ou défaite collective

#### Scenario: Mise à jour de l'historique
- **WHEN** une partie terminée ou les données influençant le champion sont ajoutées, modifiées ou supprimées pendant l'affichage
- **THEN** les vues concernées sont actualisées sans rechargement manuel et les requêtes restent bornées

### Requirement: Podium des joueurs du jeu
La vue de l'historique du jeu en cours SHALL présenter un podium des trois premiers rangs de joueurs non anonymisés, classés selon leur nombre de victoires dans les parties terminées de ce jeu. Les joueurs ayant le même nombre de victoires SHALL partager le même rang ; les trois rangs correspondent aux trois nombres distincts de victoires les plus élevés. Chaque rang SHALL afficher au plus trois joueurs, triés par nom sans tenir compte de la casse puis par identifiant, avec leurs noms et avatars, et SHALL indiquer le nombre d'autres joueurs ex æquo non affichés. Chaque score SHALL être affiché au format compact `nV` avec une alternative accessible explicite ; le premier rang SHALL être entouré de lauriers, y compris en cas d'ex æquo. Les joueurs sans victoire peuvent figurer au podium, sans être désignés champions. Le podium SHALL apparaître dans les cinq premières secondes de la vue, du troisième rang au premier, et rester visible jusqu'à la fin des dix secondes ; le QR et les résultats SHALL rester lisibles.

#### Scenario: Trois rangs
- **WHEN** la vue du jeu apparaît avec au moins trois rangs de joueurs éligibles
- **THEN** les joueurs du troisième rang apparaissent, puis ceux du deuxième, puis ceux du premier, et le podium complet est visible au plus tard cinq secondes après l'apparition de la vue

#### Scenario: Égalité sur un rang
- **WHEN** deux joueurs éligibles ont le même nombre de victoires parmi les trois premiers rangs
- **THEN** ils apparaissent tous deux sur le même rang

#### Scenario: Plus de trois joueurs ex æquo
- **WHEN** dix joueurs éligibles sont ex æquo sur un rang du podium
- **THEN** les trois premiers par nom puis identifiant sont affichés sur ce rang et le nombre de sept autres ex æquo est indiqué

#### Scenario: Moins de trois rangs
- **WHEN** moins de trois nombres distincts de victoires existent parmi les joueurs éligibles
- **THEN** seuls les rangs disponibles sont affichés, révélés du moins élevé au plus élevé dans les cinq premières secondes

#### Scenario: Aucun joueur éligible
- **WHEN** aucun joueur non anonymisé n'a participé à une partie terminée du jeu
- **THEN** aucun podium ni champion n'est affiché et les dernières parties restent visibles

#### Scenario: Mouvement réduit
- **WHEN** le navigateur demande un mouvement réduit (`prefers-reduced-motion: reduce`)
- **THEN** le podium complet est affiché immédiatement sans animation et la vue dure toujours dix secondes

#### Scenario: Retour sur la vue
- **WHEN** la rotation revient à la vue du jeu
- **THEN** la révélation du podium reprend pour cette nouvelle apparition, sauf si le mouvement réduit est demandé

#### Scenario: Compteur et lauriers
- **WHEN** le premier rang compte quatre victoires et plusieurs joueurs ex æquo
- **THEN** le rang affiche `4V` et des lauriers entourent la première place partagée

### Requirement: Galerie des dernières parties
En veille comme pendant une partie, l'écran SHALL présenter la même galerie originale, sans liste de participants, pour au plus cinq parties terminées, ordonnées par date de partie décroissante puis identifiant décroissant. Un même jeu MAY apparaître plusieurs fois. Chaque résultat SHALL montrer le nom et l'image du jeu, la date et les noms et avatars de ses gagnants ; une victoire ou une défaite coopérative SHALL être identifiée explicitement, sans inventer de gagnant en cas de défaite. La galerie SHALL être lisible en portrait et paysage et respecter les replis d'image existants.

#### Scenario: Plusieurs parties du même jeu
- **WHEN** deux des cinq dernières parties concernent le même jeu
- **THEN** chacune apparaît séparément avec son propre résultat

#### Scenario: Plusieurs gagnants
- **WHEN** une partie a plusieurs gagnants, individuellement ou par équipe
- **THEN** tous les gagnants sont représentés sans mettre en avant les autres participants

#### Scenario: Défaite coopérative
- **WHEN** une partie coopérative est perdue
- **THEN** son médaillon indique une défaite collective sans afficher de faux vainqueur

#### Scenario: Galerie dans les deux états
- **WHEN** les mêmes parties sont affichées en veille et pendant une partie
- **THEN** les mêmes résultats sont présentés selon la même composition adaptée à l'orientation

### Requirement: Lancement depuis la veille
Le QR de veille SHALL encoder l'origine publique suivie de `/parties/direct` et ouvrir le formulaire de lancement sans déclencher de commande. Il SHALL disposer d'un libellé accessible qui le distingue du QR de fin. En production, son accès SHALL rester soumis à Cloudflare Access et le lancement SHALL rester protégé contre la CSRF ; le mode local de développement reste sans authentification.

#### Scenario: Scan du QR de lancement
- **WHEN** un appareil scanne le QR visible en veille
- **THEN** il ouvre `/parties/direct` sur l'origine publique sans démarrer automatiquement de partie

#### Scenario: Accès non autorisé en production
- **WHEN** un appareil sans authentification Cloudflare valide suit le lien du QR en production
- **THEN** il ne peut pas accéder au formulaire ni lancer une partie

#### Scenario: Commande sans CSRF
- **WHEN** une commande de lancement est envoyée sans jeton CSRF valide
- **THEN** elle est refusée

#### Scenario: Accès local
- **WHEN** le QR est scanné en développement depuis un appareil capable d'atteindre l'origine configurée
- **THEN** le formulaire est accessible sans authentification
