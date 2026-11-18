## MODIFIED Requirements

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

## ADDED Requirements

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