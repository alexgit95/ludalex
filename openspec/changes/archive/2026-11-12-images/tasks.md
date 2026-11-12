## 1. Modèle et stockage

- [x] 1.1 Créer l'embeddable `ImageSource` (type UPLOAD|URL, référence) compatible SQLite/PostgreSQL
- [x] 1.2 Configurer le répertoire du volume d'images (`ludalex.images.repertoire`) et la limite de 2 Mo
- [x] 1.3 Implémenter le service de stockage (nom généré, vérification de chemin, détection de type par signature, suppression)
- [x] 1.4 Implémenter le contrôleur de restitution en flux avec en-têtes de cache
- [x] 1.5 Gérer le remplacement d'une source (suppression du fichier remplacé, nettoyage sur rollback)

## 2. Protection SSRF

- [x] 2.1 Implémenter `UrlSortanteValidateur` (schéma, DNS, plages interdites, adresse validée utilisée pour la connexion)
- [x] 2.2 Implémenter le client de téléchargement ponctuel (délai, taille maximale, 5 redirections revalidées)
- [x] 2.3 Tests : adresses privées/locales/métadonnées, redirection interne, domaine résolvant vers une adresse privée, réponse trop volumineuse, schéma refusé

## 3. Couleur de fond

- [x] 3.1 Ajouter la bibliothèque WebP au `pom.xml` et vérifier son fonctionnement sur arm64
- [x] 3.2 Implémenter le décodage avec sous-échantillonnage et refus des images aux dimensions excessives
- [x] 3.3 Implémenter l'histogramme par classes de couleur et le choix de la couleur dominante (ignorer transparent, quasi blanc, quasi noir)
- [x] 3.4 Implémenter le repli neutre et la journalisation
- [x] 3.5 Implémenter la règle de recalcul : nouvelle image sans couleur saisie, conservation sinon
- [x] 3.6 Tests : image majoritairement rouge, image transparente, WebP, grande image, URL injoignable, modification manuelle

## 4. Composants d'interface

- [x] 4.1 Créer le fragment Thymeleaf de saisie d'image (upload ou URL, aperçu, sélecteur de couleur)
- [x] 4.2 Créer le fragment d'affichage d'image (fichier du volume ou URL)

## 5. Documentation et livraison

- [x] 5.1 Mettre à jour `docs/database.puml` si une entité de test ou une table est ajoutée
- [x] 5.2 Mettre à jour `README.md` (formats, limite 2 Mo, volume, calcul de la couleur)
- [x] 5.3 Mettre à jour `CHANGELOG.md` et passer le `pom.xml` à la version `0.2.0`, identique dans les deux fichiers
- [x] 5.4 Lancer `mvn test` avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
