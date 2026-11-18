(function () {
    var ECHECS_MAX = 3;
    var echecs = 0;
    var minuteur = null;
    var zoneCourante = null;
    var changerOrientation = null;

    // Session Cloudflare expirée ou réseau coupé : on recharge la page entière pour que Cloudflare redemande la connexion
    document.addEventListener('htmx:afterRequest', function (evenement) {
        if (evenement.detail.successful) {
            echecs = 0;
            return;
        }
        echecs += 1;
        if (echecs >= ECHECS_MAX) {
            window.location.reload();
        }
    });

    function demarrerAffichage() {
        var zone = document.getElementById('zone-affichage');
        if (zone === zoneCourante) {
            return;
        }
        zoneCourante = zone;
        if (minuteur !== null) {
            clearInterval(minuteur);
            minuteur = null;
        }
        changerOrientation = null;
        var conteneur = document.getElementById('diaporama');
        if (!conteneur) {
            return;
        }
        var images = (conteneur.dataset.images || '').split('|').filter(Boolean);
        var couches = conteneur.querySelectorAll('.couche');
        var vide = conteneur.querySelector('.veille-vide');
        var indexes = { portrait: null, paysage: null };
        var active = 0;
        var suivante = null;
        var rotation = document.getElementById('rotation');
        var veille = document.getElementById('veille-rotation');
        var racine = rotation || veille;
        var vues = racine ? Array.from(racine.querySelectorAll('[data-vue]')) : [];
        var qrLancement = veille ? veille.querySelector('.qr-lancement') : null;
        var ordre = rotation ? ['combat', 'image'] : [];
        if (rotation && rotation.querySelector('[data-vue="jeu"]')) {
            ordre.push('jeu', 'image');
        }
        if (rotation && rotation.querySelector('[data-vue="global"]')) {
            ordre.push('global', 'image');
        }
        var position = 0;
        var imagesAffichees = 1;
        var imagesDepuisGalerie = 1;
        var prochainRecapitulatif = 2 + Math.floor(Math.random() * 4);
        var vueVeille = 'image';

        function afficherImage(avancer, changement) {
            var mode = window.matchMedia('(orientation: portrait)').matches ? 'portrait' : 'paysage';
            var selection = images.filter(function (url) {
                var nom = url.substring(url.lastIndexOf('/') + 1);
                return (nom.indexOf('portrait_') === 0) === (mode === 'portrait');
            });
            if (changement) {
                couches.forEach(function (couche) {
                    couche.classList.remove('visible');
                    couche.style.backgroundImage = '';
                });
            }
            vide.hidden = selection.length > 0;
            if (selection.length === 0) {
                couches.forEach(function (couche) { couche.classList.remove('visible'); });
                suivante = null;
                return;
            }
            if (indexes[mode] === null) {
                indexes[mode] = Math.floor(Math.random() * selection.length);
            } else if (avancer) {
                indexes[mode] = (indexes[mode] + 1) % selection.length;
            }
            var cible = 1 - active;
            couches[cible].style.backgroundImage = 'url("' + selection[indexes[mode]] + '")';
            couches[cible].classList.add('visible');
            couches[active].classList.remove('visible');
            active = cible;
            suivante = new Image();
            suivante.src = selection[(indexes[mode] + 1) % selection.length];
        }

        function afficherVue(nom) {
            vues.forEach(function (vue) {
                vue.hidden = vue.dataset.vue !== nom;
                vue.classList.toggle('revele-podium', !vue.hidden && nom === 'jeu');
            });
            if (nom === 'image') {
                afficherImage(true, false);
            }
            if (qrLancement) {
                qrLancement.hidden = nom !== 'image' || imagesAffichees % 2 === 0;
            }
        }

        changerOrientation = function () {
            if ((veille && vueVeille === 'image') || (rotation && ordre[position] === 'image')) {
                afficherImage(false, true);
            } else {
                couches.forEach(function (couche) {
                    couche.classList.remove('visible');
                    couche.style.backgroundImage = '';
                });
                suivante = null;
            }
        };
        if (veille) {
            afficherImage(false, false);
        }
        minuteur = setInterval(function () {
            if (rotation) {
                position = (position + 1) % ordre.length;
                afficherVue(ordre[position]);
            } else if (veille && vueVeille === 'global') {
                vueVeille = 'image';
                imagesAffichees += 1;
                imagesDepuisGalerie = 1;
                prochainRecapitulatif = 2 + Math.floor(Math.random() * 4);
                afficherVue('image');
            } else if (veille && veille.querySelector('[data-vue="global"]')
                    && imagesDepuisGalerie >= prochainRecapitulatif) {
                vueVeille = 'global';
                afficherVue('global');
            } else if (veille) {
                imagesAffichees += 1;
                imagesDepuisGalerie += 1;
                afficherImage(true, false);
                if (qrLancement) {
                    qrLancement.hidden = imagesAffichees % 2 === 0;
                }
            }
        }, 10000);
    }

    document.addEventListener('DOMContentLoaded', demarrerAffichage);
    document.addEventListener('htmx:afterSettle', demarrerAffichage);
    window.matchMedia('(orientation: portrait)').addEventListener('change', function () {
        if (changerOrientation) {
            changerOrientation();
        }
    });

    // Écran maintenu allumé quand le navigateur le permet (HTTPS)
    var verrou = null;
    function garderAllume() {
        if (!('wakeLock' in navigator)) {
            return;
        }
        navigator.wakeLock.request('screen').then(function (obtenu) { verrou = obtenu; }).catch(function () { /* facultatif */ });
    }
    document.addEventListener('DOMContentLoaded', garderAllume);
    document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'visible' && (verrou === null || verrou.released)) {
            garderAllume();
        }
    });
})();
