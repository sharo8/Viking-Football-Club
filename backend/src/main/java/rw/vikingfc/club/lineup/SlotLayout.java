package rw.vikingfc.club.lineup;

/**
 * Formation rows share one placement rule: the goalkeeper sits at 88% and the
 * last attacking row at 14%, with x positions fixed by how many players share a row.
 */
public final class SlotLayout {

    private SlotLayout() {
    }

    public static int[] xs(int count) {
        return switch (count) {
            case 1 -> new int[] {50};
            case 2 -> new int[] {34, 66};
            case 3 -> new int[] {20, 50, 80};
            case 4 -> new int[] {13, 38, 62, 87};
            case 5 -> new int[] {10, 30, 50, 70, 90};
            default -> throw new IllegalArgumentException("A row holds 1 to 5 players");
        };
    }

    public static int y(int index, int rows) {
        if (rows <= 1) {
            return 88;
        }
        return (int) Math.round(88 - index * (74.0 / (rows - 1)));
    }
}
