package com.alexgit95.ludalex.importmyludo;

import java.util.List;
import java.util.Map;

/** Résultat d'un import réussi : tout ce qui a été fait, ignoré ou signalé. */
public record RapportImport(
        long jeuxCrees,
        long jeuxCompletes,
        long jeuxInchanges,
        long joueursCrees,
        long joueursAssocies,
        int partiesAjoutees,
        int partiesDejaPresentes,
        int totalIgnorees,
        Map<String, Integer> ignoreesParRaison,
        List<String> detailsIgnorees,
        List<String> avertissements,
        int totalAvertissements) {
}
