# Fin de partie par QR code

Permet de valider le résultat d'une partie depuis l'appareil de n'importe qui, sans passer par Cloudflare Access.

## Principe

1. L'écran Affichage (protégé par Cloudflare Access) montre un QR code encodant `https://<domaine>/fin/<token>`.
2. Un invité scanne le QR avec son téléphone et arrive sur une page minimale.
3. Il indique que la partie est terminée et désigne le(s) gagnant(s), ou met la partie en pause en indiquant à qui c'était de jouer.
4. Le token est consommé : il ne fonctionne plus ensuite.
5. Si l'écran Affichage est ouvert, l'animation de victoire se déclenche.

```
Écran Affichage ──(QR)──► Téléphone invité ──► Cloudflare (Bypass /fin/*) ──► Application
   (JWT requis)                                                       valide le token, pas de JWT
```

## Règles du token

| Règle | Valeur |
|---|---|
| Génération | Aléatoire, au moins 128 bits, créé au lancement de la partie |
| Stockage | Haché en base (le clair n'existe que dans le QR) |
| Portée | Une seule partie |
| Usage | **Unique** : invalidé dès la première validation |
| Expiration | 7 jours |
| Droits | Désigner un ou plusieurs gagnants, renseigner les scores (facultatif) ou mettre en pause, uniquement parmi les joueurs de la partie |
| Données exposées | Aucune autre que la partie concernée |
| Protection | Limitation de débit, CSRF sur le formulaire, réponse identique pour un token inconnu, expiré ou utilisé |

Une mise en pause consomme aussi le token ; un nouveau token est généré à la reprise de la partie.

### Côté écran Affichage

- Le QR code est généré en SVG par le serveur (ZXing) ; le token en clair n'est jamais stocké en base, il n'est gardé qu'en mémoire du processus pour la partie affichée.
- **Chaque ouverture ou rechargement de `/affichage` génère un nouveau token** et invalide l'ancien non utilisé : un QR scanné avant le rechargement ne fonctionne plus, il faut rescanner.
- L'interrogation toutes les 3 s (`/affichage/etat`) ne génère pas de token ; elle en crée un nouveau uniquement s'il est absent, consommé ou s'il expire dans moins de 24 h.
- Pendant la pause, aucun QR n'est affiché ; il revient (avec un nouveau token) à la reprise.
- L'adresse encodée utilise le schéma et le domaine publics (`X-Forwarded-*`) ou `ludalex.affichage.url-publique` si renseignée (utile en `dev` pour qu'un téléphone joigne le poste).

## Réglages applicatifs

| Propriété | Défaut | Rôle |
|---|---|---|
| `ludalex.fin.validite` | `7d` | Durée de validité d'un token |
| `ludalex.fin.limite-par-minute` | `30` | Requêtes par minute et par adresse sur `/fin/**` (429 au-delà) |
| `ludalex.affichage.url-publique` | vide (déduite de la requête) | Origine imposée pour l'adresse du QR code |
| `ludalex.affichage.duree-victoire` | `15s` | Durée de l'animation de victoire |
| `ludalex.affichage.renouvellement-token` | `24h` | Un token expirant plus tôt est renouvelé |
| `ludalex.fin.en-tete-ip` | vide (`CF-Connecting-IP` en `prod`) | En-tête qui porte l'adresse du client ; Cloudflare l'écrase, un client ne peut donc pas le falsifier derrière lui |

Conséquence : l'application ne doit pas être joignable autrement que par Cloudflare en `prod`, sinon l'en-tête d'adresse pourrait être falsifié pour contourner la limitation.

## À faire côté Cloudflare

Dans Zero Trust, application Access de Ludalex :

1. Conserver la politique **Allow** actuelle sur toute l'application (propriétaire uniquement).
2. Ajouter une seconde application (ou un chemin) ciblant **uniquement** `<domaine>/fin/*`.
3. Lui associer une politique **Bypass** (Include : Everyone).
4. Vérifier que Bypass ne couvre aucun autre chemin (pas de `/*`, pas de `/api`).
5. Tester : sans session, `/fin/<token>` répond ; `/` redirige vers la connexion Cloudflare.

Le Bypass désactive la protection Cloudflare sur ce chemin : la sécurité repose alors entièrement sur le token et le contrôle de débit côté application. Il est conseillé d'ajouter une règle de limitation de débit Cloudflare sur `/fin/*`.

## À faire côté application

- En `prod`, le filtre de validation du JWT ignore uniquement le chemin `/fin/**` ; tout le reste exige un JWT valide.
- En `dev`, aucune authentification n'est requise.
- Tests : token valide, expiré, déjà utilisé, d'une autre partie, inconnu ; JWT toujours exigé hors `/fin/**`.
