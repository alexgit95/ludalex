(function () {
    // Le ZIP est envoyé comme corps brut : la limite multipart de 2 Mo (images) ne s'applique pas
    var formulaire = document.getElementById('formulaire-restauration');
    if (!formulaire) {
        return;
    }
    var resultat = document.getElementById('resultat-restauration');

    function afficher(html) {
        resultat.innerHTML = html;
    }

    formulaire.addEventListener('submit', function (evenement) {
        evenement.preventDefault();
        var fichier = formulaire.elements.sauvegarde.files[0];
        var jeton = document.querySelector('meta[name="_csrf"]');
        var enteteJeton = document.querySelector('meta[name="_csrf_header"]');
        if (!fichier) {
            return;
        }
        var entetes = { 'Content-Type': 'application/zip' };
        entetes[formulaire.dataset.enteteConfirmation] = formulaire.elements.confirmation.value;
        if (jeton && enteteJeton) {
            entetes[enteteJeton.content] = jeton.content;
        }
        var bouton = formulaire.querySelector('button[type="submit"]');
        bouton.disabled = true;
        afficher('<p class="message">Restauration en cours, ne fermez pas la page…</p>');
        fetch('/admin/sauvegarde/restaurer', { method: 'POST', headers: entetes, body: fichier })
            .then(function (reponse) { return reponse.text(); })
            .then(afficher)
            .catch(function () {
                afficher('<p class="erreur">Échec de l\'envoi : vérifiez la connexion et la taille du fichier.</p>');
            })
            .finally(function () { bouton.disabled = false; });
    });
})();
