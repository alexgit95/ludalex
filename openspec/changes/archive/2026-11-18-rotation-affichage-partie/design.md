## Context

L'écran `/affichage` reçoit un état serveur interrogé toutes les trois secondes. Une signature stable évite de remplacer le fragment et de redémarrer le diaporama. Les images sont énumérées au démarrage et le navigateur les affiche en boucle. Les statistiques par jeu disposent déjà de requêtes bornées et du calcul des champions.

## Goals / Non-Goals

**Goals:**

- Alterner toutes les dix secondes les vues d'une partie active, en intercalant une image et en gardant le QR visible.
- Adapter les images à l'orientation de la fenêtre et réagir à ses changements, y compris en veille.
- Afficher des historiques récents et les champions selon les règles existantes, sans chargement massif.
- Révéler les trois rangs du podium du jeu en cinq secondes maximum sans prolonger la vue de dix secondes.
- Préserver les transitions rapides entre veille, partie, pause et victoire et les règles de sécurité du QR.

**Non-Goals:**

- Gestion des images depuis l'interface, changement des règles du champion ou modification du schéma de base.
- Rotation des historiques pendant la pause ou la veille, ou configuration des durées dans l'administration.

## Decisions

1. **Rotation locale et état serveur prioritaire.** Le fragment d'une partie en cours contient les vues disponibles et un QR commun ; JavaScript gère la rotation de dix secondes. L'interrogation HTMX reste responsable des changements de statut et du renouvellement du QR. À chaque remplacement du fragment, arrêter proprement les minuteurs et repartir de la confrontation. Une rotation calculée côté serveur aurait rendu les écrans dépendants des interrogations et multiplié les remplacements.
2. **Images par orientation côté navigateur.** Transmettre les images énumérées au serveur puis filtrer leurs noms par le préfixe exact `portrait_` selon l'orientation de la fenêtre. Écouter les changements d'orientation, mettre à jour l'image et le préchargement ; une fenêtre carrée est traitée comme portrait. Chaque passage par le diaporama avance dans la liste de l'orientation courante, triée comme aujourd'hui, avec un départ aléatoire. En l'absence d'image adaptée, afficher LUDALEX. Le filtrage serveur seul nécessiterait de lui transmettre l'orientation à chaque changement.
3. **Historiques bornés et résultat explicite.** Charger au plus cinq parties terminées par vue, par date de partie décroissante puis identifiant décroissant ; charger leurs participations en lot. Présenter date, joueurs et gagnants, ou victoire/défaite coopérative, ainsi que le jeu dans l'historique global. Utiliser les requêtes agrégées existantes pour les champions, en excluant les joueurs anonymisés et en préservant les égalités. Cinq lignes constituent un compromis de lisibilité ; adapter la présentation au viewport sans masquer silencieusement les résultats retenus.
4. **Rafraîchissement et coût maîtrisé.** Prévoir une invalidation ou une actualisation bornée des historiques et champions après modification de données sans charger ces données à chaque interrogation légère de trois secondes ; leur évolution doit finir par apparaître sans rechargement manuel. La signature et les mises à jour ne doivent pas réinitialiser inutilement la rotation ou invalider le QR.
5. **QR commun.** Afficher le même QR renouvelable dans une zone persistante au-dessus de chaque vue, y compris les images et le fond LUDALEX, avec contraste et taille de lecture suffisants. La pause ne montre pas de QR, comme actuellement ; une fin de partie interrompt la rotation au profit de la victoire.
6. **Podium à rangs partagés.** Sur la vue de l'historique du jeu, regrouper les joueurs non anonymisés par nombre de victoires sur les parties terminées et retenir les trois nombres distincts les plus élevés : les ex æquo occupent le même rang. Afficher au plus trois joueurs par rang, triés par nom sans tenir compte de la casse puis par identifiant, et indiquer le nombre d'autres ex æquo. Borner les joueurs chargés depuis la base et obtenir le nombre total de chaque rang par une requête agrégée. Les joueurs sans victoire peuvent figurer sur le podium, mais aucun champion n'est désigné sans victoire. Les groupes sont révélés du troisième au premier pendant les cinq premières secondes ; s'il y a moins de trois rangs, seuls les rangs disponibles sont révélés dans l'ordre décroissant. Le podium reste visible le reste des dix secondes et le QR reste visible. Respecter `prefers-reduced-motion` en affichant immédiatement le podium sans animation.

## Risks / Trade-offs

- [Des minuteurs continuent après remplacement HTMX] → Nettoyer les minuteurs et écouteurs, puis tester plusieurs changements de statut et orientations.
- [Données d'historique périmées ou interrogation coûteuse] → Borner les requêtes et tester l'actualisation après ajout, modification ou suppression, ainsi que la stabilité des réponses inchangées.
- [QR ou résultats illisibles sur petit écran] → Vérifier en portrait et paysage, limiter les lignes et adapter leur disposition ; ne pas recouvrir les contenus.
- [Égalités nombreuses sur le podium] → Limiter à trois joueurs visibles par rang et afficher explicitement le nombre d'autres ex æquo ; tester la lisibilité des rangs remplis.
- [Changement de comportement pour les utilisateurs existants] → Documenter que VS ne reste plus affiché en permanence et que les images `portrait_` sont réservées au portrait.

## Migration Plan

Livrer sous une nouvelle version majeure, documenter le changement et les noms d'images dans le changelog et les README, puis reconstruire l'image Docker. Pour revenir en arrière, redéployer la version précédente : aucune migration de données n'est prévue ; les fichiers `portrait_` y seront à nouveau traités comme des images ordinaires.

## Open Questions

Aucune bloquante ; les cinq lignes d'historique sont un choix de présentation révisable.