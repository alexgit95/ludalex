## Context

`ImageSource` distingue une URL d'un upload et fournit actuellement une seule adresse d'affichage. `ServiceImages` telecharge ponctuellement les images des jeux pour leur couleur, sans conserver ces octets ; les joueurs n'ont pas ce telechargement. `TelechargeurImage` applique deja les controles SSRF et les bornes reseau. Les templates ont plusieurs chemins d'affichage, dont des replis directs vers l'image par defaut et des images decoratives supprimees en cas d'erreur.

Le volume `/data/images` est deja persistant. L'application utilise Thymeleaf/HTMX, SQLite en dev et PostgreSQL en prod, sans outil de migration ni service de file externe. Les sauvegardes actuelles ne contiennent que les images uploadees.

## Goals / Non-Goals

**Goals:**
- Proteger toutes les images par URL referencees, jeux et joueurs compris, existantes et futures.
- Garder la priorite distante et fournir une copie de secours sur toute erreur de chargement d'une image.
- Automatiser la collecte avec suivi admin, reprise durable et ressources bornees.
- Conserver les copies et leurs associations lors d'une sauvegarde/restauration.

**Non-Goals:**
- Intercepter les erreurs des pages, scripts, API ou autres ressources non images.
- Recuperer une image deja disparue avant sa premiere copie reussie.
- Synchroniser periodiquement le contenu d'une URL deja copiee ou garantir un mode hors ligne complet de la PWA.
- Modifier le calcul ou les valeurs existantes de couleur de fond, l'authentification, les pipelines ou le choix URL/upload des utilisateurs.

## Decisions

### 1. Copie distincte de la source choisie

Conserver `ImageSource` comme choix URL/upload. Ajouter une table dediee aux copies, partagees pour une meme URL exacte, avec cle SHA-256 de l'URL, URL d'origine, etat, nom de fichier genere, taille, nombre de tentatives, dates de tentative/prochaine tentative et derniere erreur bornee. Verifier l'egalite de l'URL lors d'une correspondance de cle. Ne pas normaliser les parametres des URL signees.

Ce choix evite de convertir une source URL en upload et limite la duplication des fichiers. Ajouter des colonnes de copie a chaque entite multiplierait les etats et les telechargements.

### 2. File durable et traitement mono-worker

Decouvrir les references par lots au demarrage et periodiquement pour reconciler l'etat, notamment apres import/restauration. Enregistrer aussi les references nouvelles apres commit, sans reseau dans la transaction metier. Un worker unique traite les travaux eligibles ; aucun broker et aucune file contenant toutes les URL en memoire.

Etats : EN_ATTENTE -> EN_COURS -> DISPONIBLE, ou retour EN_ATTENTE avec echeance, puis ECHEC apres au plus trois tentatives. Espacer les deux relances automatiques d'au moins une minute puis quinze minutes. Un refus de securite ou un contenu non conforme passe directement en ECHEC. La reconciliation ne reinitialise pas les echecs ; seule une action de relance explicite le fait. Au redemarrage, reprendre les travaux interrompus sans reprendre les copies disponibles.

La reference metier courante doit encore exister avant publication d'une copie. Une ancienne URL ne doit jamais fournir la copie de la nouvelle. Un verrou de coordination interne doit empecher le worker de publier pendant un remplacement de donnees par restauration ; ce design cible l'instance applicative unique existante.

### 3. Ecriture en flux et publication atomique

Reutiliser les regles de `UrlSortanteValidateur` et adapter le telechargeur/stockage pour cette voie en flux, sans charger une image complete en memoire. Conserver les delais et bornes de redirections existants, la limite de 2 Mo et les formats JPEG, PNG, GIF et WebP valides sur leur contenu reel, pas sur leur extension ou leur seul Content-Type.

Ecrire un fichier temporaire sur le volume, verifier puis publier sous un nom genere ; ne declarer DISPONIBLE qu'apres ecriture reussie. Nettoyer les temporaires sur erreur et les orphelins par reconciliation, y compris apres un crash. Une erreur disque doit etre visible et ne doit pas bloquer les modifications de fiches. Le telechargement utilise les memes protections contre le DNS rebinding et revalide les redirections.

### 4. Repli dans le navigateur, limite aux images

Exposer aux vues l'URL distante et, si disponible, une adresse locale de secours via le mecanisme protege de restitution des images. Resoudre les copies par lots dans les vues de listes pour eviter une requete supplementaire par image.

