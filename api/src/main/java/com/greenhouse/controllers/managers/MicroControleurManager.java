package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.MicroControleurRepository;
import com.greenhouse.entities.Actionneur;
import com.greenhouse.entities.Capteur;
import com.greenhouse.entities.MicroControleur;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class MicroControleurManager {

    @Inject
    private MicroControleurRepository microControleurRepository;

    public MicroControleur creerMicroControleur(MicroControleur microControleur) {
        return microControleurRepository.save(microControleur);
    }

    public Optional<MicroControleur> trouverParId(String id) {
        return microControleurRepository.findById(id);
    }

    public Optional<MicroControleur> trouverParAdresseIP(String adresseIP) {
        return microControleurRepository.findByAdresseIP(adresseIP);
    }

    public void ajouterCapteur(String microControleurId, Capteur capteur) {
        microControleurRepository.findById(microControleurId).ifPresent(micro -> {
            micro.getListeCapteur().add(capteur);
            microControleurRepository.save(micro);
        });
    }

    public void ajouterActionneur(String microControleurId, Actionneur actionneur) {
        microControleurRepository.findById(microControleurId).ifPresent(micro -> {
            micro.getListeActionneur().add(actionneur);
            microControleurRepository.save(micro);
        });
    }

    public String lireDonneesCapteur(String microControleurId, Capteur capteur) {
        return microControleurRepository.findById(microControleurId)
                .map(micro -> micro.lireDonneesCapteur(capteur))
                .orElse("Microcontrôleur non trouvé");
    }

    public String envoyerCommande(String microControleurId, Actionneur actionneur, String donnee) {
        return microControleurRepository.findById(microControleurId)
                .map(micro -> micro.envoyerCommande(actionneur, donnee))
                .orElse("Microcontrôleur non trouvé");
    }

    public void supprimerMicroControleur(String id) {
        microControleurRepository.deleteById(id);
    }
}

