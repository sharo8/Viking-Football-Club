package rw.vikingfc.club.player;

import java.util.Arrays;

public enum Position {
    GOALKEEPER("Goalkeeper"),
    CENTER_BACK("Center-Back"),
    LEFT_BACK("Left-Back"),
    RIGHT_BACK("Right-Back"),
    DEFENSIVE_MIDFIELDER("Defensive Midfielder"),
    CENTRAL_MIDFIELDER("Central Midfielder"),
    OFFENSIVE_MIDFIELDER("Offensive Midfielder"),
    WINGER("Winger"),
    FORWARD("Forward");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Position fromLabel(String raw) {
        return Arrays.stream(values())
                .filter(position -> position.label.equalsIgnoreCase(raw) || position.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown position: " + raw));
    }
}