Chaine commune : distante -> locale -> image par defaut, sans boucle ni nouvelle tentative distante pour le meme element. Pour les images decoratives actuellement retirees sur erreur, essayer aussi la copie avant le comportement terminal existant. Couvrir les fragments, apercus, ecran d'affichage, diaporama et mises a jour HTMX. Une copie absente ou devenue illisible ne bloque jamais l'IHM.

L'evenement d'erreur d'une image ne permet pas de distinguer un 404 d'une erreur reseau, d'un refus ou d'un contenu indecodable : toutes ces erreurs declenchent le repli. Pas de proxy serveur permanent ni de requete HEAD prealable, qui ajouteraient charge, latence et appels SSRF. Une requete distante qui ne termine pas ne declenche pas immediatement le repli ; aucun delai navigateur additionnel n'est promis.

### 5. Administration dans l'experience existante

Ajouter une rubrique au sein de l'administration existante : compteurs par etat, volume occupe par les copies, liste paginee avec fiche(s) concernee(s), dates, tentatives et erreur intelligible. Rafraichir compteurs et page courante par HTMX ; proposer la relance d'un echec et des echecs par action protegee CSRF. Les URL sont echappees, les messages d'erreur ne divulguent pas d'informations internes sensibles.

Conserver les protections habituelles : Cloudflare Access obligatoire en prod, acces libre en dev ; aucune nouvelle permission publique sous `/fin/**` ni exception pour les copies locales.

### 6. Cycle de vie et sauvegarde

Supprimer les copies qui ne sont plus referencees apres commit/reconciliation ; ne jamais supprimer une copie encore utilisee par une autre fiche. Une copie disponible n'est pas rafraichie automatiquement. Replacer en attente une copie referencee dont le fichier a disparu, sans faire d'acces reseau dans la requete d'affichage.

Faire evoluer le format ZIP vers une version nouvelle contenant les associations URL/copie et les fichiers disponibles, dedupliques. Ne pas exporter une file de travail active ou les erreurs techniques ; les etats non disponibles sont reconstruits a partir des sources. Maintenir la lecture du format precedent : les sources URL restaurees sans copie sont automatiquement mises en attente.

L'export doit presenter un ensemble coherent de references et fichiers, meme si le worker termine ou nettoie un fichier en parallele, avec coordination bornee et sans suspendre l'export pour un appel reseau. La restauration garde les validations, limites de ZIP, atomicite et traitement en flux actuels ; suspendre la publication du worker pendant le remplacement puis reconciler.

## Risks / Trade-offs

- [URL deja inaccessible] -> Afficher l'echec et conserver le comportement de secours par defaut ; aucune recuperation retroactive garantie.
- [Stockage supplementaire] -> Une copie par URL exacte, limite de taille, nettoyage des orphelins et volume occupe visible ; pas de suppression automatique d'une copie utile.
- [URL identiques mais contenu changeant] -> Conserver la premiere copie valide ; la synchronisation du contenu reste hors perimetre.
- [URL differentes pour un meme contenu] -> Accepter cette duplication plutot qu'introduire une deduplication complexe par contenu.
- [Latence avant une erreur navigateur] -> Le distant reste prioritaire par choix ; ne pas promettre un repli immediat en cas de requete suspendue.
- [Concurrence avec restauration ou edition] -> Coordination locale et revalidation des references avant publication, avec tests de non-regression.
- [Evolution du contrat et du ZIP] -> Version majeure documentee ; restaurer les anciens ZIP, annoncer l'incompatibilite des nouveaux ZIP avec les anciennes applications.

## Migration Plan

1. Sauvegarder donnees et volume avant deploiement ; ne pas modifier les pipelines.
2. Ajouter la table via Hibernate en conservant les colonnes existantes et mettre a jour `docs/database.puml`.
3. Deployer avec le volume existant : l'initialisation par lots collecte automatiquement les URL sans bloquer le demarrage sur le reseau.
4. Verifier le suivi admin, une copie reussie et le repli navigateur ; verifier un aller-retour du nouveau ZIP et l'import d'un ancien ZIP.
5. Mettre a jour README, instructions projet sur les images, changelog avec rubrique BREAKING CHANGE et version majeure synchronisee avec le POM lors de l'implementation.
6. Pour revenir a l'ancienne version, utiliser la sauvegarde et le volume d'avant deploiement. Ne pas presenter un ZIP du nouveau format a l'ancienne application.

## Open Questions

Aucune decision fonctionnelle bloquante. Les choix locaux de noms de routes et de classes suivront les conventions du projet lors de l'implementation.