## 1. Export

- [ ] 1.1 Définir le format v1 (manifeste, fichiers JSON par entité, dossier `images/`) et les DTO d'export
- [ ] 1.2 Implémenter l'export en flux dans la réponse HTTP (`ZipOutputStream`, JSON en flux, lots de 500, vidage du contexte)
- [ ] 1.3 Copier les images uploadées par flux et ignorer les images par URL
- [ ] 1.4 Calculer les compteurs du manifeste et exclure les tokens
- [ ] 1.5 Test d'exhaustivité : chaque entité JPA est exportée
- [ ] 1.6 Tests : contenu du ZIP, images par URL, absence de tokens, lecture par lots

## 2. Validation de la restauration

- [ ] 2.1 Écrire le ZIP reçu en fichier temporaire avec plafond de taille configurable
- [ ] 2.2 Valider manifeste et version de format
- [ ] 2.3 Valider les entrées (liste autorisée, chemins normalisés, nombre, taille décompressée, ratio)
- [ ] 2.4 Valider les images (signature réelle, 2 Mo maximum, fichiers référencés présents)
- [ ] 2.5 Valider la cohérence des références entre fichiers et les compteurs du manifeste
- [ ] 2.6 Tests : zip slip, bombe de décompression, entrée inattendue, image non conforme, version inconnue, références incohérentes, ZIP corrompu

## 3. Application de la restauration

- [ ] 3.1 Implémenter la suppression complète dans l'ordre des dépendances (dont les tokens)
- [ ] 3.2 Implémenter l'insertion par lots avec table de correspondance des identifiants
- [ ] 3.3 Extraire les images dans un répertoire de préparation puis l'échanger avec le volume après commit
- [ ] 3.4 Garantir l'atomicité (rollback et nettoyage en cas d'échec), un verrou de restauration unique et le contrôle de l'espace disque
- [ ] 3.5 Supprimer le fichier temporaire en toutes circonstances
- [ ] 3.6 Tests : aller-retour export/restauration, changement de moteur (SQLite ↔ PostgreSQL ou équivalent de test), échec en cours d'import, tokens supprimés, volume sans orphelins

## 4. Interface Admin

- [ ] 4.1 Créer la page Sauvegarde/Restauration (téléchargement, upload, avertissement)
- [ ] 4.2 Exiger le mot de confirmation et le jeton CSRF avant restauration
- [ ] 4.3 Afficher le résultat ou l'erreur de validation explicite

## 5. Documentation et livraison

- [ ] 5.1 Documenter le format de sauvegarde et sa règle d'évolution dans `README.md`
- [ ] 5.2 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à `0.9.0`, identique dans les deux fichiers
- [ ] 5.3 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
