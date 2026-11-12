(function () {
    // Jeton CSRF ajouté à chaque requête HTMX modifiante
    document.addEventListener('htmx:configRequest', function (evenement) {
        var jeton = document.querySelector('meta[name="_csrf"]');
        var entete = document.querySelector('meta[name="_csrf_header"]');
        if (jeton && entete) {
            evenement.detail.headers[entete.content] = jeton.content;
        }
    });

    // Aperçu immédiat de l'image choisie (fichier local ou adresse web)
    document.addEventListener('change', function (evenement) {
        var champ = evenement.target;
        var apercu = champ.closest && champ.closest('.saisie-image')
            ? champ.closest('.saisie-image').querySelector('.apercu') : null;
        if (!apercu) {
            return;
        }
        if (champ.type === 'file' && champ.files.length > 0) {
            apercu.src = URL.createObjectURL(champ.files[0]);
        } else if (champ.type === 'url' && champ.value) {
            apercu.src = champ.value;
        }
    });

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js').catch(function () { /* PWA facultative */ });
    }
})();
