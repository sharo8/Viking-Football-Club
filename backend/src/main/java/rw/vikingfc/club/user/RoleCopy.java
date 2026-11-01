package rw.vikingfc.club.user;

import java.util.Map;

/** Names of seeded roles. Custom roles are translated in the database instead. */
public final class RoleCopy {

    private static final Map<String, Map<String, String>> NAMES = Map.ofEntries(
            Map.entry("PRESIDENT", Map.of("en", "President", "fr", "Président", "rw", "Perezida")),
            Map.entry("GENERAL_MANAGER", Map.of("en", "General Manager", "fr", "Directeur général", "rw", "Umuyobozi mukuru")),
            Map.entry("FINANCE_MANAGER", Map.of("en", "Finance Manager", "fr", "Responsable financier", "rw", "Umuyobozi w'imari")),
            Map.entry("ASSISTANT_MANAGER", Map.of("en", "Assistant Manager", "fr", "Directeur adjoint", "rw", "Umufasha w'umuyobozi")),
            Map.entry("SOCIAL_MEDIA_MANAGER", Map.of("en", "Social Media Manager", "fr", "Responsable des réseaux", "rw", "Umuyobozi w'imbuga nkoranyambaga")),
            Map.entry("MARKETING_MANAGER", Map.of("en", "Marketing Manager", "fr", "Responsable marketing", "rw", "Umuyobozi w'kwamamaza")),
            Map.entry("HEAD_COACH", Map.of("en", "Head Coach", "fr", "Entraîneur principal", "rw", "Umutoza mukuru")),
            Map.entry("ASSISTANT_COACH", Map.of("en", "Assistant Coach", "fr", "Entraîneur adjoint", "rw", "Umufasha w'umutoza")),
            Map.entry("GOALKEEPER_COACH", Map.of("en", "Goalkeeper Coach", "fr", "Entraîneur des gardiens", "rw", "Umutoza w'abakinnyi b'intego")),
            Map.entry("FITNESS_COACH", Map.of("en", "Fitness Coach", "fr", "Préparateur physique", "rw", "Umutoza w'imbaraga")),
            Map.entry("DOCTOR", Map.of("en", "Doctor", "fr", "Médecin", "rw", "Muganga")),
            Map.entry("PHYSIOTHERAPIST", Map.of("en", "Physiotherapist", "fr", "Kinésithérapeute", "rw", "Umuforomo w'imikino")),
            Map.entry("KIT_MANAGER", Map.of("en", "Kit Manager", "fr", "Responsable du matériel", "rw", "Umuyobozi w'ibikoresho")),
            Map.entry("TEAM_MANAGER", Map.of("en", "Team Manager", "fr", "Team manager", "rw", "Umuyobozi w'ikipe")),
            Map.entry("PLAYER", Map.of("en", "Player", "fr", "Joueur", "rw", "Umukinnyi"))
    );

    private RoleCopy() {
    }

    public static Map<String, String> of(String code) {
        return NAMES.get(code);
    }
}
