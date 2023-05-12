package men.groupiron;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class LocationState implements ConsumableState {
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final int plane;
    private transient final String playerName;

    LocationState(String playerName, WorldPoint worldPoint) {
        this.playerName = playerName;
        x = worldPoint.getX();
        y = worldPoint.getY();
        plane = worldPoint.getPlane();
    }

    @Override
    public Object get() {
        return new int[] { x, y, plane };
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LocationState)) return false;

        LocationState other = (LocationState) o;
        return (x == other.x) && (y == other.y) && (plane == other.plane);
    }

    @Override
    public String toString() {
        return String.format("{ x: %d, y: %d, plane: %d }", x, y, plane);
    }
}
