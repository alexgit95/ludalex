## Why

Les images distantes des jeux et des joueurs peuvent disparaitre ou devenir inaccessibles. Une copie locale preventive doit permettre de continuer a les afficher sans renoncer a utiliser leur URL d'origine en priorite.

## What Changes

- Copier automatiquement en arriere-plan toutes les images par URL referencees par l'application : existantes, nouvelles et issues des imports ou restaurations.
- Stocker les copies sur le volume persistant des images, apres verification SSRF, de taille et de format, avec une consommation de ressources bornee.
- Afficher l'URL distante en premier, puis la copie locale sur toute erreur de chargement de cette ressource image, puis l'image par defaut si necessaire. Ce mecanisme ne concerne pas les autres ressources ou requetes de l'application.
- Ajouter dans l'administration un suivi pagine des copies, des compteurs, des erreurs et une relance des echecs ; le traitement reste automatique et reprend apres redemarrage.
- Inclure les copies disponibles et leurs associations dans le ZIP de sauvegarde ; continuer a accepter les anciens ZIP ne contenant que les URL.
- **BREAKING** : remplacer la regle interdisant toute copie locale d'une image par URL et faire evoluer le format d'export. Le futur deploiement doit conserver le volume des images, accepter les sauvegardes historiques et documenter que les nouveaux ZIP ne sont pas lisibles par les anciennes versions.

## Capabilities

### New Capabilities

- `copies-images-distantes` : collecte automatique persistante, suivi depuis l'administration, reprise et gestion du cycle de vie des copies de secours.

### Modified Capabilities

- `stockage-images` : autoriser une copie de secours d'une source URL, restituer cette copie et definir la chaine de repli des images dans toute l'IHM.
- `sauvegarde-restauration` : exporter et restaurer les copies locales et leurs associations, en conservant la lecture des anciens ZIP.

## Impact

- Backend images, traitement des references de jeux et joueurs, imports, administration MVC/HTMX, templates et affichage/diaporama.
- Nouvelle persistance de l'etat des copies compatible SQLite et PostgreSQL, sans remplacer le choix URL/upload des fiches ; mise a jour du modele PlantUML lors de l'implementation.
- Volume existant `/data/images`, sans stockage dans la couche ephemere du conteneur ni ajout de service externe ; aucune modification des pipelines.
- Sauvegarde/restauration, tests unitaires et d'integration, README, instructions projet contredisant la copie des URL, changelog et version majeure synchronisee avec le POM lors de l'implementation.
- Aucun changement de l'authentification, des permissions des liens de fin de partie ou du calcul ponctuel de couleur de fond.