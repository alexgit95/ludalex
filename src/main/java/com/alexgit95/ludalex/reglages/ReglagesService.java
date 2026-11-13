package com.alexgit95.ludalex.reglages;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture avec repli sur la valeur par défaut, écriture validée par le catalogue. */
@Service
public class ReglagesService {

    private final ReglageRepository reglages;

    public ReglagesService(ReglageRepository reglages) {
        this.reglages = reglages;
    }

    @Transactional(readOnly = true)
    public int valeur(ReglageCle cle) {
        return reglages.findByCle(cle.cle()).flatMap(r -> cle.analyser(r.getValeur())).orElse(cle.defaut());
    }

    /** Toutes les valeurs effectives en une requête. */
    @Transactional(readOnly = true)
    public Map<ReglageCle, Integer> tous() {
        Map<ReglageCle, Integer> valeurs = new EnumMap<>(ReglageCle.class);
        for (ReglageCle cle : ReglageCle.values()) {
            valeurs.put(cle, cle.defaut());
        }
        for (Reglage reglage : reglages.findAll()) {
            ReglageCle.depuisCle(reglage.getCle())
                    .ifPresent(cle -> cle.analyser(reglage.getValeur()).ifPresent(v -> valeurs.put(cle, v)));
        }
        return valeurs;
    }

    /** @throws ReglageInvalideException si une valeur est absente, non numérique ou hors bornes ; rien n'est alors écrit */
    @Transactional
    public void enregistrer(Map<ReglageCle, String> saisies) {
        Map<ReglageCle, String> erreurs = new LinkedHashMap<>();
        Map<ReglageCle, Integer> valides = new LinkedHashMap<>();
        for (Map.Entry<ReglageCle, String> saisie : saisies.entrySet()) {
            ReglageCle cle = saisie.getKey();
            Optional<Integer> valeur = cle.analyser(saisie.getValue());
            if (valeur.isPresent()) {
                valides.put(cle, valeur.get());
            } else {
                erreurs.put(cle, "Saisir un entier entre " + cle.min() + " et " + cle.max());
            }
        }
        if (!erreurs.isEmpty()) {
            throw new ReglageInvalideException(erreurs);
        }
        valides.forEach((cle, valeur) -> {
            String texte = Integer.toString(valeur);
            Reglage existant = reglages.findByCle(cle.cle()).orElse(null);
            if (existant == null) {
                reglages.save(new Reglage(cle.cle(), texte));
            } else if (!existant.getValeur().equals(texte)) {
                existant.setValeur(texte);
            }
        });
    }

    /** Supprime la valeur enregistrée : la valeur par défaut s'applique de nouveau. */
    @Transactional
    public void retablir(ReglageCle cle) {
        reglages.findByCle(cle.cle()).ifPresent(reglages::delete);
    }
}
