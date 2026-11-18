## Context

L'écran alterne actuellement des fragments pilotés côté navigateur, avec interrogation HTMX toutes les trois secondes et signature stable. La veille ne charge pas l'historique et utilise la signature constante `VEILLE`. Les deux vues d'historique en partie partagent une liste de cinq parties ; le QR de fin est commun et fixé en haut à droite. Les joueurs du podium sont limités à trois par rang et les autres ex æquo sont comptés.

## Goals / Non-Goals

**Goals:**

- Organiser la vue du jeu en portrait en trois zones verticales : podium, trois dernières parties, QR de fin ; conserver cinq parties en paysage.
- Réutiliser une galerie des cinq dernières parties terminées en veille et pendant une partie, en donnant la priorité aux gagnants.
- Introduire en veille un QR de lancement sur une image sur deux et un récapitulatif après deux à cinq images, tirés au hasard.
- Conserver la sécurité Cloudflare/CSRF, les transitions d'état, la lisibilité et la sobriété Raspberry Pi.

**Non-Goals:**

- Autoriser un lancement anonyme, créer une nouvelle API publique, modifier le modèle de données ou ajouter des réglages d'administration.
- Modifier la règle de classement du podium, la durée de dix secondes ou le comportement des parties en pause et de victoire.

## Decisions

1. **Une galerie commune.** Créer un fragment réutilisable en veille et en partie, affichant les cinq dernières parties terminées par date puis identifiant décroissants, sans dédupliquer les jeux. Utiliser l'image du jeu, sa date et ses gagnants sous forme de médaillons disposés de manière expressive mais lisible sur les deux orientations ; afficher tous les gagnants, ou la défaite collective, sans liste des autres participants. Réutiliser les images de secours et les requêtes bornées et groupées existantes. Une seconde implémentation de la galerie risquerait de diverger.
2. **Portrait selon l'orientation réelle.** Utiliser l'orientation du viewport, pas seulement sa largeur, y compris les écrans portrait larges ; traiter une fenêtre carrée comme portrait, conformément au diaporama. En portrait, organiser podium, trois dernières parties et QR en flux vertical ; en paysage conserver la présentation et cinq parties. Ne jamais dupliquer le QR de fin ni le masquer derrière un autre élément. Conserver les gagnants et autres données de la liste de jeu.
3. **Veille et historique.** Charger les données globales uniquement à l'ouverture ou lors d'un changement d'état ou de données, jamais à chaque interrogation légère. Étendre l'invalidation de la veille, dont la signature est actuellement constante, pour que l'ajout, la modification et la suppression de parties ou d'images associées soient reflétés sans rechargement manuel. Les réponses inchangées ne réinitialisent pas la rotation. Éviter les caches non bornés et les collections volumineuses.
4. **Rotation de veille locale.** À chaque retour de la galerie, tirer uniformément un nombre entier de deux à cinq affichages d'image avant le prochain récapitulatif. Démarrer par une image ; chaque affichage d'image dure dix secondes, même si une seule image ou le fond LUDALEX est disponible. La galerie dure dix secondes et n'est pas programmée si l'historique est vide. Un compteur indépendant rend le QR visible sur le premier affichage d'image, puis un affichage sur deux, sans le montrer sur la galerie. Un changement d'orientation ne compte pas comme un nouvel affichage. Les transitions HTMX nettoient les minuteurs et ne relancent pas de rotation pour une réponse inchangée.
5. **QR de lancement protégé.** Générer côté serveur un QR portant l'origine publique existante suivie de `/parties/direct`, avec un libellé accessible spécifique. Le scan ouvre seulement le formulaire de lancement ; l'authentification Cloudflare en production et la protection CSRF des commandes restent obligatoires. Ne pas étendre le bypass `/fin/**`, exposer de token de fin ni lancer de partie par simple GET. En développement, l'origine doit être accessible au téléphone, avec le réglage existant si nécessaire.
6. **Podium.** Afficher chaque score en `nV` avec une alternative accessible explicite ; décorer le premier rang de lauriers, y compris lorsqu'il est partagé. Conserver le compteur des ex æquo non affichés, l'animation du troisième rang au premier et le comportement en mouvement réduit. Utiliser des ressources locales sobres conformes à la charte.

## Risks / Trade-offs

- [Nombre élevé de gagnants ou noms longs] → Prévoir une composition responsive sans chevauchement ni suppression silencieuse des gagnants, et tester les cas extrêmes ainsi que la lisibilité du QR.
- [QR mal compris comme une autorisation] → Préciser qu'il ouvre une page protégée ; tester le refus sans JWT en prod, l'accès en dev et le CSRF du lancement.
- [Incohérence de la galerie entre veille et partie] → Partager le fragment et tester les deux états, y compris les modifications de données et les défauts de chargement des images.
- [Accumulation de minuteurs ou préchargements] → Nettoyer l'état lors des remplacements HTMX, conserver une seule image préchargée et tester orientation, transitions et plusieurs cycles.

## Migration Plan

Documenter les changements fonctionnels et leur migration dans le changelog, incrémenter la version majeure et le cache PWA, puis reconstruire l'image Docker. Aucun changement de schéma ni de sauvegarde n'est attendu. Un retour à la version précédente restaure l'ancienne présentation et le diaporama sans QR de lancement.

## Open Questions

Aucune bloquante ; les médaillons doivent respecter la charte et la lisibilité des écrans réels.